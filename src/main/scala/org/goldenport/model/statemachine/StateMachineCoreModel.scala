package org.goldenport.model.statemachine

/**
 * Canonical core model for Phase 4 state machine semantics.
 *
 * Compatibility note:
 * `PostStatus` and `Aliveness` remain adapter/view enums.
 * Canonical transition semantics are defined by this package.
 */
/*
 * @since   May. 20, 2025
 * @version Mar. 20, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait GuardExpr
object GuardExpr {
  final case class Ref(name: String) extends GuardExpr
  final case class Expression(expr: String) extends GuardExpr
}

final case class ActionRef(
  name: String
)

final case class EventDef(
  name: String
)

final case class TransitionDef(
  to: String,
  event: String,
  guard: Option[GuardExpr],
  actions: Vector[ActionRef],
  priority: Int
)

final case class StateDef(
  name: String,
  transitions: Vector[TransitionDef] = Vector.empty
)

final case class StateMachineDef(
  name: String,
  states: Vector[StateDef],
  initialState: String
) {
  private val _states_by_name = states.map(x => x.name -> x).toMap

  def state(name: String): Option[StateDef] =
    _states_by_name.get(name)

  def transitions(currentstate: String, event: String): Vector[(TransitionDef, Int)] =
    _states_by_name
      .get(currentstate)
      .map(_.transitions.zipWithIndex.filter { case (t, _) => t.event == event })
      .getOrElse(Vector.empty)
}
