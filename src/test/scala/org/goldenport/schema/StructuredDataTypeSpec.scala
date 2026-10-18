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
 * Executable specification for canonical JSON and semi-structured datatypes.
 *
 * @since   Oct.  5, 2026
 * @version Oct. 18, 2026
 * @author  ASAMI, Tomoharu
 */
final class StructuredDataTypeSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks
  with ConclusionMatchers {
  "Canonical structured datatypes (R1)" should {
    "expose distinct stable identities and normalized runtime types" in {
      Given("the canonical JSON and semi-structured datatype identities")
      val json = Json.obj("number" -> Json.fromInt(7))
      val jsontype: CanonicalDataType[Json] = XJson
      val recordtype: CanonicalDataType[Record] = XSemiStructuredData
      When("their names and normalized values are interpreted")
      val names = Vector(jsontype.name, recordtype.name)
      val jsonresult = jsontype.normalizer.normalize(json)
      val recordresult = recordtype.normalizer.normalize(json)
      Then("the types retain separate identities, public names and runtime representations")
      names shouldBe Vector("json", "semiStructuredData")
      XJson.print shouldBe "json"
      XSemiStructuredData.print shouldBe "semiStructuredData"
      (XJson: DataType) should not equal (XSemiStructuredData: DataType)
      jsonresult.get.get shouldBe a[Json]
      recordresult.get.get shouldBe a[Record]
      recordresult.get.get.fields.head.value.single shouldBe BigDecimal(7)
    }
  }

  "XJson (R2)" should {
    "retain every JSON shape unchanged" in {
      Given("object, array, string, boolean, number and null JSON values")
      val values = Vector(Json.obj(), Json.arr(Json.Null), Json.fromString("日本語"),
        Json.True, Json.fromInt(1), Json.Null)
      When("the canonical JSON normalizer receives those values")
      val results = values.map(XJson.normalizer.normalize)
      Then("each value remains the same JSON instance")
      results.zip(values).foreach { case (result, value) =>
        result.get.get should be theSameInstanceAs value
      }
    }

    "explicitly parse text containing any JSON shape" in {
      Given("declared JSON text inputs of every shape")
      val inputs = Vector("{}", "[]", "\"text\"", "true", "123.456", "null")
      val expected = Vector(Json.obj(), Json.arr(), Json.fromString("text"),
        Json.True, Json.fromBigDecimal(BigDecimal("123.456")), Json.Null)
      When("fromJsonText explicitly parses each input")
      val results = inputs.map(XJson.fromJsonText)
      Then("each input has its declared JSON meaning")
      results.map(_.get) shouldBe expected.map(Some.apply)
    }

    "report malformed text as a JSON format failure" in {
      Given("malformed and absent JSON text")
      val inputs = Vector("{", "", "[1,]", "unquoted", null)
      When("the explicit text conversion interprets each input")
      val results = inputs.map(XJson.fromJsonText)
      Then("each failure carries FormatError and the json datatype")
      results.foreach { result =>
        result match {
          case Consequence.Failure(conclusion) =>
            conclusion should be_format_error_failure_conclusion
            conclusion.observation.cause.getDataType shouldBe Some(XJson)
          case _ => fail("Malformed JSON text must produce a format failure")
        }
      }
    }

    "reject undeclared runtime conversions" in {
      Given("JSON-looking text, a Record and an arbitrary application value")
      val values = Vector[Any]("{\"a\":1}", Record.empty, new Object(), 1, null)
      When("the JSON normalizer receives those runtime values")
      val results = values.map(XJson.normalizer.normalize)
      Then("each value is invalid for json and no text is parsed implicitly")
      results.foreach(_expect_invalid(_, XJson))
    }
  }

  "XSemiStructuredData (R3-R7)" should {
    "preserve empty root objects" in {
      Given("an empty JSON object and an empty Record")
      val json = Json.obj()
      val record = Record.empty
      When("both explicit directions and Record normalization interpret them")
      val decoded = XSemiStructuredData.fromJson(json)
      val encoded = XSemiStructuredData.toJson(record)
      val normalized = XSemiStructuredData.normalizer.normalize(record)
      Then("the empty object remains a successful empty Record and JSON object")
      decoded.get shouldBe Some(record)
      encoded.get shouldBe Some(json)
      normalized.get shouldBe Some(record)
    }

    "omit null attributes recursively while preserving nested empty objects and arrays" in {
      Given("a JSON object with empty and populated nested values in a declared key order")
      val json = Json.obj(
        "emptyObject" -> Json.obj(),
        "emptyArray" -> Json.arr(),
        "nullMember" -> Json.Null,
        "nested" -> Json.obj("absent" -> Json.Null,
          "items" -> Json.arr(Json.fromString("雪"), Json.True)))
      val expected = Record.createFull(Vector(
        "emptyObject" -> Record.empty,
        "emptyArray" -> Vector.empty,
        "nested" -> Record.createFull(Vector("items" -> Vector("雪", true)))))
      val expectedjson = _expected_json(json)
      When("canonical conversion decodes and re-encodes the object")
      val decoded = XSemiStructuredData.normalizer.normalize(json)
      val encoded = decoded.flatMap(XSemiStructuredData.toJson)
      Then("retained members and array order survive while null attributes disappear")
      decoded.get shouldBe Some(expected)
      decoded.get.get.fields.map(_.key) shouldBe expectedjson.asObject.get.keys.toVector
      encoded.get shouldBe Some(expectedjson)
      encoded.get.get.asObject.get.keys.toVector shouldBe expectedjson.asObject.get.keys.toVector
    }

    "reject JSON roots that are not objects and implicit text decoding" in {
      Given("array, scalar, JSON null, text and unsupported runtime roots")
      val jsonvalues = Vector(Json.arr(), Json.fromString("text"), Json.True,
        Json.fromInt(1), Json.Null)
      val values = jsonvalues ++ Vector[Any]("{}", new Object(), null)
      When("explicit JSON conversion and normalization interpret those roots")
      val decoded = jsonvalues.map(XSemiStructuredData.fromJson)
      val normalized = values.map(XSemiStructuredData.normalizer.normalize)
      Then("every wrong shape or runtime value has the semiStructuredData invalid failure")
      (decoded ++ normalized).foreach(_expect_invalid(_, XSemiStructuredData))
    }

    "retain numeric meaning beyond Long and Double precision" in {
      Given("large integral and fractional decimal JSON numbers")
      val integer = BigInt("12345678901234567890123456789012345678901234567890")
      val decimal = new BigDecimal(new java.math.BigDecimal(
        "123456789012345678901234567890.12345678901234567890123456789"), MathContext.UNLIMITED)
      val json = Json.obj("numbers" -> Json.arr(Json.fromBigInt(integer), Json.fromBigDecimal(decimal)))
      When("the object roundtrips through the canonical Record representation")
      val decoded = XSemiStructuredData.fromJson(json)
      val encoded = decoded.flatMap(XSemiStructuredData.toJson)
      Then("both numbers are exact BigDecimals and the JSON decimal meaning is unchanged")
      val numbers = decoded.get.get.fields.head.value.single.asInstanceOf[Vector[Any]]
      numbers.foreach(_ shouldBe a[BigDecimal])
      numbers.head.asInstanceOf[BigDecimal].bigDecimal.toBigIntegerExact shouldBe integer.bigInteger
      numbers(1) shouldBe decimal
      encoded.get shouldBe Some(json)
    }

    "fail when a JSON number cannot be represented as an exact BigDecimal" in {
      Given("a valid Circe JSON number with an exponent beyond BigDecimal's scale range")
      val number = io.circe.JsonNumber.fromDecimalStringUnsafe("1e2147483649")
      val json = Json.obj("number" -> Json.fromJsonNumber(number))
      When("canonical object conversion directly interprets the supplied JSON object")
      val result = XSemiStructuredData.fromJson(json)
      Then("the conversion fails without substituting a Double")
      _expect_invalid(result, XSemiStructuredData)
    }

    "normalize supported Scala and Java numeric values directly and exactly" in {
      Given("supported numeric families and finite floating point decimal spellings")
      val numbers = Vector[Any](1.toByte, 2.toShort, 3, 4L, BigInt("9007199254740993"),
        BigDecimal("5.125"), new java.math.BigInteger("9223372036854775808"),
        new java.math.BigDecimal("6.000000000000000000000000000000000000001"),
        java.lang.Integer.valueOf(7), java.lang.Long.valueOf(8),
        java.lang.Byte.valueOf(9.toByte), java.lang.Short.valueOf(10.toShort),
        0.1f, 0.1d, Float.MinPositiveValue, Double.MinPositiveValue, Float.MaxValue, Double.MaxValue)
      val record = Record.createFull(Vector("numbers" -> numbers))
      val expected = numbers.map(number => new java.math.BigDecimal(number.toString))
      When("the canonical normalizer validates and normalizes the Record")
      val result = XSemiStructuredData.normalizer.normalize(record)
      Then("all numeric values become BigDecimals with their exact declared decimal spelling")
      val normalized = result.get.get.fields.head.value.single.asInstanceOf[Vector[Any]]
      normalized.foreach(_ shouldBe a[BigDecimal])
      normalized.map(_.asInstanceOf[BigDecimal].bigDecimal) shouldBe expected
    }

    "preserve native primitive and reference arrays and sequences" in {
      Given("a Record with ordered primitive arrays, reference arrays, lists and empty arrays")
      val record = Record.createFull(Vector(
        "ints" -> Array(2, 1), "refs" -> Array[Any]("first", Record.empty),
        "list" -> List(true, false), "empty" -> Array.empty[String]))
      val expected = Json.obj(
        "ints" -> Json.arr(Json.fromInt(2), Json.fromInt(1)),
        "refs" -> Json.arr(Json.fromString("first"), Json.obj()),
        "list" -> Json.arr(Json.True, Json.False), "empty" -> Json.arr())
      When("explicit encoding and canonical normalization interpret the Record")
      val encoded = XSemiStructuredData.toJson(record)
      val normalized = XSemiStructuredData.normalizer.normalize(record)
      Then("encoding retains all elements and normalization makes every sequence a Vector")
      encoded.get shouldBe Some(expected)
      normalized.get.get.fields.foreach(_.value.single shouldBe a[Vector[?]])
      normalized.get.get.fields.head.value.single shouldBe Vector(BigDecimal(2), BigDecimal(1))
    }

    "reject unknown Record members and nonfinite numbers" in {
      Given("Records containing unsupported objects and nonfinite Float and Double values")
      val values = Vector[Any](new Object(), Map("key" -> 1), Json.obj(),
        Float.NaN, Float.PositiveInfinity, Float.NegativeInfinity,
        Double.NaN, Double.PositiveInfinity, Double.NegativeInfinity)
      val records = values.map(value => Record.createFull(Vector("value" -> value)))
      When("explicit encoding and normalization validate each Record")
      val encoded = records.map(XSemiStructuredData.toJson)
      val normalized = records.map(XSemiStructuredData.normalizer.normalize)
      Then("unsupported and nonfinite members fail instead of being omitted or stringified")
      (encoded ++ normalized).foreach(_expect_invalid(_, XSemiStructuredData))
    }

    "reject null sequence elements without removing positions at any depth" in {
      Given("Record and JSON objects containing direct, nested and native-array null elements")
      val records = Vector(
        Record.data("items" -> Vector[Any](null)),
        Record.data("items" -> Vector(Vector[Any](1, null))),
        Record.data("items" -> Array[Any](Record.empty, null)))
      val jsons = Vector(Json.obj("items" -> Json.arr(Json.Null)),
        Json.obj("items" -> Json.arr(Json.arr(Json.fromInt(1), Json.Null))))
      When("canonical encoding, semantic normalization and JSON decoding interpret them")
      val results = records.flatMap(record => Vector(
        XSemiStructuredData.toJson(record), XSemiStructuredData.normalizer.normalize(record))) ++
        jsons.map(XSemiStructuredData.fromJson)
      Then("every conversion rejects the invalid element instead of shortening its sequence")
      results.foreach(_expect_invalid(_, XSemiStructuredData))
    }

    "reject duplicate keys at every object level" in {
      Given("a duplicate-key Record and a nested duplicate-key Record")
      val duplicate = Record.createFull(Vector("key" -> 1, "key" -> 2))
      val records = Vector(duplicate, Record.createFull(Vector("nested" -> duplicate)))
      When("encoding and normalization interpret both Records")
      val encoded = records.map(XSemiStructuredData.toJson)
      val normalized = records.map(XSemiStructuredData.normalizer.normalize)
      Then("each duplicate is an invalid semiStructuredData value")
      (encoded ++ normalized).foreach(_expect_invalid(_, XSemiStructuredData))
    }

    "aggregate independent nested member and element failures" in {
      Given("two unsupported array elements and an independent unsupported sibling member")
      val record = Record.createFull(Vector(
        "array" -> Vector(new Object(), new Object()), "sibling" -> new Object()))
      When("explicit encoding validates the complete Record")
      val result = XSemiStructuredData.toJson(record)
      Then("the failure retains every independent invalid member conclusion")
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

  "Canonical object roundtrips (R8)" should {
    "preserve generated normalized decimals, Unicode, nested objects and ordered arrays" in {
      forAll(_object_gen) { json =>
        Given("a generated object with nullable attributes and non-null array elements")
        val expected = _expected_json(json)
        When("each object is decoded, normalized and encoded canonically")
        val decoded = XSemiStructuredData.fromJson(json)
        val encoded = decoded.flatMap(XSemiStructuredData.toJson)
        val normalized = decoded.flatMap(XSemiStructuredData.normalizer.normalize)
        Then("independently normalized JSON meaning, retained key order and canonical values are stable")
        decoded.isSuccess shouldBe true
        encoded.get shouldBe Some(expected)
        encoded.get.get.asObject.get.keys.toVector shouldBe expected.asObject.get.keys.toVector
        normalized.get shouldBe decoded.get
        decoded.get.get.fields.map(_.key) should not contain ("nullMember")
      }
    }
  }

  "Existing scalar identities (R9)" should {
    "retain their names and integer normalization semantics" in {
      Given("the existing scalar datatypes and canonical integer normalizer")
      val types = Vector(XString, XBoolean, XInt, XLong, XFloat, XDouble, XDecimal)
      When("their names and supported integer inputs are interpreted")
      val names = types.map(_.name)
      val results = Vector[Any]("123", 123, 123L, 123.toShort, BigInt(123))
        .map(XInteger.normalizer.normalize)
      Then("scalar names and integer interpretation retain their established meaning")
      names shouldBe Vector("string", "boolean", "int", "long", "float", "double", "decimal")
      results.map(_.get) shouldBe Vector.fill(5)(Some(BigInt(123)))
    }
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
    val scalar = Gen.oneOf(
      Gen.oneOf(true, false).map(Json.fromBoolean),
      _string_gen.map(Json.fromString), _decimal_gen.map(Json.fromBigDecimal))
    if (depth == 0) {
      scalar
    } else {
      val nested = _value_gen(depth - 1)
      Gen.oneOf(scalar,
        Gen.choose(0, 5).flatMap(size => Gen.listOfN(size, nested)).map(Json.fromValues),
        Gen.oneOf(Gen.const(Json.Null), nested).map(value =>
          Json.obj("child" -> value, "empty" -> Json.obj())))
    }
  }

  private val _object_gen: Gen[Json] = {
    for {
      decimal <- _decimal_gen
      string <- _string_gen
      payload <- Gen.oneOf(Gen.const(Json.Null), _value_gen(2))
      elements <- Gen.choose(0, 5).flatMap(size => Gen.listOfN(size, _value_gen(1)))
    } yield {
      Json.obj("number" -> Json.fromBigDecimal(decimal), "text" -> Json.fromString(string),
        "payload" -> payload, "array" -> Json.fromValues(elements),
        "nullMember" -> Json.Null, "emptyObject" -> Json.obj())
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
