package org.goldenport.configuration.source

import java.nio.file.Path

import org.goldenport.Consequence
import org.goldenport.configuration.ConfigurationValue
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalacheck.Gen

/*
 * @since   Mar. 13, 2026
 * @version Mar. 24, 2026
 * @author  ASAMI, Tomoharu
 */
class ConfigurationSourceSpec
  extends AnyWordSpec
    with GivenWhenThen
    with Matchers
    with ScalaCheckDrivenPropertyChecks {

  "ConfigurationSource" should {
    "use .simplemodeling as default config directory" in {
      Given("a project path")
      val cwd = Path.of("/tmp/example")

      When("building project configuration sources with default settings")
      val sources = ConfigurationSource.project(cwd)
      val files = sources.collect { case f: ConfigurationSource.File => f }

      Then("all file locations are under .simplemodeling")
      files.map(_.path.getParent.getFileName.toString).toSet shouldBe Set(".simplemodeling")
    }

    "treat application name with and without leading dot as equivalent" in {
      Given("arbitrary numeric app ids")
      val idgen = Gen.chooseNum(1, Int.MaxValue)
      val cwd = Path.of("/tmp/example")

      forAll(idgen) { id =>
        val app = s"app$id"

        When("building sources with app and .app")
        val plain = ConfigurationSource.project(cwd, app).collect { case f: ConfigurationSource.File => f.path }
        val dotted = ConfigurationSource.project(cwd, s".$app").collect { case f: ConfigurationSource.File => f.path }

        Then("both resolve to the same paths")
        plain shouldBe dotted
      }
    }

    "canonicalize CNCF-style env key into dot notation" in {
      Given("cncf-prefixed env variable")
      val src = ConfigurationSource.env(
        Map("CNCF_DATASTORE_SQLITE_PATH" -> "/tmp/a.db"),
        "cncf"
      ).get

      When("loading env source")
      val cfg = src.load()

      Then("key is normalized to lowercase dot notation")
      cfg match {
        case Consequence.Success(c) =>
          c.values.get("cncf.datastore.sqlite.path") shouldBe Some(
            ConfigurationValue.StringValue("/tmp/a.db")
          )
        case Consequence.Failure(err) =>
          fail(s"unexpected failure: ${err.print}")
      }
    }

    "canonicalize MYAPP-style env key into dot notation" in {
      Given("myapp-prefixed env variable")
      val src = ConfigurationSource.env(
        Map("MYAPP_FOO_BAR" -> "x"),
        "myapp"
      ).get

      When("loading env source")
      val cfg = src.load()

      Then("key is normalized to lowercase dot notation")
      cfg match {
        case Consequence.Success(c) =>
          c.values.get("myapp.foo.bar") shouldBe Some(ConfigurationValue.StringValue("x"))
        case Consequence.Failure(err) =>
          fail(s"unexpected failure: ${err.print}")
      }
    }

    "exclude env keys with unmatched application prefix" in {
      Given("mixed env variables with unrelated prefixes")
      val src = ConfigurationSource.env(
        Map(
          "CNCF_DATASTORE_SQLITE_PATH" -> "/tmp/a.db",
          "MYAPP_HTTP_TIMEOUT_SECONDS" -> "30"
        ),
        "cncf"
      ).get

      When("loading cncf env source")
      val cfg = src.load()

      Then("only CNCF_* keys are imported")
      cfg match {
        case Consequence.Success(c) =>
          c.values.keySet should contain ("cncf.datastore.sqlite.path")
          c.values.keySet should not contain "myapp.http.timeout.seconds"
        case Consequence.Failure(err) =>
          fail(s"unexpected failure: ${err.print}")
      }
    }
  }
}
