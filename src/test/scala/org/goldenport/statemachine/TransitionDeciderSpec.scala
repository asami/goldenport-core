package org.goldenport.statemachine

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import org.goldenport.Consequence

/*
 * @since   Mar. 20, 2026
 *  version Apr. 14, 2026
 * @version Sep. 18, 2026
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
  private val _lifecycle = StateMachineIdentity("example.lifecycle")

  private def _canonical(
    from: State,
    to: State,
    event: String,
    priority: Int,
    declarationorder: Int,
    guard: Option[Guard[Runtime, String]] = None
  ): Transition[Runtime, String] =
    Transition(
      from = from,
      to = to,
      event = event,
      guard = guard,
      priority = priority,
      identity = Some(TransitionIdentity(_lifecycle, declarationorder))
    )

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

    "select canonical transitions by explicit declaration order" in {
      Given("two canonical transitions supplied in the reverse collection order")
      val sm = machine(
        Vector(
          _canonical(start, end, "go", priority = 10, declarationorder = 2),
          _canonical(start, middle, "go", priority = 10, declarationorder = 1)
        )
      )

      When("the strict canonical selector decides the transition")
      val result = TransitionDecider.decideCanonical(sm, Runtime(start), "go")

      Then("declared order, rather than the incidental Vector order, wins")
      result.map(_.map(_.to)) shouldBe Consequence.success(Some(middle))
    }

    "return an unexecuted candidate-state plan for the selected canonical transition" in {
      Given("one canonical transition with a declared effect")
      var executions = 0
      val effect = new Effect[Runtime, String] {
        def execute(state: Runtime, event: String): Consequence[Unit] = {
          executions += 1
          Consequence.success(())
        }
      }
      val transition = _canonical(start, middle, "go", priority = 10, declarationorder = 0).copy(
        effects = Vector(effect)
      )
      val sm = machine(Vector(transition))

      When("the strict selector returns its typed outcome")
      val result = TransitionDecider.decideCanonicalOutcome(sm, Runtime(start), "go")

      Then("the candidate target and declared effects are returned without execution")
      result shouldBe Consequence.success(
        TransitionSelectionOutcome.Selected(
          TransitionPlan(transition, middle, Vector(effect))
        )
      )
      executions shouldBe 0
    }

    "reject incomplete canonical transition identity rather than inventing order" in {
      Given("a matching transition with no declared identity or declaration order")
      var guardexecutions = 0
      val guard = new Guard[Runtime, String] {
        def eval(state: Runtime, event: String): Consequence[Boolean] = {
          guardexecutions += 1
          Consequence.success(true)
        }
      }
      val sm = machine(Vector(Transition[Runtime, String](
        start,
        middle,
        "go",
        guard = Some(guard),
        priority = 10
      )))

      When("the strict canonical selector decides the transition")
      val result = TransitionDecider.decideCanonical(sm, Runtime(start), "go")

      Then("selection fails before evaluating the transition")
      result shouldBe a[Consequence.Failure[?]]
      guardexecutions shouldBe 0
    }

    "return a typed no-match outcome when no canonical transition matches" in {
      Given("a valid canonical machine with a transition for a different event")
      val sm = machine(Vector(_canonical(start, middle, "stop", priority = 10, declarationorder = 0)))

      When("the strict canonical selector returns its typed outcome for an unmatched event")
      val result = TransitionDecider.decideCanonicalOutcome(sm, Runtime(start), "go")

      Then("the outcome is an explicit canonical no-match")
      result shouldBe Consequence.success(TransitionSelectionOutcome.NoMatch())
    }

    "reject duplicate canonical candidate order" in {
      Given("two matching canonical transitions with the same identity and order")
      val sm = machine(
        Vector(
          _canonical(start, middle, "go", priority = 10, declarationorder = 1),
          _canonical(start, end, "go", priority = 10, declarationorder = 1)
        )
      )

      When("the strict canonical selector decides the transition")
      val result = TransitionDecider.decideCanonical(sm, Runtime(start), "go")

      Then("the ambiguous declaration is rejected")
      result shouldBe a[Consequence.Failure[?]]
    }

    "reject a canonical transition with an invalid priority" in {
      Given("one canonical transition with a negative priority")
      val sm = machine(Vector(_canonical(start, middle, "go", priority = -1, declarationorder = 0)))

      When("the strict canonical selector decides the transition")
      val result = TransitionDecider.decideCanonical(sm, Runtime(start), "go")

      Then("selection fails before evaluating the transition")
      result shouldBe a[Consequence.Failure[?]]
    }

    "reject a negative canonical declaration order at identity construction" in {
      Given("a StateMachine identity and an invalid negative declaration order")
      val machineidentity = StateMachineIdentity("example.lifecycle")

      When("the canonical transition identity is constructed")
      val result = intercept[IllegalArgumentException] {
        TransitionIdentity(machineidentity, -1)
      }

      Then("the malformed canonical order is rejected before selection")
      result.getMessage should include("declaration order")
    }

    "reject an empty canonical StateMachine identity at construction" in {
      Given("an empty qualified StateMachine declaration name")

      When("the canonical StateMachine identity is constructed")
      val result = intercept[IllegalArgumentException] {
        StateMachineIdentity("")
      }

      Then("the identity cannot reach canonical selection")
      result.getMessage should include("qualified name")
    }

    "reject a canonical candidate set that mixes machine identities" in {
      Given("otherwise matching transitions from two declared StateMachines")
      val other = StateMachineIdentity("example.other")
      val sm = machine(
        Vector(
          _canonical(start, middle, "go", priority = 10, declarationorder = 0),
          Transition(
            from = start,
            to = end,
            event = "go",
            priority = 20,
            identity = Some(TransitionIdentity(other, 1))
          )
        )
      )

      When("the strict canonical selector decides the transition")
      val result = TransitionDecider.decideCanonical(sm, Runtime(start), "go")

      Then("the accidental cross-machine candidate set is rejected")
      result shouldBe a[Consequence.Failure[?]]
    }

    "continue after a false canonical guard" in {
      Given("an earlier canonical guard that does not match")
      val falseguard = new Guard[Runtime, String] {
        def eval(state: Runtime, event: String): Consequence[Boolean] = Consequence.success(false)
      }
      val sm = machine(
        Vector(
          _canonical(start, middle, "go", priority = 10, declarationorder = 0, guard = Some(falseguard)),
          _canonical(start, end, "go", priority = 10, declarationorder = 1)
        )
      )

      When("the strict canonical selector decides")
      val result = TransitionDecider.decideCanonical(sm, Runtime(start), "go")

      Then("the next ordered candidate is selected")
      result.map(_.map(_.to)) shouldBe Consequence.success(Some(end))
    }

    "stop after a canonical guard failure" in {
      Given("an earlier canonical guard that fails to evaluate")
      val errorguard = new Guard[Runtime, String] {
        def eval(state: Runtime, event: String): Consequence[Boolean] =
          Consequence.operationInvalid("guard evaluation error")
      }
      val sm = machine(
        Vector(
          _canonical(start, middle, "go", priority = 10, declarationorder = 0, guard = Some(errorguard)),
          _canonical(start, end, "go", priority = 10, declarationorder = 1)
        )
      )

      When("the strict canonical selector decides")
      val result = TransitionDecider.decideCanonical(sm, Runtime(start), "go")

      Then("selection preserves the guard failure")
      result shouldBe a[Consequence.Failure[?]]
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
          Consequence.operationInvalid("guard evaluation error")
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
