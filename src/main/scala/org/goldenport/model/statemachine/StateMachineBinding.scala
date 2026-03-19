package org.goldenport.model.statemachine

sealed trait GuardError {
  def message: String
}

object GuardError {
  final case class NotFound(name: String) extends GuardError {
    val message = s"Guard not found: '$name'"
  }

  final case class EvaluationFailure(message: String) extends GuardError
}

sealed trait ActionError {
  def message: String
}

object ActionError {
  final case class NotFound(name: String) extends ActionError {
    val message = s"Action not found: '$name'"
  }

  final case class ResolutionFailure(message: String) extends ActionError
}

trait Guard[S, E] {
  def eval(state: S, event: E): Either[GuardError, Boolean]
}

trait ResolvedAction[S, E] {
  def execute(state: S, event: E): Either[ActionError, Unit]
}

trait GuardEvaluator[S, E] {
  def eval(guard: GuardExpr, state: S, event: E): Either[GuardError, Boolean]
}

trait GuardBindingResolver[S, E] {
  def resolve(name: String): Either[GuardError, Guard[S, E]]
}

trait ActionBindingResolver[S, E] {
  def resolve(name: String): Either[ActionError, ResolvedAction[S, E]]
}
