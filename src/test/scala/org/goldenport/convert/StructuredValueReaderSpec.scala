package org.goldenport.convert

import java.math.MathContext
import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.record.Record
import org.goldenport.schema.{DataType, XJson, XSemiStructuredData}
import org.goldenport.test.matchers.ConclusionMatchers
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * Executable specification for canonical structured typed readers.
 *
 * @since   Oct.  5, 2026
 * @version Oct.  5, 2026
 * @author  ASAMI, Tomoharu
 */
final class StructuredValueReaderSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks
  with ConclusionMatchers {
  "Canonical Json typed reads (R12)" should {
    "retain every existing JSON shape and exact large decimal unchanged" in {
      Given("existing objects, arrays, strings, booleans, exact numbers and JSON null")
      val decimal = new BigDecimal(new java.math.BigDecimal(
        "123456789012345678901234567890.12345678901234567890123456789"), MathContext.UNLIMITED)
      val values = Vector(Json.obj(), Json.arr(Json.Null, Json.fromString("雪🙂")),
        Json.fromString("日本語"), Json.True, Json.False, Json.fromBigDecimal(decimal), Json.Null)
      When("a generic consumer reads each value as Json")
      val results = values.map(_read_c[Json])
      Then("each successful read retains the original JSON instance and meaning")
      results.zip(values).foreach { case (result, value) =>
        result.get shouldBe Some(value)
        result.get.get should be theSameInstanceAs value
      }
    }

    "reject undecoded text, Records and absent or unsupported roots" in {
      Given("JSON-looking text, a Record, a null runtime root and other runtime values")
      val values = Vector[Any]("{\"number\":1}", "null", Record.empty, null, 1, new Object())
      When("a generic consumer requests Json without explicit text decoding")
      val results = values.map(_read_c[Json])
      Then("each read reports an invalid json value")
      results.foreach(_expect_invalid(_, XJson))
    }
  }

  "Canonical Record typed reads (R12)" should {
    "retain supported Unicode, null, empty and nested values from object Json" in {
      Given("an object with exact numbers, Unicode, explicit null and empty nested structures")
      val decimal = new BigDecimal(new java.math.BigDecimal(
        "987654321098765432109876543210.00000000000000000000000000001"), MathContext.UNLIMITED)
      val json = Json.obj("雪🙂" -> Json.fromString("é日本語"),
        "number" -> Json.fromBigDecimal(decimal), "nullMember" -> Json.Null,
        "emptyObject" -> Json.obj(), "emptyArray" -> Json.arr(),
        "nested" -> Json.obj("items" -> Json.arr(Json.True, Json.Null, Json.fromString("中"))))
      val expected = Record.createFull(Vector("雪🙂" -> "é日本語", "number" -> decimal,
        "nullMember" -> null, "emptyObject" -> Record.empty, "emptyArray" -> Vector.empty,
        "nested" -> Record.createFull(Vector("items" -> Vector(true, null, "中")))))
      When("a generic consumer reads the object as Record")
      val result = _read_c[Record](json)
      Then("the canonical Record retains every member and exact numeric meaning")
      result.get shouldBe Some(expected)
      result.get.get.fields.map(_.key) shouldBe json.asObject.get.keys.toVector
    }

    "normalize supported Record numbers and preserve empty roots and structure" in {
      Given("a native Record with integral and exact decimal numbers, Unicode and nested empty values")
      val decimal = new BigDecimal(new java.math.BigDecimal(
        "123456789012345678901234567890.0000000000000000000000001"), MathContext.UNLIMITED)
      val record = Record.createFull(Vector("text" -> "雪🙂", "integer" -> BigInt("123456789012345678901234567890"),
        "decimal" -> decimal, "nullMember" -> null, "emptyObject" -> Record.empty,
        "emptyArray" -> Vector.empty, "nested" -> Record.createFull(Vector("items" -> Vector(1, null, "é")))))
      val expected = Record.createFull(Vector("text" -> "雪🙂", "integer" -> BigDecimal("123456789012345678901234567890"),
        "decimal" -> decimal, "nullMember" -> null, "emptyObject" -> Record.empty,
        "emptyArray" -> Vector.empty, "nested" -> Record.createFull(Vector("items" -> Vector(BigDecimal(1), null, "é")))))
      When("a generic consumer reads populated and empty canonical roots as Record")
      val result = _read_c[Record](record)
      val emptyresults = Vector[Any](Record.empty, Json.obj()).map(_read_c[Record])
      Then("numbers are canonical BigDecimals and null, empty and nested values remain present")
      result.get shouldBe Some(expected)
      emptyresults.foreach(_.get shouldBe Some(Record.empty))
    }

    "reject undecoded text, non-object JSON and absent or unsupported roots" in {
      Given("text, array, scalar and JSON null roots plus unsupported runtime roots")
      val values = Vector[Any]("{}", "[1]", Json.arr(), Json.fromString("text"),
        Json.True, Json.fromInt(1), Json.Null, null, new Object())
      When("a generic consumer reads those roots as Record")
      val results = values.map(_read_c[Record])
      Then("each read reports an invalid semiStructuredData value")
      results.foreach(_expect_invalid(_, XSemiStructuredData))
    }

    "reject unsupported Record members without omission or stringification" in {
      Given("Records containing application objects, maps, Json members and nonfinite numbers")
      val values = Vector[Any](new Object(), Map("key" -> 1), Json.obj(),
        Float.NaN, Float.PositiveInfinity, Double.NaN, Double.NegativeInfinity)
      val records = values.map(value => Record.createFull(Vector("member" -> value)))
      When("a generic consumer reads each Record")
      val results = records.map(_read_c[Record])
      Then("all unsupported members retain the canonical invalid datatype conclusion")
      results.foreach(_expect_invalid(_, XSemiStructuredData))
    }

    "retain independent nested member failures in the typed conclusion" in {
      Given("two unsupported array elements and an independent unsupported sibling")
      val record = Record.createFull(Vector("array" -> Vector(new Object(), new Object()),
        "sibling" -> new Object()))
      When("a generic consumer reads the complete Record")
      val result = _read_c[Record](record)
      Then("all three independent invalid conclusions remain available")
      result match {
        case Consequence.Failure(conclusion) =>
          conclusion.causes should have size 3
          conclusion.causes.foreach { cause =>
            cause should be_invalid_failure_conclusion
            cause.observation.cause.getDataType shouldBe Some(XSemiStructuredData)
          }
        case _ => fail("Unsupported members must produce aggregated failure")
      }
    }
  }

  "Generic Record reader roundtrips (R12)" should {
    "preserve generated exact decimals, Unicode, nested objects, arrays and null" in {
      Given("generated canonical JSON objects with supported exact and nested values")
      forAll(_object_gen) { json =>
        When("a generic consumer reads Record and explicitly encodes its canonical JSON")
        val decoded = _read_c[Record](json)
        val encoded = decoded.flatMap(XSemiStructuredData.toJson)
        Then("the roundtrip preserves semantic JSON equality and member order")
        decoded.isSuccess shouldBe true
        encoded.get shouldBe Some(json)
        encoded.get.get.asObject.get.keys.toVector shouldBe json.asObject.get.keys.toVector
        decoded.get.get.fields.map(_.key) should contain ("nullMember")
      }
    }
  }

  "Existing typed reader defaults (R12)" should {
    "preserve JSON-looking String text exactly" in {
      Given("undecoded JSON-looking text with original whitespace and numeric spelling")
      val text = "  {\"雪\": [null, 1.2300]}\n"
      When("a generic consumer requests the existing String reader")
      val result = _read_c[String](text)
      Then("the complete original lexical text remains unchanged")
      result.get shouldBe Some(text)
    }

    "expose None through optional reads for failed canonical conversions" in {
      Given("text that requires explicit decoding before canonical typed reads")
      val text = "{}"
      When("the Json and Record readers use the existing optional read API")
      val jsonresult = summon[ValueReader[Json]].read(text)
      val recordresult = summon[ValueReader[Record]].read(text)
      Then("both canonical failures become None")
      jsonresult shouldBe None
      recordresult shouldBe None
    }
  }

  private def _read_c[T: ValueReader](value: Any): Consequence[T] = {
    summon[ValueReader[T]].readC(value)
  }

  private def _expect_invalid(result: Consequence[?], datatype: DataType): Unit = {
    result match {
      case Consequence.Failure(conclusion) =>
        conclusion should be_invalid_failure_conclusion
        conclusion.observation.cause.getDataType shouldBe Some(datatype)
      case _ => fail(s"Expected an invalid ${datatype.name} value")
    }
  }

  private val _decimal_gen: Gen[BigDecimal] = {
    for {
      size <- Gen.choose(1, 80)
      digits <- Gen.listOfN(size, Gen.numChar)
      sign <- Gen.oneOf("", "-")
      scale <- Gen.choose(-50, 80)
    } yield {
      val integer = new java.math.BigInteger(sign + digits.mkString)
      new BigDecimal(new java.math.BigDecimal(integer, scale), MathContext.UNLIMITED)
    }
  }

  private val _string_gen: Gen[String] = {
    Gen.listOf(Gen.oneOf("雪", "é", "🙂", "\u0000", "\n", "\"", "\\", "a", "中"))
      .map(_.mkString)
  }

  private def _value_gen(depth: Int): Gen[Json] = {
    val scalar = Gen.oneOf(Gen.const(Json.Null), Gen.oneOf(true, false).map(Json.fromBoolean),
      _string_gen.map(Json.fromString), _decimal_gen.map(Json.fromBigDecimal))
    if (depth == 0) {
      scalar
    } else {
      val nested = _value_gen(depth - 1)
      Gen.oneOf(scalar,
        Gen.choose(0, 5).flatMap(size => Gen.listOfN(size, nested)).map(Json.fromValues),
        nested.map(value => Json.obj("child" -> value, "empty" -> Json.obj())))
    }
  }

  private val _object_gen: Gen[Json] = {
    for {
      decimal <- _decimal_gen
      string <- _string_gen
      payload <- _value_gen(2)
      elements <- Gen.choose(0, 5).flatMap(size => Gen.listOfN(size, _value_gen(1)))
    } yield {
      Json.obj("number" -> Json.fromBigDecimal(decimal), "text" -> Json.fromString(string),
        "payload" -> payload, "array" -> Json.fromValues(elements),
        "nullMember" -> Json.Null, "emptyObject" -> Json.obj())
    }
  }
}
