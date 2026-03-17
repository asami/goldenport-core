package org.goldenport.model.statemachine

import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Dec. 22, 2025
 * @version Mar. 18, 2026
 * @author  ASAMI, Tomoharu
 */
class AlivenessSpec
  extends AnyWordSpec
    with ScalaCheckDrivenPropertyChecks
    with Matchers
    with GivenWhenThen {

  "Aliveness" should {
    "use Alive as default state" in {
      Given("the Aliveness default")
      val state = Aliveness.default

      When("evaluating operational flag")
      val operational = state.isOperational

      Then("default is Alive and operational")
      state shouldBe Aliveness.Alive
      operational shouldBe true
    }

    "round-trip enum values through textual representation" in {
      Given("an arbitrary Aliveness state")
      val stategen = Gen.oneOf(Aliveness.values.toSeq)

      forAll(stategen) { state =>
        When("converting to string and parsing via from")
        val parsed = Aliveness.from(state.value)

        Then("the same state is restored")
        parsed shouldBe Some(state)
      }
    }

    "keep lowercase state name and db value mapping" in {
      Given("an arbitrary Aliveness state")
      val stategen = Gen.oneOf(Aliveness.values.toSeq)

      forAll(stategen) { state =>
        When("reading stateName and resolving from db value")
        val name = state.stateName
        val parsed = Aliveness.fromDbValue(state.dbValue)

        Then("state name is lowercase and db value round-trips")
        name shouldBe name.toLowerCase
        parsed shouldBe Some(state)
      }
    }

    "reject unknown textual state" in {
      Given("an unknown textual state")
      val unknown = "hibernating"

      When("parsing through from")
      val parsed = Aliveness.from(unknown)

      Then("no state is resolved")
      parsed shouldBe None
    }
  }
}
