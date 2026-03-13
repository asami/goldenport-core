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
 * @version Mar. 13, 2026
 * @author  ASAMI, Tomoharu
 */
class FileConfigLoaderSpec
  extends AnyWordSpec
    with GivenWhenThen
    with Matchers
    with ScalaCheckDrivenPropertyChecks {

  private val loader = new SimpleFileConfigLoader
  private val extensions = Vector("conf", "props", "properties", "json", "yaml")

  "SimpleFileConfigLoader" should {
    "load supported extensions by format mapping" in {
      Given("property-safe keys and values")
      val keygen = Gen.chooseNum(1, Int.MaxValue)
      val valuegen = Gen.chooseNum(1, Int.MaxValue)

      forAll(keygen, valuegen) { (keyid, valueid) =>
        val key = s"key$keyid"
        val value = s"value$valueid"
        extensions.foreach { ext =>
          val dir = Files.createTempDirectory("sm-config-loader-")
          val path = dir.resolve(s"config.$ext")
          val content = _content(ext, key, value)
          Files.writeString(path, content)

          When(s"loading config.$ext")
          val result = loader.load(path)

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

    "treat .properties as HOCON" in {
      Given("a .properties file using nested HOCON object syntax")
      val dir = Files.createTempDirectory("sm-config-loader-")
      val path = dir.resolve("config.properties")
      Files.writeString(path, "service { enabled = true, retries = 3 }")

      When("loading the file")
      val result = loader.load(path)

      Then("nested values are preserved as structured ConfigurationValue")
      result match {
        case Consequence.Success(cfg) =>
          cfg.values.get("service") shouldBe Some(
            ConfigurationValue.ObjectValue(
              Map(
                "enabled" -> ConfigurationValue.BooleanValue(true),
                "retries" -> ConfigurationValue.NumberValue(BigDecimal(3))
              )
            )
          )
        case Consequence.Failure(err) =>
          fail(s"unexpected parse failure: ${err.print}")
      }
    }
  }

  private def _content(ext: String, key: String, value: String): String =
    ext match {
      case "conf" | "props" | "properties" => s"$key = \"$value\""
      case "json" => s"{\"$key\":\"$value\"}"
      case "yaml" => s"$key: \"$value\""
      case _ => s"$key = \"$value\""
    }
}
