package org.goldenport.record.io

import java.io.ByteArrayOutputStream
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import org.goldenport.Consequence
import org.goldenport.record.Record
import org.goldenport.record.io.RecordImportDecoder.*
import org.goldenport.schema.{Column, Schema, ValueDomain, XInt, XString}
import org.goldenport.value.BaseContent

/*
 * @since   May. 27, 2025
 *  version May. 24, 2026
 * @version Jul.  1, 2026
 * @author  ASAMI, Tomoharu
 */
class RecordDecoderSpec
    extends AnyWordSpec
    with GivenWhenThen
    with Matchers
    with ScalaCheckDrivenPropertyChecks {

  private val _decoder = RecordDecoder()
  private val _import_decoder = RecordImportDecoder()

  "RecordDecoder.json" should {
    "decode a top-level object into one Record" in {
      Given("a JSON object document")
      val json = """{"id":"p1","name":"taro"}"""

      When("decoding the JSON document")
      val result = _decoder.json(json)

      Then("one record is returned")
      result match {
        case Consequence.Success(record) =>
          record shouldEqual Record.create(Seq("id" -> "p1", "name" -> "taro"))
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }
  }

  "RecordDecoder.jsonRecords" should {
    "decode a top-level array of objects into Vector[Record]" in {
      Given("a JSON array document")
      val json = """[{"id":"p1"},{"id":"p2"}]"""

      When("decoding the JSON document")
      val result = _decoder.jsonRecords(json)

      Then("two records are returned")
      result match {
        case Consequence.Success(records) =>
          records shouldEqual Vector(
            Record.create(Seq("id" -> "p1")),
            Record.create(Seq("id" -> "p2"))
          )
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "fail for non-object array elements" in {
      Given("a JSON array with a scalar element")
      val json = """[{"id":"p1"},1]"""

      When("decoding the JSON document")
      val result = _decoder.jsonRecords(json)

      Then("the decode fails")
      result match {
        case _: Consequence.Failure[?] => succeed
        case _ => fail("expected failure for non-object array element")
      }
    }
  }

  "RecordDecoder.jsonAutoRecords" should {
    "wrap a top-level object into a single-element vector" in {
      Given("a JSON object document")
      val json = """{"id":"p1","name":"taro"}"""

      When("decoding the JSON document")
      val result = _decoder.jsonAutoRecords(json)

      Then("one record is returned in a vector")
      result match {
        case Consequence.Success(records) =>
          records shouldEqual Vector(Record.create(Seq("id" -> "p1", "name" -> "taro")))
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "decode a top-level array of objects into a vector" in {
      Given("a JSON array document")
      val json = """[{"id":"p1"},{"id":"p2"}]"""

      When("decoding the JSON document")
      val result = _decoder.jsonAutoRecords(json)

      Then("two records are returned")
      result match {
        case Consequence.Success(records) =>
          records shouldEqual Vector(
            Record.create(Seq("id" -> "p1")),
            Record.create(Seq("id" -> "p2"))
          )
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }
  }

  "RecordDecoder.yaml" should {
    "preserve existing single-record mapping behavior" in {
      Given("a YAML object document")
      val yaml =
        """id: p1
          |name: taro
          |""".stripMargin

      When("decoding the YAML document")
      val result = _decoder.yaml(yaml)

      Then("one record is returned")
      result match {
        case Consequence.Success(record) =>
          record shouldEqual Record.create(Seq("id" -> "p1", "name" -> "taro"))
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }
  }

  "RecordDecoder.yamlRecords" should {
    "decode a top-level list of mappings into Vector[Record]" in {
      Given("a YAML document with a top-level list of mappings")
      val yaml =
        """- id: p1
          |  name: taro
          |- id: p2
          |  name: hanako
          |""".stripMargin

      When("decoding the YAML document")
      val result = _decoder.yamlRecords(yaml)

      Then("two records are returned")
      result match {
        case Consequence.Success(records) =>
          records shouldEqual Vector(
            Record.create(Seq("id" -> "p1", "name" -> "taro")),
            Record.create(Seq("id" -> "p2", "name" -> "hanako"))
          )
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "fail for non-mapping list items" in {
      Given("a YAML list with a scalar item")
      val yaml =
        """- id: p1
          |- 1
          |""".stripMargin

      When("decoding the YAML document")
      val result = _decoder.yamlRecords(yaml)

      Then("the decode fails")
      result match {
        case _: Consequence.Failure[?] => succeed
        case _ => fail("expected failure for non-mapping YAML list item")
      }
    }
  }

  "RecordDecoder.yamlAutoRecords" should {
    "wrap a top-level mapping into a single-element vector" in {
      Given("a YAML object document")
      val yaml =
        """id: p1
          |name: taro
          |""".stripMargin

      When("decoding the YAML document")
      val result = _decoder.yamlAutoRecords(yaml)

      Then("one record is returned in a vector")
      result match {
        case Consequence.Success(records) =>
          records shouldEqual Vector(Record.create(Seq("id" -> "p1", "name" -> "taro")))
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "decode a top-level sequence of mappings into a vector" in {
      Given("a YAML sequence document")
      val yaml =
        """- id: p1
          |  name: taro
          |- id: p2
          |  name: hanako
          |""".stripMargin

      When("decoding the YAML document")
      val result = _decoder.yamlAutoRecords(yaml)

      Then("two records are returned")
      result match {
        case Consequence.Success(records) =>
          records shouldEqual Vector(
            Record.create(Seq("id" -> "p1", "name" -> "taro")),
            Record.create(Seq("id" -> "p2", "name" -> "hanako"))
          )
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }
  }

  "RecordDecoder.xml" should {
    "decode a simple element tree into one Record" in {
      Given("an XML document")
      val xml =
        """<person id="p1">
          |  <name>taro</name>
          |  <address><city>Tokyo</city></address>
          |  <tag>a</tag>
          |  <tag>b</tag>
          |</person>""".stripMargin

      When("decoding the XML document")
      val result = _decoder.xml(xml)

      Then("one record is returned")
      result match {
        case Consequence.Success(record) =>
          record shouldEqual Record.create(Seq(
            "@id" -> "p1",
            "name" -> "taro",
            "address" -> Record.create(Seq("city" -> "Tokyo")),
            "tag" -> Vector("a", "b")
          ))
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "decode attributes using @name" in {
      Given("an XML document with attributes")
      val xml = """<person id="p1" role="admin"><name>taro</name></person>"""

      When("decoding the XML document")
      val result = _decoder.xml(xml)

      Then("attributes are prefixed with @")
      result match {
        case Consequence.Success(record) =>
          record.asMap.keySet should contain allOf ("@id", "@role", "name")
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "decode repeated sibling elements into Vector" in {
      Given("an XML document with repeated siblings")
      val xml = """<person><tag>a</tag><tag>b</tag></person>"""

      When("decoding the XML document")
      val result = _decoder.xml(xml)

      Then("the repeated field becomes a vector")
      result match {
        case Consequence.Success(record) =>
          record.asMap("tag") shouldEqual Vector("a", "b")
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "fail for mixed content" in {
      Given("an XML document with mixed content")
      val xml = """<person>taro<name>ignored</name></person>"""

      When("decoding the XML document")
      val result = _decoder.xml(xml)

      Then("the decode fails")
      result match {
        case _: Consequence.Failure[?] => succeed
        case _ => fail("expected failure for mixed XML content")
      }
    }
  }

  "RecordDecoder.xmlRecords" should {
    "decode a list container into Vector[Record]" in {
      Given("an XML list container")
      val xml =
        """<list>
          |  <person><name>taro</name></person>
          |  <person><name>hanako</name></person>
          |</list>""".stripMargin

      When("decoding the XML document")
      val result = _decoder.xmlRecords(xml)

      Then("two records are returned")
      result match {
        case Consequence.Success(records) =>
          records shouldEqual Vector(
            Record.create(Seq("name" -> "taro")),
            Record.create(Seq("name" -> "hanako"))
          )
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "fail for heterogeneous direct child element names" in {
      Given("an XML list container with different child names")
      val xml =
        """<list>
          |  <person><name>taro</name></person>
          |  <entry><name>hanako</name></entry>
          |</list>""".stripMargin

      When("decoding the XML document")
      val result = _decoder.xmlRecords(xml)

      Then("the decode fails")
      result match {
        case _: Consequence.Failure[?] => succeed
        case _ => fail("expected failure for heterogeneous XML child names")
      }
    }

    "keep plural shape stable for any non-empty homogeneous list" in {
      val countGen = Gen.chooseNum(1, 4)

      forAll(countGen) { n =>
        Given(s"an XML list with $n homogeneous person elements")
        val items = (1 to n).map { i =>
          s"<person><name>p$i</name></person>"
        }.mkString("\n  ")
        val xml = s"<list>\n  $items\n</list>"

        When("decoding the XML document")
        val result = _decoder.xmlRecords(xml)

        Then("the number of records matches the number of direct child elements")
        result match {
          case Consequence.Success(records) =>
            records.length shouldEqual n
          case Consequence.Failure(err) =>
            fail(err.toString)
        }
      }
    }
  }

  "RecordDecoder.xmlAutoRecords" should {
    "wrap a single XML record into a one-element vector" in {
      Given("an XML document with one root record")
      val xml = """<person id="p1"><name>taro</name></person>"""

      When("decoding the XML document")
      val result = _decoder.xmlAutoRecords(xml)

      Then("one record is returned in a vector")
      result match {
        case Consequence.Success(records) =>
          records shouldEqual Vector(
            Record.create(Seq("@id" -> "p1", "name" -> "taro"))
          )
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "decode a plural container into a vector" in {
      Given("an XML list container")
      val xml =
        """<list>
          |  <person><name>taro</name></person>
          |  <person><name>hanako</name></person>
          |</list>""".stripMargin

      When("decoding the XML document")
      val result = _decoder.xmlAutoRecords(xml)

      Then("two records are returned")
      result match {
        case Consequence.Success(records) =>
          records shouldEqual Vector(
            Record.create(Seq("name" -> "taro")),
            Record.create(Seq("name" -> "hanako"))
          )
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }
  }

  "RecordDecoder.csvRecords" should {
    "decode a header row and two data rows into two records" in {
      Given("a CSV document with a header and two rows")
      val csv =
        """id,name,age
          |p1,taro,20
          |p2,hanako,30
          |""".stripMargin

      When("decoding the CSV document")
      val result = _decoder.csvRecords(csv)

      Then("two records are returned")
      result match {
        case Consequence.Success(records) =>
          records shouldEqual Vector(
            Record.create(Seq("id" -> "p1", "name" -> "taro", "age" -> "20")),
            Record.create(Seq("id" -> "p2", "name" -> "hanako", "age" -> "30"))
          )
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "return an empty vector for a header-only document" in {
      Given("a CSV document that contains only the header row")
      val csv = "id,name,age"

      When("decoding the CSV document")
      val result = _decoder.csvRecords(csv)

      Then("an empty vector is returned")
      result match {
        case Consequence.Success(records) =>
          records shouldEqual Vector.empty
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "fail explicitly for empty input" in {
      Given("an empty CSV document")
      val csv = ""

      When("decoding the CSV document")
      val result = _decoder.csvRecords(csv)

      Then("the decode fails")
      result match {
        case _: Consequence.Failure[?] => succeed
        case _ => fail("expected failure for empty CSV input")
      }
    }
  }

  "RecordDecoder.tslRecords" should {
    "decode two blank-line separated blocks into two records" in {
      Given("a TSL document with two blocks")
      val tsl =
        """id: p1
          |name: taro
          |age: 20
          |
          |id: p2
          |name: hanako
          |age: 30
          |""".stripMargin

      When("decoding the TSL document")
      val result = _decoder.tslRecords(tsl)

      Then("two records are returned")
      result match {
        case Consequence.Success(records) =>
          records shouldEqual Vector(
            Record.create(Seq("id" -> "p1", "name" -> "taro", "age" -> "20")),
            Record.create(Seq("id" -> "p2", "name" -> "hanako", "age" -> "30"))
          )
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "fail explicitly for a malformed line" in {
      Given("a TSL document with a malformed line")
      val tsl =
        """id: p1
          |name taro
          |""".stripMargin

      When("decoding the TSL document")
      val result = _decoder.tslRecords(tsl)

      Then("the decode fails")
      result match {
        case _: Consequence.Failure[?] => succeed
        case _ => fail("expected failure for malformed TSL input")
      }
    }
  }

  "RecordImportDecoder" should {
    "decode CSV with header metadata and header row" in {
      Given("a CSV document with metadata preamble")
      val csv =
        """# source: test-fixture
          |id,name
          |p1,taro
          |p2,hanako
          |""".stripMargin

      When("decoding the CSV document")
      val result = _import_decoder.decode(csv, RecordImportFormat.Csv)

      Then("records and metadata are returned")
      result match {
        case Consequence.Success(importresult) =>
          importresult.metadata.getString("source") shouldBe Some("test-fixture")
          importresult.records shouldEqual Vector(
            Record.create(Seq("id" -> "p1", "name" -> "taro")),
            Record.create(Seq("id" -> "p2", "name" -> "hanako"))
          )
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "decode TSV without header by schema column order" in {
      Given("a TSV document with schema but no header")
      val tsv =
        """p1	taro
          |p2	hanako
          |""".stripMargin
      val options = RecordImportOptions(schema = Some(_person_schema), headerMode = HeaderMode.Absent)

      When("decoding the TSV document")
      val result = _import_decoder.decode(tsv, RecordImportFormat.Tsv, options)

      Then("schema column order provides field names")
      result match {
        case Consequence.Success(importresult) =>
          importresult.records shouldEqual Vector(
            Record.create(Seq("id" -> "p1", "name" -> "taro")),
            Record.create(Seq("id" -> "p2", "name" -> "hanako"))
          )
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "reject CSV without header or schema" in {
      Given("a headerless CSV document")
      val csv = "p1,taro"

      When("decoding the CSV document")
      val result = _import_decoder.decode(csv, RecordImportFormat.Csv, RecordImportOptions(headerMode = HeaderMode.Absent))

      Then("the decode fails")
      result match {
        case _: Consequence.Failure[?] => succeed
        case _ => fail("expected failure for headerless CSV without schema")
      }
    }

    "decode line-delimited input using configured field name" in {
      Given("line-delimited values with comments")
      val lines =
        """# sample ISBNs
          |9780134685991
          |9784774184111
          |""".stripMargin

      When("decoding lines")
      val result = _import_decoder.decode(lines, RecordImportFormat.Lines, RecordImportOptions(lineFieldName = Some("isbn")))

      Then("each value becomes one record")
      result match {
        case Consequence.Success(importresult) =>
          importresult.records shouldEqual Vector(
            Record.create(Seq("isbn" -> "9780134685991")),
            Record.create(Seq("isbn" -> "9784774184111"))
          )
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "decode LTSV rows into records" in {
      Given("LTSV values with metadata")
      val ltsv =
        """# source: ltsv-fixture
          |id:p1	name:taro
          |id:p2	name:hanako
          |""".stripMargin

      When("decoding LTSV")
      val result = _import_decoder.decode(ltsv, RecordImportFormat.Ltsv)

      Then("each row becomes one record")
      result match {
        case Consequence.Success(importresult) =>
          importresult.metadata.getString("source") shouldBe Some("ltsv-fixture")
          importresult.records shouldEqual Vector(
            Record.create(Seq("id" -> "p1", "name" -> "taro")),
            Record.create(Seq("id" -> "p2", "name" -> "hanako"))
          )
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "decode self-describing formats without schema" in {
      Given("JSON, YAML, XML, and HOCON documents")
      val json = """[{"id":"p1","name":"taro"}]"""
      val yaml =
        """- id: p1
          |  name: taro
          |""".stripMargin
      val xml = """<list><person><id>p1</id><name>taro</name></person></list>"""
      val hocon = """id = p1
                    |name = taro""".stripMargin

      Then("all formats decode into records without schema")
      _import_decoder.decode(json, RecordImportFormat.Json).toOption.map(_.records.head.getString("name")) shouldBe Some(Some("taro"))
      _import_decoder.decode(yaml, RecordImportFormat.Yaml).toOption.map(_.records.head.getString("name")) shouldBe Some(Some("taro"))
      _import_decoder.decode(xml, RecordImportFormat.Xml).toOption.map(_.records.head.getString("name")) shouldBe Some(Some("taro"))
      _import_decoder.decode(hocon, RecordImportFormat.Hocon).toOption.map(_.records.head.getString("name")) shouldBe Some(Some("taro"))
    }

    "decode HOCON container records without schema" in {
      Given("a HOCON document with records container")
      val hocon =
        """records = [
          |  { id = p1, name = taro }
          |  { id = p2, name = hanako }
          |]
          |""".stripMargin

      When("decoding the HOCON document")
      val result = _import_decoder.decode(hocon, RecordImportFormat.Hocon)

      Then("container entries become records")
      result match {
        case Consequence.Success(importresult) =>
          importresult.records.map(_.getString("id")) shouldEqual Vector(Some("p1"), Some("p2"))
          importresult.records.map(_.getString("name")) shouldEqual Vector(Some("taro"), Some("hanako"))
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "auto-detect JSON XML CSV TSV LTSV YAML HOCON and lines" in {
      Given("documents with recognizable import shapes")
      val values = Vector(
        """[{"id":"p1"}]""" -> RecordImportFormat.Json,
        """<list><person><id>p1</id></person></list>""" -> RecordImportFormat.Xml,
        "id,name\np1,taro" -> RecordImportFormat.Csv,
        "id\tname\np1\ttaro" -> RecordImportFormat.Tsv,
        "id:p1\tname:taro" -> RecordImportFormat.Ltsv,
        "- id: p1" -> RecordImportFormat.Yaml,
        "id = p1" -> RecordImportFormat.Hocon,
        "p1" -> RecordImportFormat.Lines
      )

      Then("the detected format is stable")
      values.foreach { case (text, expected) =>
        _import_decoder.decodeAuto(text).toOption.map(_.detectedFormat) shouldBe Some(expected)
      }
    }

    "apply schema field resolution and type coercion as an optional shaping step" in {
      Given("a CSV document with aliases and numeric strings")
      val csv =
        """Person ID:id,Name:name,Age:age:int,extra
          |p1,taro,20,ignored
          |""".stripMargin
      val options = RecordImportOptions(
        schema = Some(_person_schema),
        coerceBySchema = true,
        unknownFieldPolicy = UnknownFieldPolicy.Issue
      )

      When("decoding with schema shaping")
      val result = _import_decoder.decode(csv, RecordImportFormat.Csv, options)

      Then("field names are normalized and typed values are coerced")
      result match {
        case Consequence.Success(importresult) =>
          importresult.records.head.getAny("age") shouldBe Some(20)
          importresult.records.head.getString("name") shouldBe Some("taro")
          importresult.issues.exists(_.code == "unknown-field") shouldBe true
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "decode Excel with a header row" in {
      Given("an Excel workbook with a header row")
      val bytes = _excel_bytes(Vector(
        Vector("id", "name"),
        Vector("p1", "taro"),
        Vector("p2", "hanako")
      ))

      When("decoding Excel bytes")
      val result = _import_decoder.decodeBytes(bytes, RecordImportFormat.Excel)

      Then("worksheet rows become records")
      result match {
        case Consequence.Success(importresult) =>
          importresult.metadata.getString("sheetName") shouldBe Some("records")
          importresult.records shouldEqual Vector(
            Record.create(Seq("id" -> "p1", "name" -> "taro")),
            Record.create(Seq("id" -> "p2", "name" -> "hanako"))
          )
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "decode headerless Excel by schema column order" in {
      Given("an Excel workbook without a header row")
      val bytes = _excel_bytes(Vector(
        Vector("p1", "taro"),
        Vector("p2", "hanako")
      ))
      val options = RecordImportOptions(schema = Some(_person_schema), headerMode = HeaderMode.Absent)

      When("decoding Excel bytes")
      val result = _import_decoder.decodeBytes(bytes, RecordImportFormat.Excel, options)

      Then("schema column order provides field names")
      result match {
        case Consequence.Success(importresult) =>
          importresult.records.map(_.getString("name")) shouldEqual Vector(Some("taro"), Some("hanako"))
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "decode Excel by explicit sheet name" in {
      Given("an Excel workbook with a named sheet")
      val bytes = _excel_bytes(Vector(
        Vector("id", "name"),
        Vector("p1", "taro")
      ), "people")
      val options = RecordImportOptions(sheetName = Some("people"))

      When("decoding Excel bytes")
      val result = _import_decoder.decodeBytes(bytes, RecordImportFormat.Excel, options)

      Then("the selected worksheet is used")
      result match {
        case Consequence.Success(importresult) =>
          importresult.metadata.getString("sheetName") shouldBe Some("people")
          importresult.records.head.getString("name") shouldBe Some("taro")
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "reject Excel import when explicit sheet name is missing" in {
      Given("an Excel workbook without the requested sheet")
      val bytes = _excel_bytes(Vector(
        Vector("id", "name"),
        Vector("p1", "taro")
      ), "people")
      val options = RecordImportOptions(sheetName = Some("missing"))

      When("decoding Excel bytes")
      val result = _import_decoder.decodeBytes(bytes, RecordImportFormat.Excel, options)

      Then("the import fails instead of falling back to the first sheet")
      result match {
        case _: Consequence.Failure[?] => succeed
        case Consequence.Success(importresult) =>
          fail(s"unexpected import success: ${importresult}")
      }
    }
  }

  "RecordExportEncoder" should {
    "export Java properties with properties extension" in {
      Given("a flat configuration record")
      val encoder = RecordExportEncoder()
      val records = Vector(Record.data("textus.web.descriptor" -> "config/web.yaml"))

      When("exporting as Java properties")
      val result = encoder.encode(records, org.goldenport.record.RecordFormat.Properties)

      Then("the result uses the Java properties suffix and content type")
      result.toOption.map(_.extension) shouldBe Some("properties")
      result.toOption.map(_.contentType) shouldBe Some("text/x-java-properties")
      result.toOption.flatMap(_.text).exists(_.contains("textus.web.descriptor=config/web.yaml")) shouldBe true
    }

    "choose HOCON extension by export purpose" in {
      Given("a HOCON record export")
      val encoder = RecordExportEncoder()
      val records = Vector(Record.data("name" -> "job"))

      When("exporting with default configuration purpose")
      val conf = encoder.encode(records, org.goldenport.record.RecordFormat.Hocon)
      val hocon = encoder.encode(
        records,
        org.goldenport.record.RecordFormat.Hocon,
        RecordExportEncoder.RecordExportOptions(
          extensionPurpose = RecordExportEncoder.RecordExportExtensionPurpose.Definition
        )
      )

      Then("configuration keeps .conf while definition files can use .hocon")
      conf.toOption.map(_.extension) shouldBe Some("conf")
      hocon.toOption.map(_.extension) shouldBe Some("hocon")
    }

    "export records to Excel bytes" in {
      Given("records to export")
      val encoder = RecordExportEncoder()
      val records = Vector(
        Record.create(Seq("id" -> "p1", "name" -> "taro")),
        Record.create(Seq("id" -> "p2", "name" -> "hanako"))
      )

      When("encoding as Excel")
      val result = encoder.encodeBytes(records, org.goldenport.record.RecordFormat.Excel)

      Then("the workbook can be read back")
      result match {
        case Consequence.Success(exportresult) =>
          exportresult.extension shouldBe "xlsx"
          val decoded = _import_decoder.decodeBytes(exportresult.bytes, RecordImportFormat.Excel)
          decoded.toOption.map(_.records.map(_.getString("name"))) shouldBe Some(Vector(Some("taro"), Some("hanako")))
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }
  }

  private def _excel_bytes(
    rows: Vector[Vector[String]],
    sheetname: String = "records"
  ): Array[Byte] = {
    val workbook = XSSFWorkbook()
    try {
      val sheet = workbook.createSheet(sheetname)
      rows.zipWithIndex.foreach { case (values, rowindex) =>
        val row = sheet.createRow(rowindex)
        values.zipWithIndex.foreach { case (value, colindex) =>
          row.createCell(colindex).setCellValue(value)
        }
      }
      val out = ByteArrayOutputStream()
      workbook.write(out)
      out.toByteArray
    } finally {
      workbook.close()
    }
  }

  private def _person_schema: Schema =
    Schema(Vector(
      Column(BaseContent.Builder("id").label("Person ID").build(), ValueDomain(XString)),
      Column(BaseContent.Builder("name").label("Name").build(), ValueDomain(XString)),
      Column(BaseContent.Builder("age").label("Age").build(), ValueDomain(XInt))
    ))
}
