package org.goldenport

import org.goldenport.observation.Observation

import org.goldenport.error.{ErrorCode, ErrorStrategy}

/*
 * @since   Jul. 19, 2025
 *  version Jul. 20, 2025
 * @version Dec. 25, 2025
 * @author  ASAMI, Tomoharu
 */
case class Conclusion(
  status: Conclusion.Status,
  observation: Observation,
  previous: Option[Conclusion] = None
) {
  /**
   * Returns all causal conclusions in order.
   * If this conclusion is not composite, returns a single-element list.
   */
  def causes: List[Conclusion] =
    previous match {
      case Some(p) => p.causes :+ this.copy(previous = None)
      case None    => List(this)
    }

  /**
   * Applicative-style composition.
   * Combines this conclusion with another while preserving order.
   */
  def ++(other: Conclusion): Conclusion =
    Conclusion.combine(this, other)

  def message: String =
    observation.displayMessage

  def getException: Option[Throwable] =
    observation.exception

  def RAISE: Nothing =
    throw getException.getOrElse(new ConsequenceException(Consequence.Failure(this)))

  def RAISEC: Nothing =
    throw new ConsequenceException(Consequence.Failure(this))
}

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

  /**
   * Minimal conclusion for library-level validation failures.
   * No runtime metadata, no side effects.
   */
  def simple(message: String): Conclusion =
    Conclusion(
      status = Status(
        webCode = WebCode(400)
      ),
      observation = Observation.validationError(message),
      previous = None
    )

  def combine(a: Conclusion, b: Conclusion): Conclusion = {
    val as = a.causes
    val bs = b.causes
    (as ++ bs).reduceLeft { (acc, current) =>
      current.copy(previous = Some(acc))
    }
  }
}
