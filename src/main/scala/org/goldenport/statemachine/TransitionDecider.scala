package org.goldenport.statemachine

import org.goldenport.Consequence

/*
 * @since   May. 20, 2025
 * @version Mar. 20, 2026
 * @author  ASAMI, Tomoharu
 */
/** Deterministic transition selection for the canonical state machine core. */
object TransitionDecider {
  /**
   * Orders transitions deterministically:
   * 1) priority ascending
   * 2) declaration order ascending
   */
  def ordered[S, E](
    transitions: Seq[Transition[S, E]]
  ): Vector[Transition[S, E]] =
    transitions
      .zipWithIndex
      .sortBy { case (t, order) => (t.priority, order) }
      .map(_._1)
      .toVector

  /**
   * Returns:
   * - Success(Some(transition)) when selected
   * - Success(None) when no match
   * - Failure when guard evaluation fails
   */
  def decide[S, E](
    machine: StateMachine[S, E],
    state: S,
    event: E
  ): Consequence[Option[Transition[S, E]]] = {
    val current = machine.stateOf(state)
    val candidates =
      machine.transitions.filter(t => t.from == current && t.event == event)

    _decide_ordered(ordered(candidates), state, event)
  }

  private def _decide_ordered[S, E](
    transitions: Vector[Transition[S, E]],
    state: S,
    event: E
  ): Consequence[Option[Transition[S, E]]] =
    if (transitions.isEmpty)
      Consequence.success(None)
    else {
      val head = transitions.head
      val tail = transitions.tail
      _guard_match(head, state, event).flatMap { matched =>
        if (matched)
          Consequence.success(Some(head))
        else
          _decide_ordered(tail, state, event)
      }
    }

  private def _guard_match[S, E](
    transition: Transition[S, E],
    state: S,
    event: E
  ): Consequence[Boolean] =
    transition.guard match {
      case Some(g) => g.eval(state, event)
      case None => Consequence.success(true)
    }
}
