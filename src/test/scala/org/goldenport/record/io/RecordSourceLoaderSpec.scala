package org.goldenport.record.io

import java.nio.file.Files
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import org.goldenport.Consequence
import org.goldenport.record.{Record, RecordDecoder, RecordFormat}

/*
 * @since   Apr.  8, 2026
 * @version Jul. 21, 2026
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

    "normalize nested YAML mappings and list entries as Records" in {
      Given("a YAML document containing a nested mapping and a list of mappings")
      val yaml =
        """source:
          |  kind: codex
          |servers:
          |  - name: research
          |""".stripMargin

      When("loading it through the generic Record source boundary")
      val result = RecordSourceLoader.load(yaml, RecordFormat.Yaml)

      Then("all mapping levels use the common Record representation")
      result match
        case Consequence.Success(record) =>
          record.getRecord("source").flatMap(_.getString("kind")) shouldBe Some("codex")
          record.getVector("servers").flatMap(_.headOption) shouldBe Some(
            Record.create(Seq("name" -> "research"))
          )
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

    "load Java properties into Record" in {
      val properties =
        """name=alice
          |age=20
          |nested.city=Tokyo
          |""".stripMargin

      val result = RecordSourceLoader.load(properties, RecordFormat.Properties)

      result match
        case Consequence.Success(record) =>
          record.getString("name") shouldBe Some("alice")
          record.getString("age") shouldBe Some("20")
          record.getString("nested.city") shouldBe Some("Tokyo")
        case Consequence.Failure(err) =>
          fail(err.toString)
    }

    "infer Java properties format from props and properties suffixes" in {
      RecordFormat.fromSuffix("config.props") shouldBe Some(RecordFormat.Properties)
      RecordFormat.fromSuffix("config.properties") shouldBe Some(RecordFormat.Properties)
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

    "load TOML tables and arrays into Record" in {
      Given("a Codex-style TOML document with nested MCP server tables")
      val toml =
        """[mcp_servers.research]
          |url = "https://mcp.example.test/tools"
          |enabled_tools = ["paper.search", "book.lookup"]
          |startup_timeout_sec = 10
          |""".stripMargin

      When("loading it through the generic record source boundary")
      val result = RecordSourceLoader.load(toml, RecordFormat.Toml)

      Then("nested values remain structured without a Codex-specific parser")
      result.toOption
        .flatMap(_.getRecord("mcp_servers"))
        .flatMap(_.getRecord("research"))
        .flatMap(_.getString("url")) shouldBe Some("https://mcp.example.test/tools")
      result.toOption
        .flatMap(_.getRecord("mcp_servers"))
        .flatMap(_.getRecord("research"))
        .flatMap(_.getDecimal("startup_timeout_sec")) shouldBe Some(BigDecimal(10))
    }

    "infer TOML format from its suffix" in {
      RecordFormat.fromSuffix("config.toml") shouldBe Some(RecordFormat.Toml)
    }

    "append TOML without changing existing RecordFormat ordinals" in {
      Given("the published RecordFormat enumeration")

      When("TOML input support is added")
      val existing = Vector(
        RecordFormat.Json,
        RecordFormat.Yaml,
        RecordFormat.Xml,
        RecordFormat.Hocon,
        RecordFormat.Properties,
        RecordFormat.Csv,
        RecordFormat.Tsv,
        RecordFormat.Ltsv,
        RecordFormat.Lines,
        RecordFormat.Tsl,
        RecordFormat.Excel
      )

      Then("existing ordinals remain stable and TOML is appended")
      existing.map(_.ordinal) shouldBe (0 until existing.size).toVector
      RecordFormat.Toml.ordinal shouldBe existing.size
    }

    "expose TOML through the generic Record import decoder" in {
      Given("a TOML document and the published import-format enumeration")
      val toml =
        """[service]
          |name = "research"
          |""".stripMargin
      val existing = Vector(
        RecordImportDecoder.RecordImportFormat.Auto,
        RecordImportDecoder.RecordImportFormat.Csv,
        RecordImportDecoder.RecordImportFormat.Tsv,
        RecordImportDecoder.RecordImportFormat.Ltsv,
        RecordImportDecoder.RecordImportFormat.Lines,
        RecordImportDecoder.RecordImportFormat.Json,
        RecordImportDecoder.RecordImportFormat.Yaml,
        RecordImportDecoder.RecordImportFormat.Xml,
        RecordImportDecoder.RecordImportFormat.Hocon,
        RecordImportDecoder.RecordImportFormat.Properties,
        RecordImportDecoder.RecordImportFormat.Tsl,
        RecordImportDecoder.RecordImportFormat.Excel
      )

      When("the import decoder receives the TOML format")
      val result = new RecordImportDecoder().decode(
        toml,
        RecordImportDecoder.RecordImportFormat.Toml
      )

      Then("TOML is decoded without changing existing import-format ordinals")
      existing.map(_.ordinal) shouldBe (0 until existing.size).toVector
      RecordImportDecoder.RecordImportFormat.Toml.ordinal shouldBe existing.size
      result.toOption
        .flatMap(_.records.headOption)
        .flatMap(_.getRecord("service"))
        .flatMap(_.getString("name")) shouldBe Some("research")
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
