package org.goldenport.statemachine

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import org.goldenport.Consequence

/*
 * @since   Mar. 20, 2026
 * @version Mar. 20, 2026
 * @author  ASAMI, Tomoharu
 */
class TransitionDeciderSpec
  extends AnyWordSpec
    with GivenWhenThen
    with Matchers
    with ScalaCheckDrivenPropertyChecks {

  private case class Runtime(current: State)

  private val start = State("start")
  private val middle = State("middle")
  private val end = State("end")

  private def machine(transitions: Vector[Transition[Runtime, String]]) =
    StateMachine[Runtime, String](
      states = Vector(start, middle, end),
      initial = start,
      transitions = transitions,
      stateOf = _.current
    )

  "TransitionDecider" should {
    "select smaller priority first" in {
      Given("two matching transitions with different priorities")
      val sm = machine(
        Vector(
          Transition[Runtime, String](start, middle, "go", priority = 20),
          Transition[Runtime, String](start, end, "go", priority = 10)
        )
      )

      When("deciding transition")
      val result = TransitionDecider.decide(sm, Runtime(start), "go")

      Then("priority 10 transition is selected")
      result.map(_.map(_.to)) shouldBe Consequence.success(Some(end))
    }

    "use declaration order for same priority" in {
      Given("two matching transitions with same priority")
      val sm = machine(
        Vector(
          Transition[Runtime, String](start, middle, "go", priority = 10),
          Transition[Runtime, String](start, end, "go", priority = 10)
        )
      )

      When("deciding transition")
      val result = TransitionDecider.decide(sm, Runtime(start), "go")

      Then("first declared transition is selected")
      result.map(_.map(_.to)) shouldBe Consequence.success(Some(middle))
    }

    "treat guard false as non-match" in {
      Given("first guard false and second transition guard-less")
      val falseguard = new Guard[Runtime, String] {
        def eval(state: Runtime, event: String): Consequence[Boolean] =
          Consequence.success(false)
      }
      val sm = machine(
        Vector(
          Transition[Runtime, String](start, middle, "go", guard = Some(falseguard), priority = 10),
          Transition[Runtime, String](start, end, "go", priority = 20)
        )
      )

      When("deciding transition")
      val result = TransitionDecider.decide(sm, Runtime(start), "go")

      Then("second transition is selected")
      result.map(_.map(_.to)) shouldBe Consequence.success(Some(end))
    }

    "propagate guard evaluation failure" in {
      Given("guard evaluation failure")
      val errorguard = new Guard[Runtime, String] {
        def eval(state: Runtime, event: String): Consequence[Boolean] =
          Consequence.failure("guard evaluation error")
      }
      val sm = machine(
        Vector(
          Transition[Runtime, String](start, middle, "go", guard = Some(errorguard), priority = 10),
          Transition[Runtime, String](start, end, "go", priority = 20)
        )
      )

      When("deciding transition")
      val result = TransitionDecider.decide(sm, Runtime(start), "go")

      Then("failure is returned and scanning stops")
      result shouldBe a[Consequence.Failure[?]]
    }
  }
}
