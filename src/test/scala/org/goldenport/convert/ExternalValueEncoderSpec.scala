package org.goldenport.convert

import java.math.MathContext
import java.nio.charset.StandardCharsets
import java.util.{Locale, TimeZone}
import cats.data.NonEmptyVector
import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.datatype.*
import org.goldenport.record.{Record, RecordPresentable}
import org.goldenport.schema.XSemiStructuredData
import org.goldenport.text.Presentable
import org.goldenport.value.*
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Oct. 18, 2026
 * @version Oct. 18, 2026
 */
final class ExternalValueEncoderSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import ExternalValueEncoderSpec.*

  "External value projection" should {
    "retain typed Json instances of every shape and exact large decimals" in {
      Given("raw Json shapes, embedded nulls and an unlimited-precision decimal")
      val decimal = new BigDecimal(new java.math.BigDecimal("123456789012345678901234567890.12345678901234567890123456789"), MathContext.UNLIMITED)
      val jsons = Vector(Json.obj("null" -> Json.Null), Json.arr(Json.Null), Json.Null,
        Json.fromString("雪"), Json.True, Json.fromBigDecimal(decimal))
      When("the public encoder projects typed Json and the exact decimal")
      val results = jsons.map(ExternalValueEncoder.encodeC)
      val number = ExternalValueEncoder.encodeC(decimal)
      Then("Json identity and precision remain intact without a codec roundtrip")
      results.zip(jsons).foreach { case (result, json) =>
        result.get.get.asInstanceOf[Json] should be theSameInstanceAs json
      }
      number.get shouldBe Some(decimal)
      number.get.get.asInstanceOf[BigDecimal].bigDecimal.toPlainString shouldBe decimal.bigDecimal.toPlainString
    }

    "preserve normalized Record values and present empty structures without canonical revalidation" in {
      Given("construction-normalized Records with opaque and raw Json fields, plus empty values")
      val json = Json.arr(Json.Null)
      val record = Record.data("json" -> json, "number" -> 1, "empty" -> Record.empty)
      val empty = Vector[Any](Record.empty, Vector.empty, Array.empty[Int], Map.empty[String, Any], Set.empty[Int])
      When("external projection retains Record ownership while canonical validation remains a separate request")
      val result = ExternalValueEncoder.encodeC(record)
      val emptyresults = empty.map(ExternalValueEncoder.encodeC)
      val canonical = XSemiStructuredData.normalizer.normalize(record)
      Then("the Record instance survives, empty structures remain present and the canonical contract still rejects raw Json members")
      result.get.get.asInstanceOf[Record] should be theSameInstanceAs record
      emptyresults.map(_.get) shouldBe Vector(Some(Record.empty), Some(Vector.empty), Some(Vector.empty), Some(Map.empty), Some(Vector.empty))
      canonical.isSuccess shouldBe false
    }

    "retain primitive and exact numeric runtime values and explicitly project characters" in {
      Given("finite primitive numbers, exact Scala and JDK numbers, text, booleans and a character")
      val values = Vector[Any](1.toByte, 2.toShort, 3, 4L, 1.25f, 2.5d,
        BigInt("9999999999999999999999999999999"), BigDecimal("0.00000000000000000000000000001"),
        new java.math.BigInteger("9999999999999999999999999999999"),
        new java.math.BigDecimal("12345678901234567890.00000000000000000001"), "null", false)
      When("the encoder projects these declared scalar values and a character")
      val results = values.map(ExternalValueEncoder.encodeC)
      val character = ExternalValueEncoder.encodeC('雪')
      Then("numeric meaning and runtime classes remain unchanged and the character becomes its text")
      results.zip(values).foreach { case (result, value) =>
        result.get shouldBe Some(value)
        result.get.get.getClass shouldBe value.getClass
      }
      character.get shouldBe Some("雪")
    }

    "retain nested Option structure while projecting supported children" in {
      Given("nested presence and absence around ordered arrays")
      val value = Some(Some(Array[Any](Some('a'), Json.Null, Vector.empty[Int])))
      When("the encoder recursively projects options and collections")
      val result = ExternalValueEncoder.encodeC(value)
      val absent = ExternalValueEncoder.encodeC(None)
      val nestedabsence = ExternalValueEncoder.encodeC(Some(None))
      Then("Some wrappers remain and None retains its absence meaning outside sequence positions")
      result.get shouldBe Some(Some(Some(Vector(Some("a"), Json.Null, Vector.empty))))
      absent.get shouldBe Some(None)
      nestedabsence.get shouldBe Some(Some(None))
    }

    "project ordered Seq, Array and NonEmptyVector into duplicate-preserving Vectors" in {
      Given("supported collections with the same ordered duplicate payload")
      val inputs = Vector[Any](List('b', 'a', 'b'), Array('b', 'a', 'b'), NonEmptyVector.of('b', 'a', 'b'))
      When("the public encoder projects each collection")
      val results = inputs.map(ExternalValueEncoder.encodeC)
      Then("every representation produces the same ordered external values")
      results.foreach(_.get shouldBe Some(Vector("b", "a", "b")))
    }

    "retain Set membership and keyed String Map associations during recursive projection" in {
      Given("a Set and a String-keyed Map with nested values and absence")
      val set = Set('a', 'b')
      val map = Map[String, Any]("items" -> Array('b', 'a', 'b'), "absent" -> None, "raw" -> Json.Null)
      When("the encoder projects both collection contracts")
      val setresult = ExternalValueEncoder.encodeC(set)
      val mapresult = ExternalValueEncoder.encodeC(map)
      Then("Set projection promises membership and the Map remains a keyed association")
      setresult.get.get.asInstanceOf[Vector[String]].toSet shouldBe Set("a", "b")
      mapresult.get shouldBe Some(Map("items" -> Vector("b", "a", "b"), "absent" -> None, "raw" -> Json.Null))
      mapresult.get.get shouldBe a [Map[?, ?]]
    }

    "reject nonfinite values and native null without creating an external value" in {
      Given("native null and nonfinite Float and Double values")
      val values = Vector[Any](null, Float.NaN, Float.PositiveInfinity, Float.NegativeInfinity,
        Double.NaN, Double.PositiveInfinity, Double.NegativeInfinity)
      When("the public encoder admits each root")
      val results = values.map(ExternalValueEncoder.encodeC)
      Then("each unsupported scalar returns a structured failure")
      results.foreach(_.isSuccess shouldBe false)
    }

    "reject native-null or absent sequence positions without filtering or shifting siblings" in {
      Given("malformed sequence positions, including adapter-produced absence, and a valid JSON null")
      val inputs = Vector[Any](Vector[Any]("before", null, "after"), Array[Any](None),
        NonEmptyVector.of[Any]("before", None), Vector(Vector[Any](None)), Vector(Nominal("absent")))
      val adapter: ExternalValueEncoder.Adapter = { case Nominal("absent") => Consequence.success(None) }
      When("the encoder projects each malformed sequence and a valid Json-null sequence")
      val results = inputs.map(value => ExternalValueEncoder.encodeC(value, adapter))
      val valid = ExternalValueEncoder.encodeC(Vector(Json.Null, "after", Json.Null))
      Then("malformed positions fail the entire projection and typed JSON null remains present")
      results.foreach(_.isSuccess shouldBe false)
      valid.get shouldBe Some(Vector(Json.Null, "after", Json.Null))
    }

    "aggregate independent projection failures" in {
      Given("two unsupported independent sequence members")
      val values = Vector(new UnknownValue(), new UnknownValue())
      When("the encoder projects the complete sequence")
      val result = ExternalValueEncoder.encodeC(values)
      Then("both structured failures remain available")
      result match {
        case Consequence.Failure(conclusion) => conclusion.causes should have size 2
        case _ => fail("Independent failures must aggregate")
      }
    }

    "use the model adapter before generic presentation and allow recursive library delegation" in {
      Given("nominal values that also implement Record presentation and a library-owned recursive wrapper")
      var calls = 0
      def _encode_(value: Any): Consequence[Any] = ExternalValueEncoder.encodeC(value, {
        case nominal: Nominal => calls += 1; _encode_(nominal.value)
      })
      val value = Nominal(Vector(Nominal('z'), Nominal('a'), Nominal('z')))
      When("the library wrapper delegates generic projection to Core")
      val result = _encode_(value)
      Then("nominal underlying values win over Record presentation and each adapter application occurs once")
      result.get shouldBe Some(Vector("z", "a", "z"))
      calls shouldBe 4
    }

    "reject unknown values and non-String keys without invoking arbitrary toString" in {
      Given("objects whose arbitrary string conversion throws and exposes private text")
      val unknown = new UnknownValue()
      val key = new UnknownValue()
      When("the public encoder sees an unknown root and an invalid keyed association")
      val result = ExternalValueEncoder.encodeC(unknown)
      val keyed = ExternalValueEncoder.encodeC(Map[Any, Any](key -> "value"))
      Then("both are failures without invoking arbitrary text conversion or leaking its payload")
      result.isSuccess shouldBe false
      keyed.isSuccess shouldBe false
      unknown.calls shouldBe 0
      key.calls shouldBe 0
      result match {
        case Consequence.Failure(conclusion) => conclusion.print should not include "private-payload"
        case _ => fail("Unknown objects must fail")
      }
    }

    "project Core internationalized and content protocols through their declared values" in {
      Given("Core-owned internationalized values and explicit media and locale protocols")
      val text = I18nString(NonEmptyVector.of(Locale.ENGLISH -> "hello", Locale.JAPANESE -> "雪"))
      val localized = Vector[Any](I18nLabel(text), I18nTitle(text), I18nBrief(text),
        I18nSummary(text), I18nDescription(text), I18nText(text))
      val values = Vector[Any](MimeType.TEXT_PLAIN, StandardCharsets.UTF_8, Locale.JAPAN,
        TimeZone.getTimeZone("UTC"), ContentBody("body"), ContentMarkup.SmartDox, ObjectId(Identifier("id1")))
      When("the encoder projects each Core protocol")
      val localizedresults = localized.map(ExternalValueEncoder.encodeC)
      val results = values.map(ExternalValueEncoder.encodeC)
      Then("localized Records and explicit protocol text preserve their specified values")
      localizedresults.foreach(_.get shouldBe Some(Record.data("en" -> "hello", "ja" -> "雪")))
      results.map(_.get) shouldBe Vector(Some("text/plain"), Some("UTF-8"), Some("ja-JP"),
        Some("UTC"), Some("body"), Some("smartdox"), Some("id1"))
    }

    "project every named Core attribute field while preserving empty content references" in {
      Given("complete name and descriptive attributes plus content and occurrence protocols")
      val names = NameAttributes(Name("name"), Some(I18nLabel("label")), Some(I18nTitle("title")),
        Some(Identifier("code")), Some(NonEmptyVector.of(I18nLabel("alias"))), Some(Slug.Instance("slug")))
      val description = DescriptiveAttributes(Some(I18nBrief("headline")), Some(I18nBrief("brief")),
        Some(I18nSummary("summary")), Some(I18nDescription("description")), Some(I18nSummary("lead")),
        Some(I18nSummary("abstract")), Some(I18nSummary("remarks")), Some(I18nLabel("tooltip")))
      val occurrence = ContentReferenceOccurrence(occurrenceIndex = 2, originalRef = Some("image"))
      When("the encoder projects Core attribute aggregates")
      val named = ExternalValueEncoder.encodeC(names)
      val described = ExternalValueEncoder.encodeC(description)
      val content = ExternalValueEncoder.encodeC(ContentAttributes.empty)
      val reference = ExternalValueEncoder.encodeC(occurrence)
      Then("all named fields have recursively projected values and the empty references vector stays present")
      named.get shouldBe Some(Record.data("name" -> "name", "label" -> Record.data("und" -> "label"),
        "title" -> Record.data("und" -> "title"), "code" -> "code",
        "alias" -> Vector(Record.data("und" -> "alias")), "slug" -> "slug"))
      described.get shouldBe Some(Record.data("headline" -> Record.data("und" -> "headline"),
        "brief" -> Record.data("und" -> "brief"), "summary" -> Record.data("und" -> "summary"),
        "description" -> Record.data("und" -> "description"), "lead" -> Record.data("und" -> "lead"),
        "abstract" -> Record.data("und" -> "abstract"), "remarks" -> Record.data("und" -> "remarks"),
        "tooltip" -> Record.data("und" -> "tooltip")))
      content.get shouldBe Some(Record.data("references" -> Vector.empty))
      reference.get shouldBe Some(Record.data("occurrence_index" -> 2, "original_ref" -> "image"))
    }

    "honor explicit presentation protocols and contain callback or adapter failures" in {
      Given("Record and text presentation protocols, malformed callbacks and throwing adapters")
      val record = new RecordPresentable { def toRecord(): Record = Record.data("value" -> "record") }
      val text = new Presentable { def print: String = "explicit text" }
      val nullrecord = new RecordPresentable { def toRecord(): Record = null }
      val brokenrecord = new RecordPresentable { def toRecord(): Record = throw new IllegalArgumentException("record callback") }
      val brokentext = new Presentable { def print: String = throw new IllegalArgumentException("text callback") }
      val adapter: ExternalValueEncoder.Adapter = { case _: UnknownValue => throw new IllegalArgumentException("adapter callback") }
      When("the encoder admits each explicit foreign callback")
      val projectedrecord = ExternalValueEncoder.encodeC(record)
      val projectedtext = ExternalValueEncoder.encodeC(text)
      val failures = Vector(ExternalValueEncoder.encodeC(nullrecord), ExternalValueEncoder.encodeC(brokenrecord),
        ExternalValueEncoder.encodeC(brokentext), ExternalValueEncoder.encodeC(new UnknownValue(), adapter))
      Then("valid protocols project and callback exceptions or absent Records remain structured failures")
      projectedrecord.get shouldBe Some(Record.data("value" -> "record"))
      projectedtext.get shouldBe Some("explicit text")
      failures.foreach(_.isSuccess shouldBe false)
    }

    "preserve generated ordered duplicates, raw Json and exact values without discarded samples" in {
      Given("independent ordered values with a forced duplicate, raw Json null and exact scaled integers")
      val generator = for { number <- Gen.choose(-100000, 100000); scale <- Gen.choose(1, 60); text <- Gen.alphaStr } yield (number, scale, text)
      val law = Prop.forAll(generator) { case (number, scale, text) =>
        val decimal = new BigDecimal(new java.math.BigDecimal(java.math.BigInteger.valueOf(number.toLong), scale), MathContext.UNLIMITED)
        val json = Json.arr(Json.Null, Json.fromString(text))
        val values = Vector[Any](decimal, json, text, decimal, Json.Null)
        ExternalValueEncoder.encodeC(values).get.contains(values) &&
          ExternalValueEncoder.encodeC(NonEmptyVector(values.head, values.tail)).get.contains(values) &&
          ExternalValueEncoder.encodeC(values.toArray).get.contains(values)
      }
      When("ScalaCheck evaluates at least one hundred successful semantic samples")
      val result = Test.check(Test.Parameters.default.withMinSuccessfulTests(100), law)
      Then("external value meaning and duplicate positions survive every sample")
      result.passed shouldBe true
      result.succeeded should be >= 100
      result.discarded shouldBe 0
    }
  }
}

object ExternalValueEncoderSpec {
  final case class Nominal(value: Any) extends RecordPresentable {
    def toRecord(): Record = Record.data("presentation" -> "must not win")
  }

  final class UnknownValue {
    private var _calls = 0
    def calls: Int = _calls
    override def toString: String = {
      _calls += 1
      throw new IllegalStateException("private-payload")
    }
  }
}
