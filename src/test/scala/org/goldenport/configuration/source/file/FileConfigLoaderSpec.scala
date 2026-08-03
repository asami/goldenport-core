package org.goldenport.configuration.source.file

import java.nio.file.{Files, Path}
import java.util.Comparator
import scala.util.Using

import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import org.goldenport.Consequence
import org.goldenport.configuration.ConfigurationValue
import org.goldenport.configuration.ConfigurationDocument

/*
 * @since   Mar. 13, 2026
 *  version Jul.  2, 2026
 * @version Aug.  3, 2026
 * @author  ASAMI, Tomoharu
 */
class FileConfigLoaderSpec
  extends AnyWordSpec
    with GivenWhenThen
    with Matchers
    with BeforeAndAfterEach
    with ScalaCheckDrivenPropertyChecks {

  private val _loader = new SimpleFileConfigLoader
  private val _extensions = Vector("conf", "props", "properties", "json", "yaml", "xml")
  private var _temporary_directories = Vector.empty[Path]

  override protected def afterEach(): Unit =
    try super.afterEach()
    finally {
      _temporary_directories.reverse.foreach(_delete_tree)
      _temporary_directories = Vector.empty
    }

  "SimpleFileConfigLoader" should {
    "load supported extensions by format mapping" in {
      Given("property-safe keys and values")
      val keygen = Gen.chooseNum(1, Int.MaxValue)
      val valuegen = Gen.chooseNum(1, Int.MaxValue)

      forAll(keygen, valuegen) { (keyid, valueid) =>
        val key = s"key$keyid"
        val value = s"value$valueid"
        _extensions.foreach { ext =>
          val dir = _temporary_directory()
          val path = dir.resolve(s"config.$ext")
          val content = _content(ext, key, value)
          Files.writeString(path, content)

          When(s"loading config.$ext")
          val result = _loader.load(path)

          Then("the key is loaded as StringValue")
          result match {
            case Consequence.Success(cfg) =>
              cfg.values.get(key) shouldBe Some(ConfigurationValue.StringValue(value))
            case Consequence.Failure(err) =>
              fail(s"unexpected parse failure for .$ext: ${err.print}")
          }
        }
      }
    }



    "reject scalar JSON YAML and malformed XML roots" in {
      Given("scalar and malformed config documents")
      val cases = Vector(
        "json" -> "\"plain-string\"",
        "yaml" -> "plain-string\n",
        "xml" -> "plain-string"
      )

      cases.foreach { case (ext, content) =>
        val dir = _temporary_directory()
        val path = dir.resolve(s"config.$ext")
        Files.writeString(path, content)

        When(s"loading scalar config.$ext")
        val result = _loader.load(path)

        Then("the config decoder rejects non-object roots or malformed XML")
        result match {
          case Consequence.Failure(_) => succeed
          case Consequence.Success(cfg) => fail(s"expected failure for .$ext, got $cfg")
        }
      }
    }

    "reject XML documents with external entity declarations" in {
      Given("an XML config document with an external entity declaration")
      val dir = _temporary_directory()
      val path = dir.resolve("config.xml")
      val xml =
        """<!DOCTYPE config [ <!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>
          |<config><secret>&xxe;</secret></config>""".stripMargin
      Files.writeString(path, xml)

      When("loading the XML config")
      val result = _loader.load(path)

      Then("the hardened decoder rejects the document before entity expansion")
      result match {
        case Consequence.Failure(_) => succeed
        case Consequence.Success(cfg) => fail(s"expected hardened XML parse failure, got $cfg")
      }
    }

    "treat .properties as Java properties" in {
      Given("a .properties file using Java properties syntax")
      val dir = _temporary_directory()
      val path = dir.resolve("config.properties")
      Files.writeString(path, "service.enabled=true\nservice.retries=3\n")

      When("loading the file")
      val result = _loader.load(path)

      Then("property values are loaded as string ConfigurationValue entries")
      result match {
        case Consequence.Success(cfg) =>
          cfg.values.get("service.enabled") shouldBe Some(ConfigurationValue.StringValue("true"))
          cfg.values.get("service.retries") shouldBe Some(ConfigurationValue.StringValue("3"))
        case Consequence.Failure(err) =>
          fail(s"unexpected parse failure: ${err.print}")
      }
    }

    "retain YAML duplicate mapping members in snapshot order and established scalar compatibility" in {
      Given("a YAML file with repeated members plus YAML-native boolean and radix number scalars")
      val dir = _temporary_directory()
      val path = dir.resolve("config.yaml")
      Files.writeString(
        path,
        """setting: first
          |setting: second
          |enabled: yes
          |limit: 0x10
          |explicit-null: !!null foo
          |explicit-null-control: !!null "\0"
          |published: 2026-08-03
          |payload: !!binary SGVsbG8=
          |textus:
          |  service:
          |    timeout: 1000
          |    timeout: 2000
          |""".stripMargin
      )

      When("the loader captures its physical source snapshot")
      val result = _loader.loadSnapshot(path)

      Then("the legacy map keeps its established values while the raw document retains each occurrence")
      result match {
        case Consequence.Success(snapshot) =>
          snapshot.value.values.get("setting") shouldBe Some(ConfigurationValue.StringValue("second"))
          snapshot.value.values.get("enabled") shouldBe Some(ConfigurationValue.BooleanValue(true))
          snapshot.value.values.get("limit") shouldBe Some(ConfigurationValue.NumberValue(BigDecimal(16)))
          snapshot.value.values.get("explicit-null") shouldBe Some(ConfigurationValue.NullValue)
          snapshot.value.values.get("explicit-null-control") shouldBe Some(ConfigurationValue.NullValue)
          snapshot.rawDocument.map(_.fields.map(_.name)) shouldBe Some(Vector("setting", "setting", "enabled", "limit", "explicit-null", "explicit-null-control", "published", "payload", "textus"))
          snapshot.rawDocument.get.fields(4).value shouldBe ConfigurationDocument.Scalar(ConfigurationValue.NullValue)
          snapshot.rawDocument.get.fields(5).value shouldBe ConfigurationDocument.Scalar(ConfigurationValue.NullValue)
          val published = snapshot.rawDocument.get.fields(6).value.asInstanceOf[ConfigurationDocument.Scalar].value
          snapshot.value.values.get("published") shouldBe Some(published)
          val rawenabled = snapshot.rawDocument.get.fields(2).value.asInstanceOf[ConfigurationDocument.Scalar].value
          val rawlimit = snapshot.rawDocument.get.fields(3).value.asInstanceOf[ConfigurationDocument.Scalar].value
          val rawpayload = snapshot.rawDocument.get.fields(7).value.asInstanceOf[ConfigurationDocument.Scalar].value
          snapshot.value.values.get("enabled") shouldBe Some(rawenabled)
          snapshot.value.values.get("limit") shouldBe Some(rawlimit)
          snapshot.value.values.get("payload") shouldBe Some(rawpayload)
          val first = snapshot.rawDocument.get.fields.last.value.asInstanceOf[ConfigurationDocument.Object]
          val service = first.fields.head.value.asInstanceOf[ConfigurationDocument.Object]
          service.fields.map(_.name) shouldBe Vector("timeout", "timeout")
        case Consequence.Failure(err) => fail(s"unexpected YAML snapshot failure: ${err.print}")
      }
    }
  }

  private def _content(ext: String, key: String, value: String): String =
    ext match {
      case "conf" => s"$key = \"$value\""
      case "props" | "properties" => s"$key=$value"
      case "json" => s"{\"$key\":\"$value\"}"
      case "yaml" => s"$key: \"$value\""
      case "xml" => s"<config><$key>$value</$key></config>"
      case _ => s"$key = \"$value\""
    }

  private def _temporary_directory(): Path = {
    val root = Path.of("target")
    Files.createDirectories(root)
    val directory = Files.createTempDirectory(root, "sm-config-loader-")
    _temporary_directories = _temporary_directories :+ directory
    directory
  }

  private def _delete_tree(path: Path): Unit =
    if (Files.exists(path))
      Using.resource(Files.walk(path)) { paths =>
        paths.sorted(Comparator.reverseOrder()).forEach(x => Files.deleteIfExists(x))
      }
}
