package org.goldenport.model.statemachine

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalacheck.Gen

/*
 * @since   Mar. 19, 2026
 * @version Mar. 19, 2026
 * @author  ASAMI, Tomoharu
 */
class DeterministicPlannerSpec
  extends AnyWordSpec
    with GivenWhenThen
    with Matchers
    with ScalaCheckDrivenPropertyChecks {

  private val alwaysTrueEvaluator = new GuardEvaluator[Map[String, Any], EventDef] {
    def eval(
      guard: GuardExpr,
      state: Map[String, Any],
      event: EventDef
    ): Either[GuardError, Boolean] = Right(true)
  }

  "DeterministicPlanner" should {
    "select the smallest priority among matching transitions" in {
      Given("two transitions for the same state/event with different priorities")
      val sm =
        StateMachineDef(
          name = "demo",
          states = Vector(
            StateDef(
              "draft",
              transitions = Vector(
                TransitionDef("published", "submit", None, Vector.empty, priority = 20),
                TransitionDef("archived", "submit", None, Vector.empty, priority = 10)
              )
            )
          ),
          initialState = "draft"
        )

      When("selecting transition")
      val selected =
        DeterministicPlanner.select(
          sm,
          currentstate = "draft",
          eventname = "submit",
          state = Map.empty,
          event = EventDef("submit"),
          evaluator = alwaysTrueEvaluator
        )

      Then("priority 10 transition is selected")
      selected.map(_.map(_.transition.to)) shouldBe Right(Some("archived"))
    }

    "preserve declaration order when priority is the same" in {
      Given("two same-priority transitions")
      val sm =
        StateMachineDef(
          name = "demo",
          states = Vector(
            StateDef(
              "draft",
              transitions = Vector(
                TransitionDef("published", "submit", None, Vector.empty, priority = 10),
                TransitionDef("archived", "submit", None, Vector.empty, priority = 10)
              )
            )
          ),
          initialState = "draft"
        )

      When("selecting transition")
      val selected =
        DeterministicPlanner.select(
          sm,
          currentstate = "draft",
          eventname = "submit",
          state = Map.empty,
          event = EventDef("submit"),
          evaluator = alwaysTrueEvaluator
        )

      Then("first declared transition is selected")
      selected.map(_.map(_.transition.to)) shouldBe Right(Some("published"))
    }

    "return guard evaluation error explicitly" in {
      Given("a transition with guard and a failing evaluator")
      val evaluator = new GuardEvaluator[Map[String, Any], EventDef] {
        def eval(
          guard: GuardExpr,
          state: Map[String, Any],
          event: EventDef
        ): Either[GuardError, Boolean] =
          Left(GuardError.EvaluationFailure("invalid expression"))
      }
      val sm =
        StateMachineDef(
          name = "demo",
          states = Vector(
            StateDef(
              "draft",
              transitions = Vector(
                TransitionDef("published", "submit", Some(GuardExpr.Expression("x >")), Vector.empty, priority = 10)
              )
            )
          ),
          initialState = "draft"
        )

      When("selecting transition")
      val selected =
        DeterministicPlanner.select(
          sm,
          currentstate = "draft",
          eventname = "submit",
          state = Map.empty,
          event = EventDef("submit"),
          evaluator = evaluator
        )

      Then("guard evaluation failure is returned as Left")
      selected shouldBe a[Left[?, ?]]
    }

    "keep declaration-order determinism property-wise" in {
      Given("arbitrary target names with equal priority")
      val namegen = Gen.chooseNum(1, Int.MaxValue).map(i => s"s$i")

      forAll(namegen, namegen) { (first, second) =>
        val sm =
          StateMachineDef(
            name = "demo",
            states = Vector(
              StateDef(
                "draft",
                transitions = Vector(
                  TransitionDef(first, "submit", None, Vector.empty, priority = 10),
                  TransitionDef(second, "submit", None, Vector.empty, priority = 10)
                )
              )
            ),
            initialState = "draft"
          )

        When("selecting transition")
        val selected =
          DeterministicPlanner.select(
            sm,
            currentstate = "draft",
            eventname = "submit",
            state = Map.empty,
            event = EventDef("submit"),
            evaluator = alwaysTrueEvaluator
          )

        Then("the first declared target is always selected")
        selected.map(_.map(_.transition.to)) shouldBe Right(Some(first))
      }
    }
  }
}
