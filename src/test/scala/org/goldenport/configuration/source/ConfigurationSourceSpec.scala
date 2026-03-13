package org.goldenport.configuration.source

import java.nio.file.Path

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalacheck.Gen

/*
 * @since   Mar. 13, 2026
 * @version Mar. 13, 2026
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
  }
}
