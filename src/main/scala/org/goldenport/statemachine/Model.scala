package org.goldenport.statemachine

import org.goldenport.Consequence

/*
 * @since   May. 20, 2025
 *  version Mar. 20, 2026
 * @version Sep. 18, 2026
 * @author  ASAMI, Tomoharu
 */
/** Canonical state identifier used by the core state machine boundary. */
final case class State(
  name: String
)

/**
 * Nominal identity of a StateMachine declaration.
 *
 * This is declaration metadata only.  It is neither a persisted EntityId nor
 * a generated surrogate identifier.
 */
final case class StateMachineIdentity(
  qualifiedName: String
) {
  require(qualifiedName.trim.nonEmpty, "StateMachine identity requires a qualified name.")
}

/**
 * Stable identity of a transition in one declared StateMachine version.
 *
 * The declaration order is deliberately explicit: a caller must not derive a
 * persistent identifier or a hash from a source key in order to identify a
 * transition.
 */
final case class TransitionIdentity(
  machine: StateMachineIdentity,
  declarationOrder: Int
) {
  require(declarationOrder >= 0, "Transition declaration order must be non-negative.")
}

/**
 * Guard contract for transition selection.
 *
 * - Success(false): non-match
 * - Failure: guard evaluation failure
 */
trait Guard[S, E] {
  def eval(state: S, event: E): Consequence[Boolean]
}

/**
 * Effect contract for transition side effects.
 *
 * Core keeps this abstract and runtime-agnostic.
 */
trait Effect[S, E] {
  def execute(state: S, event: E): Consequence[Unit]
}

/**
 * Transition definition at the canonical core boundary.
 *
 * `priority`: smaller means higher priority.
 */
final case class Transition[S, E](
  from: State,
  to: State,
  event: E,
  guard: Option[Guard[S, E]] = None,
  effects: Vector[Effect[S, E]] = Vector.empty[Effect[S, E]],
  priority: Int = 100,
  identity: Option[TransitionIdentity] = None
)

/**
 * Pure plan selected for a single transition.  Constructing this value never
 * executes an effect; execution remains an adapter/runtime responsibility.
 */
final case class TransitionPlan[S, E](
  transition: Transition[S, E],
  candidateState: State,
  effects: Vector[Effect[S, E]]
) {
  require(candidateState == transition.to, "Transition candidate state must be the selected transition target.")
}

sealed trait TransitionSelectionOutcome[S, E]
object TransitionSelectionOutcome {
  final case class Selected[S, E](
    plan: TransitionPlan[S, E]
  ) extends TransitionSelectionOutcome[S, E]
  final case class NoMatch[S, E]() extends TransitionSelectionOutcome[S, E]
}

/**
 * Canonical state machine model consumed by adapters.
 *
 * `stateOf` maps runtime state payload into canonical `State`.
 */
final case class StateMachine[S, E](
  states: Vector[State],
  initial: State,
  transitions: Vector[Transition[S, E]],
  stateOf: S => State
)
