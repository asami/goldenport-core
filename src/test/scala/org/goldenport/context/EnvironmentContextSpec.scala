package org.goldenport.context

import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Dec. 30, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
class EnvironmentContextSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks {

  "EnvironmentContext" should {
    "hold core values explicitly" in {
      Given("an arbitrary environment name")
      val gen = Gen.alphaStr.suchThat(_.nonEmpty)
      forAll(gen) { name =>
        When("creating a core and instant context")
        val core = EnvironmentContext.Core(name)
        val ctx = EnvironmentContext.Instant(core)
        Then("core and holder expose the same environment")
        core.environment.shouldBe(name)
        ctx.environment.shouldBe(name)
      }
    }

    "provide an explicit local context" in {
      Given("no external detection")
      When("calling localCore and local")
      val core = EnvironmentContext.localCore()
      val ctx = EnvironmentContext.local()
      Then("local uses localCore as its value")
      ctx.core.shouldBe(core)
    }
  }
}
