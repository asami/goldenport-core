package org.goldenport.configuration.source.file

import java.nio.file.Files

import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import org.goldenport.Consequence
import org.goldenport.configuration.ConfigurationValue

/*
 * @since   Mar. 13, 2026
 * @version Jul.  2, 2026
 * @author  ASAMI, Tomoharu
 */
class FileConfigLoaderSpec
  extends AnyWordSpec
    with GivenWhenThen
    with Matchers
    with ScalaCheckDrivenPropertyChecks {

  private val _loader = new SimpleFileConfigLoader
  private val _extensions = Vector("conf", "props", "properties", "json", "yaml", "xml")

  "SimpleFileConfigLoader" should {
    "load supported extensions by format mapping" in {
      Given("property-safe keys and values")
      val keygen = Gen.chooseNum(1, Int.MaxValue)
      val valuegen = Gen.chooseNum(1, Int.MaxValue)

      forAll(keygen, valuegen) { (keyid, valueid) =>
        val key = s"key$keyid"
        val value = s"value$valueid"
        _extensions.foreach { ext =>
          val dir = Files.createTempDirectory("sm-config-loader-")
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
        val dir = Files.createTempDirectory("sm-config-loader-")
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
      val dir = Files.createTempDirectory("sm-config-loader-")
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
      val dir = Files.createTempDirectory("sm-config-loader-")
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
}
