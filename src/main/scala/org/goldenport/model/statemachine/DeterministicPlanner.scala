package org.goldenport.model.statemachine

sealed trait PlannerError {
  def message: String
}

object PlannerError {
  final case class GuardEvaluation(guard: GuardExpr, error: GuardError) extends PlannerError {
    val message = s"Guard evaluation failure: ${error.message}"
  }
}

final case class PlannedTransition(
  transition: TransitionDef,
  declarationOrder: Int
)

object DeterministicPlanner {
  def select[S, E](
    sm: StateMachineDef,
    currentstate: String,
    eventname: String,
    state: S,
    event: E,
    evaluator: GuardEvaluator[S, E]
  ): Either[PlannerError, Option[PlannedTransition]] = {
    val candidates = sm.transitions(currentstate, eventname)

    val matched =
      candidates.foldLeft(Right(Vector.empty): Either[PlannerError, Vector[PlannedTransition]]) {
        case (Left(err), _) => Left(err)
        case (Right(acc), (transition, order)) =>
          _is_guard_match(transition.guard, state, event, evaluator).map { ok =>
            if (ok)
              acc :+ PlannedTransition(transition, order)
            else
              acc
          }
      }

    matched.map { transitions =>
      transitions
        .sortBy(x => (x.transition.priority, x.declarationOrder))
        .headOption
    }
  }

  private def _is_guard_match[S, E](
    guard: Option[GuardExpr],
    state: S,
    event: E,
    evaluator: GuardEvaluator[S, E]
  ): Either[PlannerError, Boolean] =
    guard match {
      case None => Right(true)
      case Some(g) =>
        evaluator.eval(g, state, event) match {
          case Right(value) => Right(value)
          case Left(err) => Left(PlannerError.GuardEvaluation(g, err))
        }
    }
}
