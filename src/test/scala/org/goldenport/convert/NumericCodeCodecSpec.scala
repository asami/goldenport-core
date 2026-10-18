package org.goldenport.convert

import io.circe.Json
import io.circe.parser.parse
import org.goldenport.{Conclusion, Consequence, ConsequenceException}
import org.goldenport.observation.Taxonomy
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Oct. 19, 2026
 * @version Oct. 19, 2026
 */
final class NumericCodeCodecSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import NumericCodeCodecSpec.*

  private val _entries_gen: Gen[Vector[(Kind, Int)]] =
    Gen.listOfN(8, Gen.choose(Int.MinValue, Int.MaxValue)).map { generated =>
      (Vector(Int.MinValue, -1, 0, 1, Int.MaxValue) ++ generated).distinct.zipWithIndex.map {
        case (code, index) => Kind(s"kind-$index", s"Label $index") -> code
      }
    }

  "Numeric code correspondence" should {
    "explicit mappings" which {
      "roundtrip every mapped value and exact Int code without an ordinal or zero sentinel" in {
        Given("injective explicit mappings containing zero, negative codes and both Int boundaries")
        val law = Prop.forAll(_entries_gen) { entries =>
          val codec = _codec(entries)
          val jsoncodec = codec.jsonCodec
          entries.forall { case (value, code) =>
            codec.encodeC(value).get.contains(code) &&
              codec.decodeC(code).get.contains(value) &&
              codec.encodeC(value).flatMap(codec.decodeC).get.contains(value) &&
              codec.decodeC(code).flatMap(codec.encodeC).get.contains(code) &&
              jsoncodec(value) == Json.fromInt(code) &&
              jsoncodec.decodeJson(Json.fromInt(code)) == Right(value)
          }
        }
        When("ScalaCheck evaluates at least one hundred explicit mapping samples")
        val result = Test.check(Test.Parameters.default.withMinSuccessfulTests(100), law)
        Then("typed and JSON roundtrips preserve the correspondence for every sample")
        result.passed shouldBe true
        result.succeeded should be >= 100
        result.discarded shouldBe 0
      }

      "retain codes when declaration order and presentation labels change" in {
        Given("explicit assignments and an equivalent declaration with reversed order and new labels")
        val law = Prop.forAll(_entries_gen) { entries =>
          val reordered = entries.reverse.map { case (value, code) =>
            value.copy(label = s"Changed ${value.value}") -> code
          }
          val original = _codec(entries)
          val changed = _codec(reordered)
          entries.forall { case (value, code) =>
            original.encodeC(value).get == changed.encodeC(value).get &&
              changed.decodeC(code).get.exists(_.value == value.value) &&
              changed.jsonCodec(value) == original.jsonCodec(value)
          }
        }
        When("ScalaCheck evaluates at least one hundred reorder and relabel samples")
        val result = Test.check(Test.Parameters.default.withMinSuccessfulTests(100), law)
        Then("the fixed assignment follows symbolic identity rather than order or label")
        result.passed shouldBe true
        result.succeeded should be >= 100
        result.discarded shouldBe 0
      }

      "reject missing assignments and unknown codes through structured value failures" in {
        Given("a partial mapping with an explicitly mapped zero and an unmapped value")
        val codec = _codec(Vector(Kind("zero", "Zero") -> 0, Kind("one", "One") -> 1))
        val missing = Kind("missing", "Missing")
        When("encoding the unmapped value and decoding a code outside the mapping")
        val encoded = codec.encodeC(missing)
        val decoded = codec.decodeC(42)
        Then("both outcomes are value-invalid failures rather than successful absence or a default")
        _failure(encoded).observation.taxonomy shouldBe Taxonomy(Taxonomy.Category.Value, Taxonomy.Symptom.Invalid)
        _failure(decoded).observation.taxonomy shouldBe Taxonomy(Taxonomy.Category.Value, Taxonomy.Symptom.Invalid)
        encoded.get shouldBe None
        decoded.get shouldBe None
        codec.decodeC(0).get shouldBe Some(Kind("zero", "Zero"))
      }

      "contain exceptions from both foreign mapping callbacks" in {
        Given("a codec whose forward and inverse callbacks throw")
        val codec = NumericCodeCodec.instance[Kind](
          _ => throw new IllegalArgumentException("forward callback"),
          _ => throw new IllegalArgumentException("inverse callback")
        )
        When("the typed boundary invokes either callback")
        val encoded = codec.encodeC(Kind("one", "One"))
        val decoded = codec.decodeC(1)
        Then("both exceptions are represented by structured Consequence failures")
        _failure(encoded).getException.map(_.getMessage) shouldBe Some("forward callback")
        _failure(decoded).getException.map(_.getMessage) shouldBe Some("inverse callback")
      }

      "reject an unmapped object without consulting arbitrary string presentation" in {
        Given("an opaque value whose toString must never supply or guess its code")
        val codec = NumericCodeCodec.instance[OpaqueValue](_ => None, _ => None)
        val value = new OpaqueValue()
        When("the explicit mapping reports no code")
        val result = codec.encodeC(value)
        Then("the value-invalid failure is independent of string conversion")
        _failure(result).observation.taxonomy shouldBe Taxonomy(Taxonomy.Category.Value, Taxonomy.Symptom.Invalid)
        value.calls shouldBe 0
      }
    }

    "numeric JSON projection" which {
      "accept mathematically integral decimal and exponent numbers using the same fixed code" in {
        Given("an explicit code and JSON numbers with integral decimal or exponent notation")
        val value = Kind("hundred", "Hundred")
        val codec = NumericCodeCodec.instance[Kind](v => Option.when(v == value)(100), code => Option.when(code == 100)(value))
        val inputs = Vector(_json("100"), _json("100.000"), _json("1e2"))
        When("the numeric adapter decodes each exact representation")
        val results = inputs.map(codec.jsonCodec.decodeJson)
        Then("numeric notation does not alter the assigned code or decoded value")
        results shouldBe Vector.fill(3)(Right(value))
        NumericCodeCodec.jsonCodec(codec)(value) shouldBe Json.fromInt(100)
      }

      "reject generated fractional and overflow numbers without rounding or truncation" in {
        Given("generated nonintegral numbers and integers beyond either Int boundary")
        val codec = NumericCodeCodec.instance[Int](Some(_), Some(_)).jsonCodec
        val generator = for {
          whole <- Gen.choose(-100000, 100000)
          excess <- Gen.choose(1, 100000)
        } yield Vector(
          Json.fromBigDecimal(BigDecimal(whole) + BigDecimal("0.5")),
          Json.fromBigInt(BigInt(Int.MaxValue) + excess),
          Json.fromBigInt(BigInt(Int.MinValue) - excess)
        )
        val law = Prop.forAll(generator)(inputs => inputs.forall(codec.decodeJson(_).isLeft))
        When("ScalaCheck evaluates at least one hundred rejected numeric input samples")
        val result = Test.check(Test.Parameters.default.withMinSuccessfulTests(100), law)
        Then("none of the inputs can become a mapped Int value")
        result.passed shouldBe true
        result.succeeded should be >= 100
        result.discarded shouldBe 0
      }

      "reject strings, objects, arrays, booleans and null before invoking the inverse mapping" in {
        Given("non-number JSON shapes and an inverse mapping that records its calls")
        var calls = 0
        val codec = NumericCodeCodec.instance[Int](Some(_), code => { calls += 1; Some(code) }).jsonCodec
        val inputs = Vector(Json.fromString("1"), Json.fromString("1.0"), Json.obj("code" -> Json.fromInt(1)),
          Json.arr(Json.fromInt(1)), Json.True, Json.False, Json.Null)
        When("the numeric adapter decodes each shape")
        val results = inputs.map(codec.decodeJson)
        Then("all inputs are rejected without admitting a code to the inverse mapping")
        results.foreach(_.isLeft shouldBe true)
        calls shouldBe 0
      }

      "render unknown-code failures using their structured conclusion and Circe cursor history" in {
        Given("an unmapped numeric code inside an object field")
        val codec = _codec(Vector(Kind("one", "One") -> 1))
        val cursor = Json.obj("code" -> Json.fromInt(42)).hcursor.downField("code")
        val expected = _failure(codec.decodeC(42)).print
        When("the selected JSON codec decodes that field")
        val result = codec.jsonCodec.tryDecode(cursor)
        Then("Circe retains the structured failure's rendering and the failing field location")
        result.isLeft shouldBe true
        result.swap.toOption.get.message shouldBe expected
        result.swap.toOption.get.history shouldBe cursor.history
      }

      "escalate encoding failures and return decoding failures for throwing callbacks" in {
        Given("an unmapped value and a separate codec with throwing mapping callbacks")
        val value = Kind("missing", "Missing")
        val absent = _codec(Vector(Kind("one", "One") -> 1)).jsonCodec
        val broken = NumericCodeCodec.instance[Kind](
          _ => throw new IllegalArgumentException("forward callback"),
          _ => throw new IllegalArgumentException("inverse callback")
        ).jsonCodec
        When("Circe encoding has no successful numeric code and decoding invokes a failed inverse")
        val absentexception = intercept[ConsequenceException](absent(value))
        val brokenexception = intercept[ConsequenceException](broken(value))
        val decoded = broken.decodeJson(Json.fromInt(1))
        Then("encoding exposes the Consequence failure boundary and decoding remains a Circe failure")
        absentexception shouldBe a [ConsequenceException]
        brokenexception shouldBe a [ConsequenceException]
        decoded.isLeft shouldBe true
      }
    }
  }

  private def _codec(entries: Vector[(Kind, Int)]): NumericCodeCodec[Kind] =
    NumericCodeCodec.instance(
      value => entries.find(_._1.value == value.value).map(_._2),
      code => entries.find(_._2 == code).map(_._1)
    )

  private def _failure(result: Consequence[?]): Conclusion = result match {
    case Consequence.Failure(conclusion) => conclusion
    case _ => fail("Expected a structured failure")
  }

  private def _json(text: String): Json = parse(text).fold(error => fail(error.message), identity)
}

object NumericCodeCodecSpec {
  final case class Kind(value: String, label: String)

  final class OpaqueValue {
    var calls: Int = 0
    override def toString: String = {
      calls += 1
      throw new IllegalStateException("arbitrary presentation")
    }
  }
}
