package org.goldenport.schema

import java.math.MathContext
import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.record.Record
import org.goldenport.test.matchers.ConclusionMatchers
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * Executable specification for explicit structured text formats.
 *
 * @since   Oct.  5, 2026
 * @version Oct. 18, 2026
 * @author  ASAMI, Tomoharu
 */
final class StructuredDataTextSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks
  with ConclusionMatchers {
  "Explicit structured formats (R10)" should {
    "produce the same canonical Record from each declared syntax" in {
      Given("equivalent JSON, YAML and literal HOCON objects and their explicit selectors")
      val inputs = Vector(
        StructuredDataFormat.Json -> "{\"name\":\"雪\",\"count\":7,\"absent\":null,\"items\":[true,false]}",
        StructuredDataFormat.Yaml -> "name: 雪\ncount: 7\nabsent: null\nitems: [true, false]\n",
        StructuredDataFormat.Hocon -> "name=雪\ncount=7\nabsent=null\nitems=[true,false]")
      val expected = Record.createFull(Vector("name" -> "雪", "count" -> BigDecimal(7),
        "items" -> Vector(true, false)))
      val expectedjson = Json.obj("name" -> Json.fromString("雪"), "count" -> Json.fromInt(7),
        "items" -> Json.arr(Json.True, Json.False))
      When("each text is decoded using its declared format")
      val results = inputs.map { case (format, text) => XSemiStructuredData.fromText(text, format) }
      val encoded = results.map(_.flatMap(XSemiStructuredData.toJson))
      val implicitresult = XSemiStructuredData.normalizer.normalize(inputs.head._2)
      Then("all decoded objects share canonical meaning and undecoded text remains invalid")
      results.map(_.get.map(_.fields.sortBy(_.key))) shouldBe Vector.fill(3)(Some(expected.fields.sortBy(_.key)))
      encoded.map(_.get) shouldBe Vector.fill(3)(Some(expectedjson))
      _expect_invalid(implicitresult)
      StructuredDataFormat.values.toVector shouldBe Vector(
        StructuredDataFormat.Json, StructuredDataFormat.Yaml, StructuredDataFormat.Hocon)
    }

    "omit null attributes while preserving empty structures, Unicode and literal dotted keys" in {
      Given("JSON text valid in all formats, with nested empty and populated shapes")
      val json = Json.obj("empty" -> Json.obj(), "array" -> Json.arr(), "null" -> Json.Null,
        "a.b" -> Json.fromString("雪🙂"), "a" -> Json.obj("b" -> Json.True),
        "nested" -> Json.arr(Json.arr(), Json.arr(Json.obj("absent" -> Json.Null))))
      val expected = _expected_json(json)
      When("all selected formats decode and canonically encode the object and an empty root")
      val results = StructuredDataFormat.values.map { format =>
        XSemiStructuredData.fromText(json.noSpaces, format).flatMap(XSemiStructuredData.toJson)
      }
      val emptyresults = StructuredDataFormat.values.map(XSemiStructuredData.fromText("{}", _))
      Then("retained members and distinct literal keys survive while empty children remain")
      results.foreach(_.get shouldBe Some(expected))
      emptyresults.foreach(_.get shouldBe Some(Record.empty))
    }

    "retain large integers and high precision fractional and exponent values exactly" in {
      Given("numbers beyond Long range and Double precision, written in all three syntaxes")
      val lexemes = Vector("12345678901234567890123456789012345678901234567890",
        "123456789012345678901234567890.12345678901234567890123456789",
        "-123456789012345678901234567890123456789.1234567890123456789e-50")
      val text = "{\"numbers\":[" + lexemes.mkString(",") + "]}"
      val expected = Json.obj("numbers" -> Json.fromValues(lexemes.map(value =>
        Json.fromBigDecimal(_decimal(value)))))
      When("each selected decoder produces canonical values and JSON")
      val records = StructuredDataFormat.values.map(XSemiStructuredData.fromText(text, _))
      val results = records.map(_.flatMap(XSemiStructuredData.toJson))
      Then("numeric members become exact BigDecimals and their JSON meaning survives")
      records.foreach(_.get.get.fields.head.value.single.asInstanceOf[Vector[Any]]
        .foreach(_ shouldBe a[BigDecimal]))
      results.foreach(_.get shouldBe Some(expected))
    }

    "roundtrip generated normalized decimals, Unicode, nested objects and arrays" in {
      forAll(_object_gen) { json =>
        Given("a generated canonical object serialized as JSON text valid in all three formats")
        val text = json.noSpaces
        val expected = _expected_json(json)
        When("each declared format decodes and encodes that text")
        val results = StructuredDataFormat.values.map { format =>
          XSemiStructuredData.fromText(text, format).flatMap(XSemiStructuredData.toJson)
        }
        Then("all decoded values match independent recursive attribute omission")
        results.foreach(_.get shouldBe Some(expected))
      }
    }
  }

  "YAML node interpretation (R10-R11)" should {
    "interpret signed radix, octal, base-60 and underscored numbers without rounding" in {
      Given("YAML integers and floats in the supported original scalar spellings")
      val text = "values: [0b1010, -0b1010, +0xFF, -0xFF, 012, -012, 1_000, " +
        "1:02:03, -1:02:03, 1:02:03.12345678901234567890123456789, " +
        "-1:02:03.12345678901234567890123456789, 1.234_567e+40]"
      val expected = Vector("10", "-10", "255", "-255", "10", "-10", "1000",
        "3723", "-3723", "3723.12345678901234567890123456789",
        "-3723.12345678901234567890123456789", "1.234567e+40").map(_decimal)
      When("the selected YAML decoder interprets the number nodes")
      val result = XSemiStructuredData.fromText(text, StructuredDataFormat.Yaml)
      Then("all supported numbers are exact canonical BigDecimals")
      result.get.get.fields.head.value.single shouldBe expected
    }

    "use tags to distinguish booleans and numbers from quoted strings and keys" in {
      Given("all YAML boolean spellings, quoted equivalents, a null-valued empty key and a numeric string key")
      val text = "values: [true, FALSE, yes, No, ON, off, \"true\", \"012\"]\n" +
        "\"\": null\n\"123\": {}\n"
      When("YAML node tags determine canonical member types")
      val result = XSemiStructuredData.fromText(text, StructuredDataFormat.Yaml)
      Then("boolean nodes become booleans, quoted scalars remain strings and the null attribute is omitted")
      result.get.get.fields.head.value.single shouldBe Vector(true, false, true, false,
        true, false, "true", "012")
      result.get.get.fields.map(_.key) shouldBe Vector("values", "123")
      result.get.get.fields(1).value.single shouldBe Record.empty
    }

    "decode independent acyclic alias reuse without omitting members" in {
      Given("a shared anchored object with a null attribute reused independently and inside an array")
      val text = "original: &shared {empty: {}, absent: null, values: [雪]}\n" +
        "copy: *shared\narray: [*shared, *shared]\n"
      val shared = Json.obj("empty" -> Json.obj(), "values" -> Json.arr(Json.fromString("雪")))
      val expected = Json.obj("original" -> shared, "copy" -> shared,
        "array" -> Json.arr(shared, shared))
      When("YAML aliases are recursively decoded and canonically encoded")
      val result = XSemiStructuredData.fromText(text, StructuredDataFormat.Yaml)
        .flatMap(XSemiStructuredData.toJson)
      Then("each independent reuse retains the normalized anchored value with its null attribute omitted")
      result.get shouldBe Some(expected)
    }

    "reject non-string keys, duplicate keys and merge keys at nested levels" in {
      Given("YAML mappings with invalid keys or duplicate members")
      val inputs = Vector("1: value", "? [a,b]\n: value", "a: 1\na: 2",
        "nested: {a: 1, a: 2}", "base: &base {a: 1}\nnested: {<<: *base}")
      When("the selected YAML decoder interprets those mappings")
      val results = inputs.map(XSemiStructuredData.fromText(_, StructuredDataFormat.Yaml))
      Then("every unsupported key contract produces an invalid semiStructuredData outcome")
      results.foreach(_expect_invalid)
    }

    "reject unsupported tags, nonfinite numbers and cyclic aliases" in {
      Given("custom, timestamp, binary, set, nonfinite and cyclic YAML nodes")
      val inputs = Vector("value: !custom text", "value: 2026-10-05",
        "value: !!binary SGVsbG8=", "value: !!set {a: null}", "value: .inf",
        "value: -.Inf", "value: .NaN", "value: &cycle [*cycle]",
        "value: &cycle {self: *cycle}", "!custom {value: 1}")
      When("the selected YAML decoder interprets those nodes")
      val results = inputs.map(XSemiStructuredData.fromText(_, StructuredDataFormat.Yaml))
      Then("unsupported members fail explicitly with concise invalid outcomes")
      results.foreach(_expect_invalid)
    }

    "aggregate independent invalid mapping and sequence members" in {
      Given("two unsupported sequence tags and one unsupported sibling tag")
      val text = "array: [!custom a, !custom b]\nsibling: !custom c"
      When("the YAML decoder validates the independent members")
      val result = XSemiStructuredData.fromText(text, StructuredDataFormat.Yaml)
      Then("all three invalid member conclusions are retained")
      result match {
        case Consequence.Failure(conclusion) => conclusion.causes should have size 3
        case _ => fail("Unsupported YAML members must produce aggregated failure")
      }
    }

    "reject single documents whose roots are not mappings" in {
      Given("valid YAML sequence, scalar, null and empty document roots")
      val inputs = Vector("[]", "text", "null", "")
      When("the selected YAML decoder interprets those document roots")
      val results = inputs.map(XSemiStructuredData.fromText(_, StructuredDataFormat.Yaml))
      Then("each root is an invalid semiStructuredData value")
      results.foreach(_expect_invalid)
    }

    "reject multiple documents and malformed YAML as format failures" in {
      Given("multiple YAML documents and an unfinished collection")
      val inputs = Vector("---\na: 1\n---\nb: 2", "value: [")
      When("the selected YAML parser composes a single document")
      val results = inputs.map(XSemiStructuredData.fromText(_, StructuredDataFormat.Yaml))
      Then("each input produces a semiStructuredData format failure")
      results.foreach(_expect_format)
    }
  }

  "Literal HOCON interpretation (R10-R11)" should {
    "preserve exact integer meaning and declared string compatibility" which {
      "retain native positive and negative oversized integers in nested and multiline values" in {
        Given("bare HOCON integers outside Long, a 320-digit integer and native whitespace")
        val positive = "9223372036854775808"
        val negative = "-9223372036854775809"
        val enormous = "9" * 320
        val text = "nested { values=[" + positive + ", [" + negative + ", {huge=" + enormous +
          "}]] }\nmultiline=\n\u00a0" + positive + "\nnegative=\n\u2007" + negative +
          "\nwhitespace=[\u202f" + positive + ",\ufeff" + negative + "]"
        val expected = Json.obj(
          "nested" -> Json.obj("values" -> Json.arr(Json.fromBigDecimal(_decimal(positive)),
            Json.arr(Json.fromBigDecimal(_decimal(negative)),
              Json.obj("huge" -> Json.fromBigDecimal(_decimal(enormous)))))),
          "multiline" -> Json.fromBigDecimal(_decimal(positive)),
          "negative" -> Json.fromBigDecimal(_decimal(negative)),
          "whitespace" -> Json.arr(Json.fromBigDecimal(_decimal(positive)),
            Json.fromBigDecimal(_decimal(negative))))
        When("the explicit HOCON decoder interprets and canonically encodes those values")
        val result = XSemiStructuredData.fromText(text, StructuredDataFormat.Hocon)
          .flatMap(XSemiStructuredData.toJson)
        Then("all bare integers retain exact numeric meaning even beyond Double's finite range")
        result.get shouldBe Some(expected)
      }

      "retain quoted and triple-quoted numbers and numeric-containing concatenations as strings" in {
        Given("numeric string declarations, triple strings with three to five closing quotes, escaped quotes and unquoted string concatenations")
        val positive = "9223372036854775808"
        val negative = "-9223372036854775809"
        val text = "numeric=" + positive + "\nquoted=\"" + positive + "\"\ntriple=\"\"\"" +
          negative + "\n" + positive + "\"\"\"\ntripleFour=\"\"\"" + positive +
          "\"\"\"\"\ntripleFive=\"\"\"" + negative + "\"\"\"\"\"\nescaped=\"\\\"" + positive +
          "\"\nwords=prefix " + positive + " suffix\ntrailing=" + negative +
          " suffix\njoined=\"prefix \"" + positive + "\" suffix\""
        val expected = Json.obj("numeric" -> Json.fromBigDecimal(_decimal(positive)),
          "quoted" -> Json.fromString(positive),
          "triple" -> Json.fromString(negative + "\n" + positive),
          "tripleFour" -> Json.fromString(positive + "\""),
          "tripleFive" -> Json.fromString(negative + "\"\""),
          "escaped" -> Json.fromString("\"" + positive),
          "words" -> Json.fromString("prefix " + positive + " suffix"),
          "trailing" -> Json.fromString(negative + " suffix"),
          "joined" -> Json.fromString("prefix " + positive + " suffix"))
        When("the selected HOCON decoder interprets the lexical declarations")
        val result = XSemiStructuredData.fromText(text, StructuredDataFormat.Hocon)
          .flatMap(XSemiStructuredData.toJson)
        Then("declared strings keep their original text while the bare integer remains numeric")
        result.get shouldBe Some(expected)
      }

      "keep oversized numeric keys and dotted paths distinct while skipping numeric comments" in {
        Given("oversized unquoted and quoted numeric keys, dotted paths and both comment forms")
        val key = "9223372036854775808"
        val negative = "-9223372036854775809"
        val text = key + "=" + negative + " // " + key + "\n" + key +
          ".child=" + key + "\n\"" + key + ".literal\"=" + negative +
          "\n\"" + negative + "\"=" + key + "\n# " + negative + "\nafter=" + key
        val expected = Json.obj(key -> Json.obj("child" -> Json.fromBigDecimal(_decimal(key))),
          (key + ".literal") -> Json.fromBigDecimal(_decimal(negative)),
          negative -> Json.fromBigDecimal(_decimal(key)),
          "after" -> Json.fromBigDecimal(_decimal(key)))
        When("HOCON interprets its key paths and the explicit decoder converts the values")
        val result = XSemiStructuredData.fromText(text, StructuredDataFormat.Hocon)
          .flatMap(XSemiStructuredData.toJson)
        Then("keys and paths retain native HOCON meaning and comments do not affect values")
        result.get shouldBe Some(expected)
      }

      "preserve object concatenation and duplicate-key precedence with exact numeric values" in {
        Given("concatenated objects and repeated keys whose final values include numbers and strings")
        val positive = "9223372036854775808"
        val negative = "-9223372036854775809"
        val text = "combined={positive=" + positive + ", kept=1} {negative=" + negative +
          ", kept=2}\nrepeated=" + positive + "\nrepeated=" + negative +
          "\nreplaced=" + positive + "\nreplaced=\"" + positive +
          "\"\nquotedFirst=\"" + negative + "\"\nquotedFirst=" + positive
        val expected = Json.obj("combined" -> Json.obj(
          "positive" -> Json.fromBigDecimal(_decimal(positive)),
          "negative" -> Json.fromBigDecimal(_decimal(negative)), "kept" -> Json.fromInt(2)),
          "repeated" -> Json.fromBigDecimal(_decimal(negative)),
          "replaced" -> Json.fromString(positive),
          "quotedFirst" -> Json.fromBigDecimal(_decimal(positive)))
        When("the selected HOCON decoder interprets concatenation and precedence")
        val result = XSemiStructuredData.fromText(text, StructuredDataFormat.Hocon)
          .flatMap(XSemiStructuredData.toJson)
        Then("native merge and precedence semantics retain exact numbers and final string declarations")
        result.get shouldBe Some(expected)
      }
    }

    "retain nested objects separately from literal quoted dotted keys" in {
      Given("a literal HOCON dotted key and a distinct nested object with an absent attribute and empty structures")
      val text = "\"a.b\"=1\na { b=2, empty={}, absent=null, items=[[],{}] }"
      val expected = Json.obj("a.b" -> Json.fromInt(1), "a" -> Json.obj(
        "b" -> Json.fromInt(2), "empty" -> Json.obj(), "items" -> Json.arr(Json.arr(), Json.obj())))
      When("HOCON root entries are decoded directly and canonically encoded")
      val result = XSemiStructuredData.fromText(text, StructuredDataFormat.Hocon)
        .flatMap(XSemiStructuredData.toJson)
      Then("literal keys and retained nested members remain distinct while the null attribute is omitted")
      result.get shouldBe Some(expected)
    }

    "reject required, optional and locally referential substitutions without resolving them" in {
      Given("HOCON substitution expressions for environment, optional and local values")
      val inputs = Vector("value=${HOME}", "value=${?HOME}", "a=1\nb=${a}",
        "value=${?TEXTUS_ABSENT_VALUE}", "values=[${a}]")
      When("the explicit HOCON parser interprets those unresolved documents")
      val results = inputs.map(XSemiStructuredData.fromText(_, StructuredDataFormat.Hocon))
      Then("every substitution is invalid before omission or ambient resolution can occur")
      results.foreach(_expect_invalid)
    }

    "reject all include forms before any external resource can be read" in {
      Given("plain, file, URL and classpath include expressions, with required variants")
      val targets = Vector("\"textus-absent\"", "file(\"/textus-absent.conf\")",
        "url(\"https://example.invalid/textus.conf\")", "classpath(\"textus-absent.conf\")")
      val inputs = targets.flatMap(target => Vector("include " + target,
        "include required(" + target + ")"))
      When("the selected HOCON parser encounters those includes")
      val results = inputs.map(XSemiStructuredData.fromText(_, StructuredDataFormat.Hocon))
      Then("every include is a format failure even when the target is optional")
      results.foreach(_expect_format)
    }
  }

  "Unsupported external input (R11)" should {
    "reject null array positions at every depth in each declared syntax" in {
      Given("direct and nested null array elements in JSON, YAML and HOCON inputs")
      val inputs = Vector(
        StructuredDataFormat.Json -> "{\"items\":[null]}",
        StructuredDataFormat.Json -> "{\"items\":[[1,null]]}",
        StructuredDataFormat.Yaml -> "items: [null]",
        StructuredDataFormat.Yaml -> "items: [[1, null]]",
        StructuredDataFormat.Hocon -> "items=[null]",
        StructuredDataFormat.Hocon -> "items=[[1,null]]")
      When("each explicitly selected decoder interprets the invalid positions")
      val results = inputs.map { case (format, text) => XSemiStructuredData.fromText(text, format) }
      Then("all inputs fail as invalid values without filtering or shifting their elements")
      results.foreach(_expect_invalid)
    }

    "distinguish malformed or absent text from invalid format selection and JSON roots" in {
      Given("malformed text, absent text, a null selector and non-object JSON roots")
      val malformed = Vector(StructuredDataFormat.Json -> "{",
        StructuredDataFormat.Yaml -> "value: [", StructuredDataFormat.Hocon -> "value=[")
      val roots = Vector("[]", "true", "123", "null", "\"text\"")
      When("the declared converters interpret each input")
      val formatresults = malformed.map { case (format, text) =>
        XSemiStructuredData.fromText(text, format)
      } ++ StructuredDataFormat.values.map(XSemiStructuredData.fromText(null, _))
      val selection = XSemiStructuredData.fromText("{}", null)
      val rootresults = roots.map(XSemiStructuredData.fromText(_, StructuredDataFormat.Json))
      Then("syntax and absent text carry FormatError while selection and roots carry Invalid")
      formatresults.foreach(_expect_format)
      _expect_invalid(selection)
      rootresults.foreach(_expect_invalid)
    }

    "use only the selected format without a syntax fallback" in {
      Given("a YAML-only object supplied with the JSON selector")
      val text = "value: true"
      When("explicit JSON conversion interprets that text")
      val result = XSemiStructuredData.fromText(text, StructuredDataFormat.Json)
      Then("the input fails JSON syntax instead of being accepted as YAML")
      _expect_format(result)
    }

    "reject numbers beyond exact BigDecimal representation" in {
      Given("an exponent beyond BigDecimal's scale range in each declared syntax")
      val text = "{\"number\":1e2147483649}"
      When("each declared decoder interprets the unrepresentable number")
      val results = StructuredDataFormat.values.map(XSemiStructuredData.fromText(text, _))
      Then("each conversion fails rather than substituting a floating point value")
      results.foreach(_expect_invalid)
    }
  }

  private def _expect_invalid(result: Consequence[?]): Unit = {
    result match {
      case Consequence.Failure(conclusion) =>
        conclusion should be_invalid_failure_conclusion
        conclusion.observation.cause.getDataType shouldBe Some(XSemiStructuredData)
      case _ => fail("Expected an invalid semiStructuredData value")
    }
  }

  private def _expect_format(result: Consequence[?]): Unit = {
    result match {
      case Consequence.Failure(conclusion) =>
        conclusion should be_format_error_failure_conclusion
        conclusion.observation.cause.getDataType shouldBe Some(XSemiStructuredData)
      case _ => fail("Expected a semiStructuredData format failure")
    }
  }

  private def _decimal(text: String): BigDecimal = {
    new BigDecimal(new java.math.BigDecimal(text, MathContext.UNLIMITED), MathContext.UNLIMITED)
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
    Gen.listOf(Gen.oneOf("雪", "é", "🙂", "\u0000", "\n", "\"", "\\", "a", "中")).map(_.mkString)
  }

  private def _value_gen(depth: Int): Gen[Json] = {
    val scalar = Gen.oneOf(Gen.oneOf(true, false).map(Json.fromBoolean),
      _string_gen.map(Json.fromString), _decimal_gen.map(Json.fromBigDecimal))
    if (depth == 0) {
      scalar
    } else {
      val nested = _value_gen(depth - 1)
      Gen.oneOf(scalar,
        Gen.choose(0, 4).flatMap(size => Gen.listOfN(size, nested)).map(Json.fromValues),
        Gen.oneOf(Gen.const(Json.Null), nested).map(value =>
          Json.obj("child" -> value, "empty" -> Json.obj())))
    }
  }

  private val _object_gen: Gen[Json] = {
    for {
      decimal <- _decimal_gen
      string <- _string_gen
      payload <- Gen.oneOf(Gen.const(Json.Null), _value_gen(2))
      elements <- Gen.choose(0, 4).flatMap(size => Gen.listOfN(size, _value_gen(1)))
    } yield {
      Json.obj("number" -> Json.fromBigDecimal(decimal), "text" -> Json.fromString(string),
        "payload" -> payload, "array" -> Json.fromValues(elements), "nullMember" -> Json.Null,
        "emptyObject" -> Json.obj(), "a.b" -> Json.fromString(string))
    }
  }

  private def _expected_json(value: Json): Json = {
    value.arrayOrObject(value,
      values => Json.fromValues(values.map(_expected_json)),
      members => Json.fromFields(members.toVector.collect {
        case (key, child) if !child.isNull => key -> _expected_json(child)
      }))
  }
}
