package org.simplemodeling

import observation.Observation
import org.simplemodeling.error.{ErrorCode, ErrorStrategy}

/*
 * @since   Jul. 19, 2025
 *  version Jul. 20, 2025
 * @version Dec. 18, 2025
 * @author  ASAMI, Tomoharu
 */
case class Conclusion(
  status: Conclusion.Status,
  observation: Observation,
  previous: Option[Conclusion] = None
)

object Conclusion {
  // Status represents semantic classification of an error.
  // It may carry declarative strategies and error codes,
  // but does not select concrete handlers or perform actions.
  case class Status(
    webCode: WebCode,
    detailCodes: List[ErrorCode] = Nil,
    strategies: List[ErrorStrategy] = Nil
  )

  case class WebCode(code: Int)

  def from(p: Throwable): Conclusion = ???
}
