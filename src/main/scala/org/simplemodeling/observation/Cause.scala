package org.simplemodeling.observation

/*
 * @since   Jul. 19, 2025
 * @version Jul. 19, 2025
 * @author  ASAMI, Tomoharu
 */
sealed trait Cause {
}

object Cause {
  case object SyntaxError extends Cause
  case object FormatError extends Cause
  case object ValueDomainError extends Cause
  case object InvalidState extends Cause
  case class Argument(reason: Reason) extends Cause
  case class Configuration(reason: Reason) extends Cause
  case object Timeout extends Cause
  case object DataCorruption extends Cause
  case object ValidationError extends Cause
  case object ResourceExhausted extends Cause
  case object Unknown extends Cause

  enum Reason {
    case Missng, Redundant, ValidationError
  }
}
