package org.simplemodeling.observation

/*
 * @since   Jul. 19, 2025
 * @version Dec. 24, 2025
 * @author  ASAMI, Tomoharu
 */
enum Cause derives CanEqual:
  case SyntaxError
  case FormatError
  case ValueDomainError
  case InvalidState

  case Argument(reason: Cause.Reason)
  case Configuration(reason: Cause.Reason)

  case Timeout
  case DataCorruption
  case ValidationError
  case ResourceExhausted
  case Unknown

object Cause:
  enum Reason derives CanEqual:
    case Missing
    case Redundant
    case ValidationError
