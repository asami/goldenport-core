package org.goldenport

import org.goldenport.observation.Observation

import org.goldenport.error.{ErrorCode, ErrorStrategy}
import org.goldenport.text.Presentable

/*
 * @since   Jul. 19, 2025
 *  version Jul. 20, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
case class Conclusion(
  status: Conclusion.Status,
  observation: Observation,
  previous: Option[Conclusion] = None
) extends Presentable {
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

  override def display: String =
    message

  override def show: String =
    s"${getClass.getSimpleName}(${message})"

  override final def print: String =
    display
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
  object WebCode {
    val Ok: WebCode = WebCode(200)
    val Created: WebCode = WebCode(201)
    val NoContent: WebCode = WebCode(204)

    val BadRequest: WebCode = WebCode(400)
    val Unauthorized: WebCode = WebCode(401)
    val Forbidden: WebCode = WebCode(403)
    val NotFound: WebCode = WebCode(404)
    val Conflict: WebCode = WebCode(409)

    val InternalError: WebCode = WebCode(500)
    val NotImplemented: WebCode = WebCode(501)
    val ServiceUnavailable: WebCode = WebCode(503)
  }

  /**
   * Boundary-only alias for clarity.
   * Prefer this method when converting a Throwable into a Conclusion.
   */
  def fromThrowable(p: Throwable): Conclusion =
    from(p)

  def from(p: Throwable): Conclusion = {
    val observation = Observation(
      phenomenon = org.goldenport.observation.Phenomenon.Failure,
      causeKind  = org.goldenport.observation.CauseKind.Defect,
      cause      = None,
      severity   = org.goldenport.observation.Severity.Error,
      strategy   = org.goldenport.observation.Strategy.Manual,
      handler    = org.goldenport.observation.Handler.Developer,
      timestamp  = java.time.Instant.EPOCH,
      subject    = org.goldenport.observation.Subject.System,
      `object`   = org.goldenport.observation.Resource.Unknown,
      agent      = org.goldenport.observation.Agent.System,
      location   = org.goldenport.observation.SystemLocation(None),
      traceId    = None,
      spanId     = None,
      descriptor = org.goldenport.observation.Descriptor(),
      message    = None,
      exception  = Option(p),
      properties = Map.empty
    )
    Conclusion(
      status = Status(webCode = WebCode.InternalError),
      observation = observation,
      previous = None
    )
  }

  /**
   * Minimal conclusion for library-level validation failures.
   * No runtime metadata, no side effects.
   */
  def simple(message: String): Conclusion =
    Conclusion(
      status = Status(
        webCode = WebCode.BadRequest
      ),
      observation = Observation.validationError(message),
      previous = None
    )

  def combine(a: Conclusion, b: Conclusion): Conclusion = {
    val as = a.causes
    val bs = b.causes
    val all = as ++ bs
    val maxSeverity =
      all.map(_.observation.severity).reduceLeft(org.goldenport.observation.Severity.max)
    val combined = all.reduceLeft { (acc, current) =>
      current.copy(previous = Some(acc))
    }
    combined.copy(observation = combined.observation.copy(severity = maxSeverity))
  }
}
