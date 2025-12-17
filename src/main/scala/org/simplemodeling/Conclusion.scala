package org.simplemodeling

import observation.Observation

/*
 * @since   Jul. 19, 2025
 * @version Jul. 20, 2025
 * @author  ASAMI, Tomoharu
 */
case class Conclusion(
  status: Conclusion.Status,
  observation: Observation,
  previous: Option[Conclusion] = None
)

object Conclusion {
  case class Status(
    webCode: WebCode
  )

  case class WebCode(code: Int)

  def from(p: Throwable): Conclusion = ???
}
