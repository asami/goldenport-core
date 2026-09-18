package org.goldenport.statemachine

import org.goldenport.Consequence

/*
 * @since   May. 20, 2025
 *  version Mar. 20, 2026
 * @version Sep. 18, 2026
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
   * Orders a normalized transition set.
   *
   * Unlike [[ordered]], this method does not invent declaration order from a
   * collection position.  It is the admission boundary for a normalized CML
   * transition model; callers with legacy transition values must use the
   * compatibility method above until they have supplied explicit identities.
   */
  def orderedCanonical[S, E](
    transitions: Seq[Transition[S, E]]
  ): Consequence[Vector[Transition[S, E]]] = {
    val values = transitions.toVector
    val missing = values.exists(_.identity.isEmpty)
    val identities = values.flatMap(_.identity)
    val mixedmachine = identities.map(_.machine).distinct.size > 1
    val duplicateidentity = identities.distinct.size != identities.size
    val invalidpriority = values.exists(_.priority < 0)
    val invaliddeclarationorder = identities.exists(_.declarationOrder < 0)
    val duplicateorder = values.flatMap { x =>
      x.identity.map(identity => (identity.machine, x.from, x.event, x.priority, identity.declarationOrder))
    }
    val duplicatecandidateorder = duplicateorder.distinct.size != duplicateorder.size

    if (missing)
      Consequence.operationInvalid("canonical transition requires an explicit identity and declaration order")
    else if (mixedmachine)
      Consequence.operationInvalid("canonical transition set mixes StateMachine identities")
    else if (duplicateidentity)
      Consequence.operationInvalid("canonical transition identity is duplicated")
    else if (invalidpriority)
      Consequence.operationInvalid("canonical transition priority must be non-negative")
    else if (invaliddeclarationorder)
      Consequence.operationInvalid("canonical transition declaration order must be non-negative")
    else if (duplicatecandidateorder)
      Consequence.operationInvalid("canonical transition candidate order is duplicated")
    else
      Consequence.success(values.sortBy(x => (x.priority, x.identity.get.declarationOrder)))
  }

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

  /**
   * Strict counterpart to [[decide]] for normalized declarations.
   *
   * It preserves false-guard continuation and terminal guard failure while
   * rejecting incomplete or ambiguous declaration identity before evaluation.
   */
  def decideCanonical[S, E](
    machine: StateMachine[S, E],
    state: S,
    event: E
  ): Consequence[Option[Transition[S, E]]] = {
    val current = machine.stateOf(state)
    val candidates =
      machine.transitions.filter(t => t.from == current && t.event == event)

    orderedCanonical(candidates).flatMap { orderedtransitions =>
      _decide_ordered(orderedtransitions, state, event)
    }
  }

  /**
   * Typed selection result for the normalized path.
   *
   * A selected value contains only the candidate target and the declared
   * effects. It does not execute any effect or mutate the supplied state.
   */
  def decideCanonicalOutcome[S, E](
    machine: StateMachine[S, E],
    state: S,
    event: E
  ): Consequence[TransitionSelectionOutcome[S, E]] =
    decideCanonical(machine, state, event).map {
      case Some(transition) =>
        TransitionSelectionOutcome.Selected(
          TransitionPlan(
            transition = transition,
            candidateState = transition.to,
            effects = transition.effects
          )
        )
      case None => TransitionSelectionOutcome.NoMatch()
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
