package org.goldenport.record

import java.math.MathContext
import java.nio.charset.StandardCharsets
import java.time.{Clock, Instant, ZoneOffset}
import java.util.Locale
import cats.data.NonEmptyVector
import _root_.io.circe.Json
import org.goldenport.Consequence
import org.goldenport.context.*
import org.goldenport.convert.ValueReader
import org.goldenport.log.Logger
import org.goldenport.record.io.RecordEncoder
import org.goldenport.schema.{StructuredDataFormat, XJson, XSemiStructuredData}
import org.goldenport.test.matchers.ConclusionMatchers
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * Executable specification for the connected normalized Record route.
 *
 * @since   Oct. 18, 2026
 * @version Oct. 18, 2026
 */
final class RecordNormalizationSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks
  with ConclusionMatchers {
  "Record factory boundaries" should {
    "omit null attributes while retaining empty children and all present scalar values" in {
      Given("ordered inputs with absent attributes, empty structures and present false, zero and String null")
      val inputs = Vector[(String, Any)]("before" -> null, "child" -> Record.empty,
        "vector" -> Vector.empty, "array" -> Array.empty[Int], "list" -> List.empty,
        "false" -> false, "zero" -> 0, "text" -> "null", "unicode" -> "雪🙂", "after" -> null)
      val expectedkeys = inputs.map(_._1).filterNot(Set("before", "after"))
      When("create, createFull and data construct Records and the encoder renders JSON")
      val records = Vector(Record.create(inputs), Record.createFull(inputs), Record.data(inputs*))
      val encoded = records.map(RecordEncoder.jsonC)
      Then("every factory retains the same present keys, empty structures and exact scalars in order")
      records.foreach(_.fields.map(_.key) shouldBe expectedkeys)
      val expected = Json.obj("child" -> Json.obj(), "vector" -> Json.arr(), "array" -> Json.arr(),
        "list" -> Json.arr(), "false" -> Json.False, "zero" -> Json.fromInt(0),
        "text" -> Json.fromString("null"), "unicode" -> Json.fromString("雪🙂"))
      encoded.foreach(_.get shouldBe Some(expected.noSpaces))
    }

    "normalize legacy field inputs recursively without losing an emptied child" in {
      Given("a raw field-vector Record whose root, nested object and object-in-array have null attributes")
      val child = Record(Vector(Field("absent", Field.Value.Single(null))))
      val record = Record(Vector(Field("absent", Field.Value.Single(null)),
        Field("child", Field.Value.Single(child)), Field("items", Field.Value.Single(Vector(child)))))
      When("semantic normalization and ordinary JSON and YAML output interpret the Record")
      val normalized = XSemiStructuredData.normalizer.normalize(record)
      val json = RecordEncoder.jsonC(record)
      val yaml = RecordEncoder.yamlC(record).flatMap(XSemiStructuredData.fromText(_, StructuredDataFormat.Yaml))
      Then("null attributes disappear recursively and emptied objects remain present")
      val expected = Record.data("child" -> Record.empty, "items" -> Vector(Record.empty))
      normalized.get shouldBe Some(expected)
      json.get shouldBe Some("{\"child\":{},\"items\":[{}]}")
      yaml.get shouldBe Some(expected)
    }
  }

  "Connected collection conversion" should {
    "roundtrip nested NonEmptyVectors as arrays and explicitly read their nonempty types" in {
      Given("a factory-built Record containing real nested Cats NonEmptyVectors and empty child objects")
      val numbers = NonEmptyVector.of(3, 1, 2)
      val child = Record.data("numbers" -> numbers, "empty" -> Record.empty, "absent" -> null)
      val record = Record.data("children" -> NonEmptyVector.of(child),
        "matrix" -> NonEmptyVector.of(NonEmptyVector.of(7, 8), NonEmptyVector.of(9)))
      When("ordinary JSON encoding feeds canonical decoding and the real Record typed-read APIs")
      val encoded = RecordEncoder.jsonC(record)
      val decoded = encoded.flatMap(XSemiStructuredData.fromText(_, StructuredDataFormat.Json))
      val children = decoded.flatMap(_.asC[NonEmptyVector[Record]]("children"))
      val typednumbers = children.flatMap(_.head.asC[NonEmptyVector[Int]]("numbers"))
      val matrix = decoded.flatMap(_.getAsC[NonEmptyVector[NonEmptyVector[Int]]]("matrix"))
      Then("untyped arrays become ordered Vectors and requested nonempty types retain values and cardinality")
      encoded.get shouldBe Some("{\"children\":[{\"numbers\":[3,1,2],\"empty\":{}}],\"matrix\":[[7,8],[9]]}")
      decoded.get.get.getAny("children").get shouldBe a[Vector[?]]
      typednumbers.get shouldBe Some(numbers)
      children.get.get.head.getRecord("empty") shouldBe Some(Record.empty)
      matrix.get shouldBe Some(Some(NonEmptyVector.of(NonEmptyVector.of(7, 8), NonEmptyVector.of(9))))
      record.getVector("children") shouldBe Some(Vector(child))
      child.getVector("numbers") shouldBe Some(Vector(3, 1, 2))
    }

    "accept actual nonempty vectors, sequences and native arrays through element readers" in {
      Given("nonempty Cats, List, Vector, reference-array and primitive-array runtime values")
      val inputs = Vector[Any](NonEmptyVector.of("2", "1"), List("2", "1"),
        Vector(2, 1), Array("2", "1"), Array(2, 1))
      When("the generic NonEmptyVector reader decodes each element as Int")
      val results = inputs.map(summon[ValueReader[NonEmptyVector[Int]]].readC)
      Then("each supported shape has the same typed ordered nonempty meaning")
      results.foreach(_.get shouldBe Some(NonEmptyVector.of(2, 1)))
    }

    "fail empty collections and wrong shapes while keeping a missing optional field distinct" in {
      Given("empty supported shapes, scalar and unsupported shapes, and a Record with an empty field")
      val inputs = Vector[Any](Vector.empty, List.empty, Array.empty[Int], "[1]", 1,
        false, Set(1), Json.arr(Json.fromInt(1)), Map("item" -> 1), null)
      val record = Record.data("items" -> Vector.empty)
      When("nonempty typed reads interpret each shape and present or absent fields")
      val results = inputs.map(summon[ValueReader[NonEmptyVector[Int]]].readC)
      val present = record.getAsC[NonEmptyVector[Int]]("items")
      val absent = record.getAsC[NonEmptyVector[Int]]("missing")
      Then("present empty and wrong-shaped inputs fail and only absence yields successful None")
      results.foreach(_expect_invalid)
      _expect_invalid(present)
      absent.get shouldBe Some(None)
    }

    "reject native-null elements before even an Any element reader can accept them" in {
      Given("direct and nested null positions across supported sequence representations")
      val inputs = Vector[Any](Vector[Any](1, null), Array[Any](null), NonEmptyVector.of[Any](null))
      val nested = Vector(Vector[Any](1), Vector[Any](null))
      When("nonempty Any readers and recursively nested nonempty readers interpret those positions")
      val results = inputs.map(summon[ValueReader[NonEmptyVector[Any]]].readC)
      val nestedresult = summon[ValueReader[NonEmptyVector[NonEmptyVector[Any]]]].readC(nested)
      Then("every null position is an invalid value and no shortened collection succeeds")
      results.foreach(_expect_invalid)
      _expect_invalid(nestedresult)
    }

    "aggregate independent element failures and reject nulls throughout ordinary output" in {
      Given("two invalid typed elements and Records with null positions at several depths")
      val invalids = Vector[Any]("first-invalid", "second-invalid")
      val records = Vector(Record.data("items" -> Vector[Any](1, null)),
        Record.data("items" -> NonEmptyVector.of[Any](null)),
        Record.data("items" -> Array[Any](Vector[Any](null))),
        Record.data("items" -> Vector(Record.data("nested" -> Vector[Any](null)))))
      When("the typed reader and both ordinary encoders interpret these values")
      val elements = summon[ValueReader[NonEmptyVector[Int]]].readC(invalids)
      val outputs = records.flatMap(record => Vector(RecordEncoder.jsonC(record), RecordEncoder.yamlC(record)))
      Then("both element conclusions survive and every invalid output fails through Consequence")
      elements match {
        case Consequence.Failure(conclusion) =>
          conclusion.causes should have size 2
          conclusion.causes.foreach(_ should be_invalid_failure_conclusion)
        case _ => fail("Independent element failures must aggregate")
      }
      outputs.foreach(_expect_invalid)
    }

    "propagate the supplied context to every element through Record contextual reads" in {
      Given("a context-dependent element reader and two distinct explicit execution contexts")
      given ValueReader[ExecutionContext] with {
        def readC(value: Any): Consequence[ExecutionContext] =
          Consequence.valueInvalid("An explicit execution context is required")
        override def readContextC(value: Any)(using ctx: ExecutionContext): Consequence[ExecutionContext] =
          Consequence.success(ctx)
      }
      val contexts = Vector(_context(Locale.JAPAN), _context(Locale.FRANCE))
      val record = Record.data("items" -> NonEmptyVector.of("first", "second"))
      When("each caller context passes through the real contextual field read")
      val results = contexts.map { ctx =>
        record.getAsContextC[NonEmptyVector[ExecutionContext]]("items")(using
          summon[ValueReader[NonEmptyVector[ExecutionContext]]], ctx)
      }
      val plain = record.asC[NonEmptyVector[ExecutionContext]]("items")
      Then("every element receives that exact caller context and plain reads retain element failures")
      results.zip(contexts).foreach { case (result, ctx) =>
        result.get.get.get.toVector.foreach(_ should be theSameInstanceAs ctx)
      }
      _expect_invalid(plain)
    }
  }

  "Normalized semantic laws" should {
    "preserve independently expected values, retained order, normalized roundtrip and idempotence" in {
      forAll(Gen.listOf(Gen.choose(-100000, 100000)),
        Gen.listOf(Gen.oneOf("雪", "🙂", "é", "\n", "null", "")).map(_.mkString)) { (numbers, text) =>
        Given("generated ordered arrays and exact text inside raw nullable object fields")
        val child = Record(Vector(Field("drop", Field.Value.Single(null)),
          Field("text", Field.Value.Single(text)), Field("empty", Field.Value.Single(Record.empty))))
        val input = Record(Vector(Field("first", Field.Value.Single(child)),
          Field("drop", Field.Value.Single(null)), Field("items", Field.Value.Single(numbers.toArray)),
          Field("last", Field.Value.Single(false))))
        val expected = Record.data("first" -> Record.data("text" -> text, "empty" -> Record.empty),
          "items" -> numbers.map(number => new BigDecimal(java.math.BigDecimal.valueOf(number.toLong),
            MathContext.UNLIMITED)).toVector, "last" -> false)
        val expectedjson = Json.obj("first" -> Json.obj("text" -> Json.fromString(text), "empty" -> Json.obj()),
          "items" -> Json.fromValues(numbers.map(Json.fromInt)), "last" -> Json.False)
        When("normalization, ordinary encoding, canonical decoding and normalization again interpret the input")
        val normalized = XSemiStructuredData.normalizer.normalize(input)
        val encoded = RecordEncoder.jsonC(input)
        val decoded = encoded.flatMap(XSemiStructuredData.fromText(_, StructuredDataFormat.Json))
        val repeated = normalized.flatMap(XSemiStructuredData.normalizer.normalize)
        val reencoded = decoded.flatMap(RecordEncoder.jsonC)
        Then("the pure expected structure, ordered JSON and every normalized law agree")
        normalized.get shouldBe Some(expected)
        encoded.get shouldBe Some(expectedjson.noSpaces)
        decoded.get shouldBe Some(expected)
        repeated.get shouldBe normalized.get
        reencoded.get shouldBe encoded.get
        decoded.get.get.fields.map(_.key) shouldBe Vector("first", "items", "last")
        decoded.get.get.getRecord("first").get.fields.map(_.key) shouldBe Vector("text", "empty")
      }
    }

    "retain raw JSON nulls and String text under their separate reader contracts" in {
      Given("raw JSON null shapes and JSON-looking lexical String text")
      val values = Vector(Json.Null, Json.arr(Json.Null, Json.obj("null" -> Json.Null)))
      val text = "  {\"null\": [null], \"text\": \"null\"}\n"
      When("XJson, the Json reader and the String reader interpret their declared inputs")
      val normalized = values.map(XJson.normalizer.normalize)
      val read = values.map(summon[ValueReader[Json]].readC)
      val string = summon[ValueReader[String]].readC(text)
      Then("raw JSON retains the same instances and the lexical String remains exact")
      (normalized ++ read).zip(values ++ values).foreach { case (result, value) =>
        result.get.get should be theSameInstanceAs value
      }
      string.get shouldBe Some(text)
    }
  }

  private def _expect_invalid(result: Consequence[?]): Unit = {
    result match {
      case Consequence.Failure(conclusion) => conclusion should be_invalid_failure_conclusion
      case _ => fail("Expected an invalid value")
    }
  }

  private def _context(locale: Locale): ExecutionContext = {
    val clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)
    val logger = new Logger {
      def trace(message: => String): Unit = ()
      def debug(message: => String): Unit = ()
      def info(message: => String): Unit = ()
      def warn(message: => String): Unit = ()
      def error(message: => String): Unit = ()
      def error(cause: Throwable, message: => String): Unit = ()
      def fatal(message: => String): Unit = ()
      def fatal(cause: Throwable, message: => String): Unit = ()
    }
    val environment = EnvironmentContext.Instant(EnvironmentContext.Core("specification"))
    val vm = VirtualMachineContext.Instant(VirtualMachineContext.Core(
      clock = clock, timezone = ZoneOffset.UTC, encoding = StandardCharsets.UTF_8,
      lineSeparator = "\n", mathContext = MathContext.UNLIMITED, environmentVariables = Map.empty,
      resourceBundleBaseNames = Nil, resourceBundleLocales = Nil, resourceBundleResolutionOrder = Nil))
    val i18n = I18nContext.Instant(I18nContext.Core(textNormalizationPolicy = "JapaneseCanonical",
      textComparisonPolicy = "JapaneseCollation", dateTimeFormatPolicy = "Iso8601", locale = None))
    ExecutionContext.Instant(ExecutionContext.Core(environment = environment, vm = vm, i18n = i18n,
      locale = locale, timezone = ZoneOffset.UTC, encoding = StandardCharsets.UTF_8, clock = clock,
      math = MathContext.UNLIMITED, random = RandomContext.seeded("record-specification"),
      entropy = EntropyContext.deterministic("record-specification"), logger = logger))
  }
}
