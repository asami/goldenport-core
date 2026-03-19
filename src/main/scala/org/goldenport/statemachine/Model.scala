package org.goldenport.statemachine

import org.goldenport.Consequence

/*
 * @since   May. 20, 2025
 * @version Mar. 20, 2026
 * @author  ASAMI, Tomoharu
 */
/** Canonical state identifier used by the core state machine boundary. */
final case class State(
  name: String
)

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
  priority: Int = 100
)

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
