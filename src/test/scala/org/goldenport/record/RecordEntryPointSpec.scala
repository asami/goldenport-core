package org.goldenport.record

import java.io.ByteArrayInputStream
import java.math.MathContext
import java.nio.charset.StandardCharsets
import cats.data.NonEmptyVector
import _root_.io.circe.Json
import org.goldenport.{Consequence, ConsequenceException}
import org.goldenport.bag.Bag
import org.goldenport.datatype.PathName
import org.goldenport.record.io.{RecordDecoder as IoRecordDecoder, RecordEncoder,
  RecordExportEncoder, RecordImportDecoder, RecordSourceLoader}
import org.goldenport.schema.{Column, Schema, StructuredDataFormat, ValueDomain,
  XInt, XSemiStructuredData, XString}
import org.goldenport.test.matchers.ConclusionMatchers
import org.goldenport.value.{BaseContent, ContentAttributes}
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * Executable specification for normalized Record construction and ordinary IO.
 *
 * @since   Oct. 18, 2026
 * @version Oct. 18, 2026
 */
final class RecordEntryPointSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks
  with ConclusionMatchers {
  "Record entry points" should {
    "public construction" which {
      "share field normalization while retaining the case-class model and opaque atoms" in {
        Given("ordered fields with null attributes, empty structures, duplicate keys and an opaque atom")
        val opaque = new Object {
          override def toString: String = throw new IllegalStateException("Opaque values must stay opaque")
        }
        val fields = Vector(Field("drop", Field.Value.Single(null)),
          Field("key", Field.Value.Single(1)), Field("empty", Field.Value.Single(Array.empty[Int])),
          Field("key", Field.Value.Single(false)), Field("opaque", Field.Value.Single(opaque)))
        val expected = Vector(Field("key", Field.Value.Single(1)),
          Field("empty", Field.Value.Single(Vector.empty)), Field("key", Field.Value.Single(false)),
          Field("opaque", Field.Value.Single(opaque)))
        When("apply, fromFieldsC, createC, public copy and direct or Mirror product construction interpret the same fields")
        val records = Vector(Record(fields = fields), Record.fromFieldsC(fields).TAKE,
          Record.createC(fields.map(field => field.key -> field.value.single)).TAKE,
          Record.empty.copy(fields = fields), Record.fromProduct(Tuple1(fields)),
          summon[scala.deriving.Mirror.ProductOf[Record]].fromProduct(Tuple1(fields)))
        val Record(extracted) = records.head
        val product = records.head.productElement(0)
        Then("all paths own ordered fields without canonicalizing scalars or inspecting opaque values")
        records.foreach(_.fields shouldBe expected)
        records.distinct should have size 1
        extracted shouldBe expected
        product shouldBe expected
        records.head.getAny("opaque").get.asInstanceOf[AnyRef] should be theSameInstanceAs opaque
        records.head.copy() shouldBe records.head
        Record() shouldBe Record.empty
      }

      "retain empty and falsy values through every data factory" in {
        Given("present empty structures and falsy values alongside native-null and missing optional inputs")
        val pairs = Vector[(String, Any)]("drop" -> null, "child" -> Record.empty,
          "array" -> Array.empty[Int], "list" -> List.empty, "false" -> false,
          "zero" -> 0, "text" -> "null", "empty" -> "")
        val expected = Vector[(String, Any)]("child" -> Record.empty, "array" -> Vector.empty,
          "list" -> Vector.empty, "false" -> false, "zero" -> 0, "text" -> "null", "empty" -> "")
        When("create, createFull, Map, data, dataOption and dataAuto construct their Records")
        val records = Vector(Record.create(pairs), Record.createFull(pairs), Record.data(pairs*),
          Record.dataOption((pairs.map { case (key, value) => key -> Some(value) } :+
            ("missing" -> None))*),
          Record.dataAuto((pairs.map { case (key, value) => key -> Some(value) } :+
            ("missing" -> None))*))
        val mapped = Record.create(pairs.toMap)
        val directauto = Record.dataAuto("child" -> Record.empty, "items" -> Vector.empty,
          "set" -> Set.empty, "drop" -> null)
        Then("every present empty value survives and only null attributes or missing Options disappear")
        records.foreach(record => record.fields.map(field => field.key -> field.value.single) shouldBe expected)
        mapped.asMap shouldBe expected.toMap
        directauto.getAny("child") shouldBe Some(Record.empty)
        directauto.getAny("items") shouldBe Some(Vector.empty)
        directauto.getAny("set") shouldBe Some(Set.empty)
        directauto.getAny("drop") shouldBe None
      }

      "reject malformed field boundaries with structured C failures" in {
        Given("an absent vector, absent Field, null input tuple, null key and absent or unsupported Field.Value")
        val unsupported = new Field.Value {
          def single: Any = 1
          def print: String = "unsupported"
        }
        val vectors = Vector(null, Vector(null), Vector(Field(null, Field.Value.Single(null))),
          Vector(Field("value", null)), Vector(Field("value", unsupported)))
        When("fromFieldsC and createC interpret these actual boundary inputs")
        val results = vectors.map(Record.fromFieldsC) ++ Vector(
          Record.createC(null), Record.createC(Vector(null)),
          Record.createC(Vector(null.asInstanceOf[String] -> 1)))
        val omitted = Record.fromFieldsC(Vector(Field("drop", Field.Value.Single(null))))
        Then("malformed boundaries are invalid and a valid null-valued field is omitted")
        results.foreach(_expect_invalid)
        omitted.get shouldBe Some(Record.empty)
      }

      "reject and aggregate native-null sequence positions without shortening collections" in {
        Given("direct, nested, native-array and NonEmptyVector inputs with invalid positions")
        val values = Vector[Any](Vector[Any](1, null), Vector(Vector[Any](null)),
          Array[Any](null), NonEmptyVector.of[Any](1, null))
        val fields = Vector(Field("first", Field.Value.Single(Vector[Any](null, null))),
          Field("second", Field.Value.Single(Array[Any](null))))
        When("C factories recursively own each sequence and aggregate independent failures")
        val results = values.map(value => Record.createC(Vector("items" -> value)))
        val aggregate = Record.fromFieldsC(fields)
        Then("each invalid position fails at construction and all three independent causes survive")
        results.foreach(_expect_invalid)
        _expect_causes(aggregate, 3)
      }

      "expose construction failures through the convenience APIs without admitting invalid Records" in {
        Given("a null sequence position passed to field-vector and attribute convenience boundaries")
        val fields = Vector(Field("items", Field.Value.Single(Vector[Any](1, null))))
        val pairs = Vector[(String, Any)]("items" -> Vector[Any](1, null))
        val actions = Vector[() => Record](() => Record(fields), () => Record.empty.copy(fields = fields),
          () => Record.fromProduct(Tuple1(fields)),
          () => summon[scala.deriving.Mirror.ProductOf[Record]].fromProduct(Tuple1(fields)),
          () => Record.create(pairs), () => Record.createFull(pairs), () => Record.create(pairs.toMap),
          () => Record.data(pairs*), () => Record.dataAuto(pairs*),
          () => Record.dataOption("items" -> Some(Vector[Any](1, null))))
        When("each convenience API takes the shared construction result")
        val failures = actions.map(action => intercept[ConsequenceException](action()))
        Then("every thrown convenience failure retains the structured invalid construction conclusion")
        failures.foreach(failure => _expect_invalid(failure.consequence))
      }

      "own mutable arrays and mutable sequences recursively while retaining NonEmptyVector shape" in {
        Given("a mutable reference array shared by a mutable sequence and an actual NonEmptyVector")
        val array = Array[Any]("first", 2)
        val sequence = scala.collection.mutable.ArrayBuffer[Any](array, "last")
        val nonempty = NonEmptyVector.of[Any](array)
        When("construction succeeds before callers mutate the original inputs to null and new values")
        val result = Record.createC(Vector("array" -> array, "sequence" -> sequence, "nonempty" -> nonempty))
        array(0) = null
        array(1) = 99
        sequence(0) = null
        sequence(1) = "changed"
        sequence += null
        val encoded = result.flatMap(RecordEncoder.jsonC)
        Then("stored contents and encoding retain the original valid values and collection shape")
        val record = result.TAKE
        record.getAny("array") shouldBe Some(Vector("first", 2))
        record.getAny("sequence") shouldBe Some(Vector(Vector("first", 2), "last"))
        record.getAny("nonempty") shouldBe Some(NonEmptyVector.of[Any](Vector("first", 2)))
        encoded.get shouldBe Some("{\"array\":[\"first\",2],\"sequence\":[[\"first\",2],\"last\"],\"nonempty\":[[\"first\",2]]}")
      }

      "preserve independent expected field and sequence order idempotently across public routes" in {
        forAll(Gen.listOf(Gen.choose(-10000, 10000)), Gen.alphaStr) { (numbers, text) =>
          Given("generated ordered sequence values and duplicate fields with an absent attribute")
          val pairs = Vector[(String, Any)]("key" -> text, "drop" -> null,
            "items" -> numbers.toArray, "key" -> false, "child" -> Record.empty)
          val fields = pairs.map { case (key, value) => Field(key, Field.Value.Single(value)) }
          val expected = Vector(Field("key", Field.Value.Single(text)),
            Field("items", Field.Value.Single(numbers.toVector)), Field("key", Field.Value.Single(false)),
            Field("child", Field.Value.Single(Record.empty)))
          When("public construction, direct or Mirror product construction, copy and factories interpret those inputs")
          val records = Vector(Record(fields), Record.fromFieldsC(fields).TAKE,
            Record.createC(pairs).TAKE, Record.createFull(pairs), Record.data(pairs*),
            Record.dataAuto(pairs*), Record.empty.copy(fields = fields),
            Record.fromProduct(Tuple1(fields)),
            summon[scala.deriving.Mirror.ProductOf[Record]].fromProduct(Tuple1(fields)))
          val repeated = records.map(record => Record.fromFieldsC(record.fields).TAKE)
          Then("independently expected order and contents survive and normalized reconstruction is idempotent")
          records.foreach(_.fields shouldBe expected)
          repeated.map(_.fields) shouldBe records.map(_.fields)
        }
      }
    }

    "immutable replacement and access" which {
      "distinguish null replacement from append and preserve duplicate concatenation order" in {
        Given("a normalized Record with duplicate keys and an independent sibling")
        val record = Record.data("key" -> 1, "sibling" -> false, "key" -> 2)
        val rhs = Record.data("key" -> 3, "empty" -> Record.empty)
        When("replacement, append, concatenation, copy and projection use the public immutable APIs")
        val removed = record.upsertSingle("key", null)
        val replaced = record.upsertSingle("key", Array(7, 8))
        val appended = record.appendField("key", Array(4, 5))
        val absentappend = record.appendField("key", null)
        val combined = Vector(record ++ rhs, record.concatPreservingFields(rhs))
        val filtered = record.filterFields(_.key == "key")
        val projected = record.filterKeys(Set("key"))
        val without = record.removeKeys(Set("key"))
        Then("replacement removes old occurrences, append retains them and all retained fields keep their order")
        removed.fields.map(_.key) shouldBe Vector("sibling")
        replaced.fields.map(_.key) shouldBe Vector("sibling", "key")
        replaced.getVector("key") shouldBe Some(Vector(7, 8))
        appended.fields.map(_.value.single) shouldBe Vector(1, false, 2, Vector(4, 5))
        absentappend should be theSameInstanceAs record
        combined.foreach(_.fields.map(_.value.single) shouldBe Vector(1, false, 2, 3, Record.empty))
        filtered.fields.map(_.value.single) shouldBe Vector(1, 2)
        projected shouldBe filtered
        without shouldBe removed
        record.fields.map(_.value.single) shouldBe Vector(1, false, 2)
      }

      "expose normalized present and missing values while retaining direct path precedence" in {
        Given("normalized fields containing a literal path, nested child, falsy values and an omitted attribute")
        val record = Record.data("drop" -> null, "child" -> Record.data("value" -> "nested"),
          "child/value" -> "literal", "zero" -> 0, "false" -> false,
          "items" -> NonEmptyVector.of("null", ""))
        When("ordinary accessors and typed readers interpret present, missing and path-based values")
        val present = record.getAsC[Int]("zero")
        val missing = record.getAsC[Int]("drop")
        val path = record.getString(PathName.parse("child/value"))
        val strings = record.getStringVector("items")
        val names = record.asNameStringVector
        Then("only missing fields are absent and direct path keys take precedence over nested lookup")
        present.get shouldBe Some(Some(0))
        missing.get shouldBe Some(None)
        record.getAs[Boolean]("false") shouldBe Some(false)
        record.getAny("drop") shouldBe None
        record.asMap.keySet should not contain "drop"
        path shouldBe Some("literal")
        strings shouldBe Some(Vector("null", ""))
        names should contain ("zero" -> "0")
        names should contain ("false" -> "false")
      }

      "deliver empty references from the real ContentAttributes dataAuto consumer" in {
        Given("the actual empty ContentAttributes model")
        val attributes = ContentAttributes.empty
        When("its public Record projection and ordinary encoding interpret empty references")
        val record = attributes.toRecord()
        val encoded = RecordEncoder.jsonC(record)
        Then("references remain an explicit empty array and missing optional content stays absent")
        record.fields.map(_.key) shouldBe Vector("references")
        record.getVector("references") shouldBe Some(Vector.empty)
        encoded.get shouldBe Some("{\"references\":[]}")
      }
    }
  }

  "Ordinary Record ingress" should {
    "JSON and YAML decoding" which {
      "share exact nested semantics through single, plural, auto, Bag and InputStream routes" in {
        Given("JSON-compatible YAML with exact large numbers, omitted attributes and empty nested values")
        val number = "123456789012345678901234567890.12345678901234567890123456789"
        val text = s"""{"number":$number,"drop":null,"child":{"drop":null},"items":[{},[],false,0,"null"]}"""
        val expected = Record.data("number" -> _decimal(number), "child" -> Record.empty,
          "items" -> Vector(Record.empty, Vector.empty, false, BigDecimal(0), "null"))
        val decoder = IoRecordDecoder()
        When("the real ordinary decoder interprets every supported single and collection wrapper")
        val singles = Vector(decoder.json(text), decoder.yaml(text), decoder.json(Bag.text(text)),
          decoder.yaml(ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))))
        val plural = Vector(decoder.jsonRecords(s"[$text]"), decoder.yamlRecords(s"[$text]"),
          decoder.jsonAutoRecords(text), decoder.yamlAutoRecords(text),
          decoder.jsonAutoRecords(s"[$text]"), decoder.yamlAutoRecords(s"[$text]"))
        Then("all routes retain the same exact canonical values and object/array order")
        singles.foreach(_.get shouldBe Some(expected))
        plural.foreach(_.get shouldBe Some(Vector(expected)))
        singles.foreach(_.TAKE.fields.map(_.key) shouldBe Vector("number", "child", "items"))
      }

      "accept explicit empty lists while rejecting scalar, null and empty YAML roots" in {
        Given("explicit empty lists, object-only plural inputs and scalar or empty document roots")
        val decoder = IoRecordDecoder()
        val yamlroots = Vector("", "---\n", "null", "~", "42", "true", "text")
        val jsonroots = Vector("null", "42", "true", "\"text\"")
        When("ordinary single, plural and auto decoding interpret the declared root shape")
        val empty = Vector(decoder.jsonRecords("[]"), decoder.jsonAutoRecords("[]"),
          decoder.yamlRecords("[]"), decoder.yamlAutoRecords("[]"))
        val failures = yamlroots.flatMap(text => Vector(decoder.yaml(text), decoder.yamlRecords(text),
          decoder.yamlAutoRecords(text))) ++ jsonroots.flatMap(text => Vector(decoder.json(text),
          decoder.jsonRecords(text), decoder.jsonAutoRecords(text))) ++ Vector(
          decoder.json("[]"), decoder.yaml("[]"), decoder.jsonRecords("{}"), decoder.yamlRecords("{}"))
        Then("only explicit collection roots succeed as empty and an empty YAML document is invalid")
        empty.foreach(_.get shouldBe Some(Vector.empty))
        failures.foreach(_expect_invalid)
      }

      "reject null positions at every depth and aggregate independent record failures" in {
        Given("object and collection inputs containing native JSON/YAML null array positions")
        val decoder = IoRecordDecoder()
        val objecttext = """{"items":[1,null],"nested":{"items":[[null]]}}"""
        When("single and plural canonical decoder routes interpret these invalid positions")
        val singles = Vector(decoder.json(objecttext), decoder.yaml(objecttext))
        val plurals = Vector(decoder.jsonRecords(s"[$objecttext,$objecttext]"),
          decoder.yamlAutoRecords(s"[$objecttext,$objecttext]"), decoder.jsonRecords("[null]"),
          decoder.yamlRecords("[null]"))
        Then("positions are never filtered and each independent nested failure remains available")
        singles.foreach(_expect_causes(_, 2))
        plurals.take(2).foreach(_expect_causes(_, 4))
        plurals.drop(2).foreach(_expect_invalid)
      }

      "retain exact YAML numeric and boolean lexemes without guessing quoted scalars" in {
        Given("YAML exact hexadecimal, base-60, decimal, boolean and quoted scalar forms")
        val text = "hex: 0x20000000000001\nsexagesimal: 1:02:03.25\n" +
          "decimal: 12345678901234567890.1234567890123456789\nboolean: YES\nquoted: 'null'\n"
        When("ordinary YAML decoding uses the shared Node converter")
        val decoded = IoRecordDecoder().yaml(text)
        Then("numbers are exact BigDecimals and declared strings and booleans retain their meaning")
        decoded.TAKE.getAny("hex") shouldBe Some(BigDecimal("9007199254740993"))
        decoded.TAKE.getAny("sexagesimal") shouldBe Some(BigDecimal("3723.25"))
        decoded.TAKE.getAny("decimal") shouldBe Some(_decimal("12345678901234567890.1234567890123456789"))
        decoded.TAKE.getAny("boolean") shouldBe Some(true)
        decoded.TAKE.getAny("quoted") shouldBe Some("null")
      }

      "validate bad and duplicate YAML keys before omitting null-valued members" in {
        Given("null-valued duplicate, non-string, null and merge mapping keys")
        val texts = Vector("key: null\nkey: 1", "key: 1\nkey: null", "1: null",
          "? null\n: null", "<<: null")
        val decoder = IoRecordDecoder()
        When("single and plural ordinary YAML routes interpret all mapping keys")
        val results = texts.flatMap(text => Vector(decoder.yaml(text),
          decoder.yamlRecords("- " + text.replace("\n", "\n  "))))
        Then("invalid keys cannot disappear with omitted attributes")
        results.foreach(_expect_invalid)
      }

      "preserve alias security and syntax failures on the ordinary routes" in {
        Given("cyclic aliases, unsupported tags, multiple documents and malformed syntax")
        val decoder = IoRecordDecoder()
        val invalids = Vector("node: &a [*a]", "date: 2026-10-18", "value: !custom text",
          "number: .inf", "values: !!set {a: null}")
        val malformed = Vector("items: [", "a: 1\n---\nb: 2")
        val reused = "first: &item {value: 1}\nsecond: *item\n"
        When("ordinary decoding composes Node graphs without constructing application objects")
        val rejected = invalids.map(decoder.yaml)
        val formats = malformed.map(decoder.yaml) ++ Vector(decoder.json("{"), decoder.json(null: String),
          decoder.yaml(null: String))
        val aliases = decoder.yaml(reused)
        Then("unsupported or cyclic graphs are invalid, syntax errors retain FormatError and acyclic aliases survive")
        rejected.foreach(_expect_invalid)
        formats.foreach(_expect_format)
        aliases.TAKE.getRecord("first") shouldBe Some(Record.data("value" -> BigDecimal(1)))
        aliases.TAKE.getRecord("second") shouldBe aliases.TAKE.getRecord("first")
      }
    }

    "literal HOCON and ordinary configuration sources" which {
      "retain empty objects, literal dotted keys and nested list objects through resolved source loading" in {
        Given("full HOCON with a local substitution, dotted keys, nested objects and omitted null members")
        val text = """base = 20
          |age = ${base}
          |"literal.key" = "kept"
          |nested.key = "nested"
          |empty = {}
          |items = [{ drop = null }, { n = 1 }, []]
          |drop = null
          |""".stripMargin
        When("the source loader parses and resolves before shared direct Config conversion")
        val loaded = RecordSourceLoader.load(text, RecordFormat.Hocon)
        val literal = XSemiStructuredData.fromText(text, StructuredDataFormat.Hocon)
        Then("full-config features survive while strict literal HOCON continues to reject substitutions")
        val record = loaded.TAKE
        record.getAny("age") shouldBe Some(BigDecimal(20))
        record.getAny("literal.key") shouldBe Some("kept")
        record.getRecord("nested").flatMap(_.getString("key")) shouldBe Some("nested")
        record.getRecord("empty") shouldBe Some(Record.empty)
        record.getAny("items") shouldBe Some(Vector(Record.empty, Record.data("n" -> BigDecimal(1)), Vector.empty))
        record.getAny("drop") shouldBe None
        _expect_invalid(literal)
      }

      "retain exact literal HOCON numbers and declared full-config string meaning" in {
        Given("a literal integer beyond Long, an exact decimal and a numeric-looking quoted String")
        val integer = "12345678901234567890123456789012345678901234567890"
        val decimal = "12345678901234567890.12345678901234567890123456789"
        val text = s"number = $integer\ndecimal = $decimal\nquoted = \"$integer\"\nempty = {}"
        When("strict literal parsing adapts integer tokens while full-config loading uses declared Config types")
        val literal = XSemiStructuredData.fromText(text, StructuredDataFormat.Hocon)
        val loaded = RecordSourceLoader.load(text, RecordFormat.Hocon)
        Then("literal exact numbers remain exact and resolved Config strings are never guessed as numbers")
        literal.TAKE.getAny("number") shouldBe Some(_decimal(integer))
        literal.TAKE.getAny("decimal") shouldBe Some(_decimal(decimal))
        literal.TAKE.getAny("quoted") shouldBe Some(integer)
        loaded.TAKE.getAny("number") shouldBe Some(integer)
        loaded.TAKE.getAny("decimal") shouldBe Some(_decimal(decimal))
        loaded.TAKE.getRecord("empty") shouldBe Some(Record.empty)
      }

      "reject null configuration list elements and malformed sources without changing literal include policy" in {
        Given("invalid HOCON list positions, malformed syntax and a strict literal include directive")
        val invalid = "items = [{ values = [1, null] }]"
        val importer = RecordImportDecoder()
        When("source loading, import and strict literal parsing interpret these declared inputs")
        val results = Vector(RecordSourceLoader.load(invalid, RecordFormat.Hocon),
          importer.decode(invalid, RecordImportDecoder.RecordImportFormat.Hocon),
          XSemiStructuredData.fromText(invalid, StructuredDataFormat.Hocon),
          importer.decode("records = [{ value = 1 }]\nignored = [null]",
            RecordImportDecoder.RecordImportFormat.Hocon))
        val syntax = RecordSourceLoader.load("items = [", RecordFormat.Hocon)
        val included = XSemiStructuredData.fromText("include \"never-read.conf\"", StructuredDataFormat.Hocon)
        Then("null positions fail and malformed or forbidden literal includes do not become empty Records")
        results.foreach(_expect_invalid)
        syntax.isSuccess shouldBe false
        _expect_format(included)
      }

      "preserve first nonempty records-items-data priority and scalar wrapping after whole-root conversion" in {
        Given("full-config containers including empty lists, mapping values, scalars and a null member")
        val texts = Vector("records = []\nitems = [{ age = 20 }]\ndata = [{ age = 30 }]",
          "records = { age = 20 }\nitems = [{ age = 30 }]", "records = [20, { age = 30 }]",
          "records = false", "records = []\nitems = []\ndata = []\nempty = {}", "records = null\nvalue = 1")
        val importer = RecordImportDecoder()
        When("ordinary HOCON import selects containers from the converted root")
        val results = texts.map(importer.decode(_, RecordImportDecoder.RecordImportFormat.Hocon))
        Then("the first nonempty container wins, scalars wrap as value and empty containers fall back to the root")
        results(0).TAKE.records shouldBe Vector(Record.data("age" -> BigDecimal(20)))
        results(1).TAKE.records shouldBe Vector(Record.data("age" -> BigDecimal(20)))
        results(2).TAKE.records shouldBe Vector(Record.data("value" -> BigDecimal(20)), Record.data("age" -> BigDecimal(30)))
        results(3).TAKE.records shouldBe Vector(Record.data("value" -> false))
        results(4).TAKE.records.head.getAny("records") shouldBe Some(Vector.empty)
        results(4).TAKE.records.head.getRecord("empty") shouldBe Some(Record.empty)
        results(5).TAKE.records shouldBe Vector(Record.data("value" -> BigDecimal(1)))
      }

      "retain schema-coerced Int values and present String conversion after canonical import" in {
        Given("a resolved HOCON row and an explicit integer and String schema")
        val text = "base = 20\nrecords = [{ age = ${base}, label = 20 }]"
        val schema = Schema(Vector(Column(BaseContent.Builder("age").build(), ValueDomain(XInt)),
          Column(BaseContent.Builder("label").build(), ValueDomain(XString))))
        val options = RecordImportDecoder.RecordImportOptions(schema = Some(schema), coerceBySchema = true)
        When("ordinary import applies existing schema shaping after root conversion")
        val result = RecordImportDecoder().decode(text, RecordImportDecoder.RecordImportFormat.Hocon, options)
        Then("age is Int20 and String coercion uses the normalized present value")
        result.TAKE.records.head.getAny("age") shouldBe Some(20)
        result.TAKE.records.head.getAny("age").get shouldBe a[Int]
        result.TAKE.records.head.getAny("label") shouldBe Some("20")
        result.TAKE.issues shouldBe empty
      }

      "recursively convert TOML arrays and table objects while retaining supported scalar values" in {
        Given("TOML nested inline tables, lists, booleans and ordinary table syntax")
        val text = "items = [{ child = {}, values = [1, 2] }, { enabled = false }]\n" +
          "[service]\nname = 'null'\nempty = []\n"
        When("the TOML foreign-map ingress feeds source loading and ordinary import")
        val loaded = RecordSourceLoader.load(text, RecordFormat.Toml)
        val imported = RecordImportDecoder().decode(text, RecordImportDecoder.RecordImportFormat.Toml)
        val malformed = RecordSourceLoader.load("items = [", RecordFormat.Toml)
        val invalidnull = RecordSourceLoader.load("items = [null]", RecordFormat.Toml)
        Then("objects become Records, collections become Vectors and empty and falsy values stay present")
        val expected = Vector(Record.data("child" -> Record.empty, "values" -> Vector(BigDecimal(1), BigDecimal(2))),
          Record.data("enabled" -> false))
        loaded.TAKE.getAny("items") shouldBe Some(expected)
        loaded.TAKE.getRecord("service").flatMap(_.getString("name")) shouldBe Some("null")
        loaded.TAKE.getRecord("service").flatMap(_.getVector("empty")) shouldBe Some(Vector.empty)
        imported.TAKE.records shouldBe Vector(loaded.TAKE)
        malformed.isSuccess shouldBe false
        invalidnull.isSuccess shouldBe false
      }
    }
  }

  "Ordinary Record output" should {
    "XML display" which {
      "traverse nested NonEmptyVectors and empty children without changing scalar display or escaped names" in {
        Given("actual nested NonEmptyVectors, empty child structures and an opaque display scalar")
        val opaque = new Object { override def toString: String = "opaque<&" }
        val record = Record.data("1 items" -> NonEmptyVector.of[Any](Record.empty,
          NonEmptyVector.of[Any](false, Vector.empty), Record.data("empty" -> Record.empty)),
          "scalar" -> opaque)
        When("ordinary XML encoding traverses the existing field-item display model")
        val result = RecordEncoder.xmlC(record)
        Then("nonempty collections use item elements, empty children stay empty and scalar text is XML escaped")
        result.get shouldBe Some("<record><_1_items><item/><item><item>false</item><item/></item>" +
          "<item><empty/></item></_1_items><scalar>opaque&lt;&amp;</scalar></record>")
      }
    }

    "canonical row exports" which {
      "roundtrip JSON, YAML and HOCON objects and render TSL with the same canonical values" in {
        Given("rows with quoted keys, Unicode, exact numbers, empty roots and nested NonEmptyVectors")
        val rows = Vector(Record.data("literal.key" -> "雪🙂", "quote\"key" -> "null",
          "number" -> _decimal("12345678901234567890.1234567890123456789"),
          "items" -> NonEmptyVector.of[Any](Record.empty, NonEmptyVector.of(2, 1))), Record.empty)
        val expected = Vector(Record.data("literal.key" -> "雪🙂", "quote\"key" -> "null",
          "number" -> _decimal("12345678901234567890.1234567890123456789"),
          "items" -> Vector(Record.empty, Vector(BigDecimal(2), BigDecimal(1)))), Record.empty)
        val encoder = RecordExportEncoder()
        val decoder = IoRecordDecoder()
        When("real row exporters feed ordinary JSON/YAML decoding and HOCON import")
        val json = encoder.encodeText(rows, RecordFormat.Json)
        val yaml = encoder.encodeText(rows, RecordFormat.Yaml)
        val hocon = encoder.encodeText(rows, RecordFormat.Hocon)
        val tsl = encoder.encodeText(rows, RecordFormat.Tsl)
        val decoded = Vector(json.flatMap(decoder.jsonRecords), yaml.flatMap(decoder.yamlRecords))
        val decodedhocon = hocon.flatMap(text => RecordImportDecoder().decode(text,
          RecordImportDecoder.RecordImportFormat.Hocon)).map(_.records)
        val tsldecoded = tsl.flatMap(decoder.jsonRecords)
        Then("every canonical export preserves object and sequence meaning without stringifying collections")
        decoded.foreach(_.get shouldBe Some(expected))
        decodedhocon.TAKE.map(_.fields.sortBy(_.key)) shouldBe expected.map(_.fields.sortBy(_.key))
        tsldecoded.get shouldBe Some(expected)
        json.TAKE should not include "NonEmptyVector"
      }

      "roundtrip signed integral overflow as exact numbers through HOCON import and source loading" in {
        def _roundtrip_(positive: BigInt, negative: BigInt): Unit = {
          Given("signed scale-zero integers beyond Long at root, child and nested collection positions with scalar and empty controls")
          val expectedpositive = _decimal(positive.toString)
          val expectednegative = _decimal(negative.toString)
          val fractional = _decimal("12345678901234567890.1234567890123456789")
          val row = Record.data("positive" -> positive, "negative" -> negative,
            "wholedecimal" -> expectedpositive, "maximum" -> Long.MaxValue, "minimum" -> Long.MinValue,
            "fractional" -> fractional, "quoted" -> positive.toString,
            "child" -> Record.data("positive" -> positive, "negative" -> negative),
            "items" -> Vector(Vector(positive, negative), NonEmptyVector.of[Any](negative, positive)),
            "emptychild" -> Record.empty, "emptylist" -> Vector.empty)
          val encoder = RecordExportEncoder()
          val importer = RecordImportDecoder()
          When("the actual HOCON row exporter feeds ordinary import and full-config source loading")
          val exported = encoder.encodeText(Vector(row), RecordFormat.Hocon)
          val imported = exported.flatMap(importer.decode(_, RecordImportDecoder.RecordImportFormat.Hocon))
          val loaded = exported.flatMap(RecordSourceLoader.load(_, RecordFormat.Hocon))
          Then("both consumers retain exact BigDecimal values, declared Strings, empty structures and sequence order")
          val importedrows = imported.TAKE.records
          val loadedrows = loaded.TAKE.getVector("records").get
          importedrows should have size 1
          loadedrows should have size 1
          Vector(importedrows.head, loadedrows.head.asInstanceOf[Record]).foreach { record =>
            record.getAny("positive").get shouldBe a[BigDecimal]
            record.getAny("negative").get shouldBe a[BigDecimal]
            record.getBigDecimal("positive") shouldBe Some(expectedpositive)
            record.getBigDecimal("negative") shouldBe Some(expectednegative)
            record.getAny("wholedecimal") shouldBe Some(expectedpositive)
            record.getBigDecimal("maximum") shouldBe Some(_decimal(Long.MaxValue.toString))
            record.getBigDecimal("minimum") shouldBe Some(_decimal(Long.MinValue.toString))
            record.getBigDecimal("fractional") shouldBe Some(fractional)
            record.getAny("quoted") shouldBe Some(positive.toString)
            val child = record.getRecord("child").get
            child.getAny("positive").get shouldBe a[BigDecimal]
            child.getAny("negative").get shouldBe a[BigDecimal]
            child.getBigDecimal("positive") shouldBe Some(expectedpositive)
            child.getBigDecimal("negative") shouldBe Some(expectednegative)
            record.getVector("items") shouldBe Some(Vector(
              Vector(expectedpositive, expectednegative), Vector(expectednegative, expectedpositive)))
            record.getRecord("emptychild") shouldBe Some(Record.empty)
            record.getVector("emptylist") shouldBe Some(Vector.empty)
          }
        }

        forAll(Gen.choose(1L, 1000000L), Gen.choose(1L, 1000000L)) { (positiveoffset, negativeoffset) =>
          _roundtrip_(BigInt(Long.MaxValue) + positiveoffset, BigInt(Long.MinValue) - negativeoffset)
        }
        Vector(80, 400).foreach { digits =>
          val integer = BigInt("9" * digits)
          _roundtrip_(integer, -integer)
        }
      }

      "render empty row collections as explicit arrays and an empty HOCON records container" in {
        Given("an empty collection of Records")
        val encoder = RecordExportEncoder()
        val decoder = IoRecordDecoder()
        When("each canonical row format renders the empty collection")
        val json = encoder.encodeText(Vector.empty, RecordFormat.Json)
        val yaml = encoder.encodeText(Vector.empty, RecordFormat.Yaml)
        val hocon = encoder.encodeText(Vector.empty, RecordFormat.Hocon)
        val decoded = Vector(json.flatMap(decoder.jsonRecords), yaml.flatMap(decoder.yamlRecords))
        val config = hocon.flatMap(RecordSourceLoader.load(_, RecordFormat.Hocon))
        Then("JSON and YAML decode as empty arrays and full-config loading retains the explicit empty records list")
        _root_.io.circe.parser.parse(json.TAKE) shouldBe Right(Json.arr())
        yaml.get shouldBe Some("[]\n")
        decoded.foreach(_.get shouldBe Some(Vector.empty))
        config.TAKE.getVector("records") shouldBe Some(Vector.empty)
      }

      "retain aggregated canonical failures for unknown, raw-JSON, duplicate and nonfinite members" in {
        Given("rows that construction owns but canonical structured formats cannot represent")
        val rows = Vector(Record.data("unknown" -> new Object()), Record.data("raw" -> Json.Null))
        val invalids = Vector(Record.data("number" -> Double.NaN), Record.data("key" -> 1, "key" -> 2))
        val formats = Vector(RecordFormat.Json, RecordFormat.Yaml, RecordFormat.Hocon, RecordFormat.Tsl)
        val encoder = RecordExportEncoder()
        When("each semantic row export uses canonical Consequence conversion")
        val aggregated = formats.map(encoder.encodeText(rows, _))
        val rejected = formats.flatMap(format => invalids.map(record => encoder.encodeText(Vector(record), format)))
        Then("canonical failures remain invalid and independent row causes are preserved directly")
        aggregated.foreach(_expect_causes(_, 2))
        rejected.foreach(_expect_invalid)
      }
    }

    "text and spreadsheet cells" which {
      "render collection cells semantically and retain missing columns as empty CSV and Excel cells" in {
        Given("rows with a real NonEmptyVector cell, a nested Record and independently missing columns")
        val rows = Vector(Record.data("id" -> "first", "items" -> NonEmptyVector.of[Any](
          Record.empty, NonEmptyVector.of(1, 2))), Record.data("id" -> "second", "extra" -> "present"))
        val encoder = RecordExportEncoder()
        val importer = RecordImportDecoder()
        When("CSV and Excel export render the union of columns and ordinary import reads the cells")
        val csv = encoder.encodeText(rows, RecordFormat.Csv)
        val excel = encoder.encodeBytes(rows, RecordFormat.Excel)
        val decodedcsv = csv.flatMap(importer.decode(_, RecordImportDecoder.RecordImportFormat.Csv))
        val decodedexcel = excel.flatMap(result => importer.decodeBytes(result.bytes,
          RecordImportDecoder.RecordImportFormat.Excel))
        Then("the collection cell is canonical JSON text and missing cells stay empty without null sentinels")
        csv.TAKE shouldBe "id,items,extra\nfirst,\"[{},[1,2]]\",\nsecond,,present\n"
        Vector(decodedcsv, decodedexcel).foreach { result =>
          result.TAKE.records.head.getString("items") shouldBe Some("[{},[1,2]]")
          result.TAKE.records.head.getAny("extra") shouldBe None
          result.TAKE.records(1).getAny("items") shouldBe None
          result.TAKE.records(1).getString("extra") shouldBe Some("present")
        }
      }
    }
  }

  private def _decimal(text: String): BigDecimal =
    new BigDecimal(new java.math.BigDecimal(text, MathContext.UNLIMITED), MathContext.UNLIMITED)

  private def _expect_invalid(result: Consequence[?]): Unit =
    result match {
      case Consequence.Failure(conclusion) => conclusion should be_invalid_failure_conclusion
      case _ => fail("Expected a structured invalid value")
    }

  private def _expect_format(result: Consequence[?]): Unit =
    result match {
      case Consequence.Failure(conclusion) => conclusion should be_format_error_failure_conclusion
      case _ => fail("Expected a structured format failure")
    }

  private def _expect_causes(result: Consequence[?], count: Int): Unit =
    result match {
      case Consequence.Failure(conclusion) =>
        conclusion.causes should have size count
        conclusion.causes.foreach(_ should be_invalid_failure_conclusion)
      case _ => fail("Expected independent structured invalid causes")
    }
}
