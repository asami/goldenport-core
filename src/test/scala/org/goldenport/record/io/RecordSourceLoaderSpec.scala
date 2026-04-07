package org.goldenport.record.io

import java.nio.file.Files
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import org.goldenport.Consequence
import org.goldenport.record.{Record, RecordDecoder, RecordFormat}

/*
 * @since   Apr.  8, 2026
 * @version Apr.  8, 2026
 * @author  ASAMI, Tomoharu
 */
class RecordSourceLoaderSpec
    extends AnyWordSpec
    with GivenWhenThen
    with Matchers {

  "RecordSourceLoader" should {
    "load JSON into Record" in {
      Given("a JSON document")
      val json = """{"name":"alice","age":"20"}"""

      When("loading it explicitly as JSON")
      val result = RecordSourceLoader.load(json, RecordFormat.Json)

      Then("a record is returned")
      result match
        case Consequence.Success(record) =>
          record.getString("name") shouldBe Some("alice")
          record.getDecimal("age") shouldBe Some(BigDecimal(20))
        case Consequence.Failure(err) =>
          fail(err.toString)
    }

    "load YAML into Record" in {
      val yaml =
        """name: alice
          |age: 20
          |""".stripMargin

      val result = RecordSourceLoader.load(yaml, RecordFormat.Yaml)

      result match
        case Consequence.Success(record) =>
          record shouldEqual Record.create(Seq("name" -> "alice", "age" -> BigDecimal(20)))
        case Consequence.Failure(err) =>
          fail(err.toString)
    }

    "load XML into Record" in {
      val xml = """<root><name>alice</name><age>20</age></root>"""

      val result = RecordSourceLoader.load(xml, RecordFormat.Xml)

      result match
        case Consequence.Success(record) =>
          record.getString("name") shouldBe Some("alice")
          record.getString("age") shouldBe Some("20")
        case Consequence.Failure(err) =>
          fail(err.toString)
    }

    "load HOCON into Record" in {
      val conf =
        """name = "alice"
          |age = 20
          |nested.city = "Tokyo"
          |""".stripMargin

      val result = RecordSourceLoader.load(conf, RecordFormat.Hocon)

      result match
        case Consequence.Success(record) =>
          record.getString("name") shouldBe Some("alice")
          record.getDecimal("age") shouldBe Some(BigDecimal(20))
          record.getRecord("nested").flatMap(_.getString("city")).orElse(record.getString("nested.city")) shouldBe Some("Tokyo")
        case Consequence.Failure(err) =>
          fail(err.toString)
    }

    "decode a typed object through RecordDecoder" in {
      case class Person(name: String, age: Int)

      given RecordDecoder[Person] with
        def fromRecord(r: Record): Consequence[Person] =
          for
            name <- Consequence.successOrRecordNotFound[String]("name", r)
            age <- Consequence.successOrRecordNotFound[Int]("age", r)
          yield Person(name, age)

      val json = """{"name":"alice","age":"20"}"""

      val result = RecordSourceLoader.decode[Person](json, RecordFormat.Json)

      result match
        case Consequence.Success(person) =>
          person shouldBe Person("alice", 20)
        case Consequence.Failure(err) =>
          fail(err.toString)
    }

    "infer format from path suffix" in {
      val path = Files.createTempFile("record-source-loader", ".yaml")
      Files.writeString(path, "name: alice\n")

      val result = RecordSourceLoader.load(path)

      result match
        case Consequence.Success(record) =>
          record.getString("name") shouldBe Some("alice")
        case Consequence.Failure(err) =>
          fail(err.toString)
    }
  }
}
