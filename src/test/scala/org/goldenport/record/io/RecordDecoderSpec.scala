package org.goldenport.record.io

import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import org.goldenport.Consequence
import org.goldenport.record.Record

/*
 * @since   May. 27, 2025
 * @version Mar. 27, 2026
 * @author  ASAMI, Tomoharu
 */
class RecordDecoderSpec
    extends AnyWordSpec
    with GivenWhenThen
    with Matchers
    with ScalaCheckDrivenPropertyChecks {

  private val decoder = RecordDecoder()

  "RecordDecoder.json" should {
    "decode a top-level object into one Record" in {
      Given("a JSON object document")
      val json = """{"id":"p1","name":"taro"}"""

      When("decoding the JSON document")
      val result = decoder.json(json)

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
      val result = decoder.jsonRecords(json)

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
      val result = decoder.jsonRecords(json)

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
      val result = decoder.jsonAutoRecords(json)

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
      val result = decoder.jsonAutoRecords(json)

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
      val result = decoder.yaml(yaml)

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
      val result = decoder.yamlRecords(yaml)

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
      val result = decoder.yamlRecords(yaml)

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
      val result = decoder.yamlAutoRecords(yaml)

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
      val result = decoder.yamlAutoRecords(yaml)

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
      val result = decoder.xml(xml)

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
      val result = decoder.xml(xml)

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
      val result = decoder.xml(xml)

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
      val result = decoder.xml(xml)

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
      val result = decoder.xmlRecords(xml)

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
      val result = decoder.xmlRecords(xml)

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
        val result = decoder.xmlRecords(xml)

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
      val result = decoder.xmlAutoRecords(xml)

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
      val result = decoder.xmlAutoRecords(xml)

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
      val result = decoder.csvRecords(csv)

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
      val result = decoder.csvRecords(csv)

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
      val result = decoder.csvRecords(csv)

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
      val result = decoder.tslRecords(tsl)

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
      val result = decoder.tslRecords(tsl)

      Then("the decode fails")
      result match {
        case _: Consequence.Failure[?] => succeed
        case _ => fail("expected failure for malformed TSL input")
      }
    }
  }
}
