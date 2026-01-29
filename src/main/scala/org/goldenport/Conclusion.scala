package org.goldenport

import cats.data.NonEmptyVector
import java.time.Instant
import org.goldenport.error.{ErrorCode, ErrorStrategy}
import org.goldenport.provisional.conclusion.{Interpretation, Disposition}
import org.goldenport.provisional.observation.{Observation, Taxonomy, Cause, Source, Channel, Substrate, Origin}
import org.goldenport.provisional.conclusion.{Interpretation, Disposition}
import org.goldenport.observation.Phenomenon
import org.goldenport.observation.{Subject, Agent, Resource, Severity}
import org.goldenport.text.Presentable
import org.goldenport.schema.DataType
import org.goldenport.schema.Constraint
import org.goldenport.http.HttpRequest

/*
 * @since   Jul. 19, 2025
 *  version Jul. 20, 2025
 *  version Dec. 30, 2025
 * @version Jan. 29, 2026
 * @author  ASAMI, Tomoharu
 */
case class Conclusion(
  status: Conclusion.Status,
  observation: Observation,
  interpretation: Interpretation,
  disposition: Disposition,
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

  def displayMessage: String =
    observation.getEffectiveMessage.getOrElse(observation.taxonomy.print)

  def getException: Option[Throwable] =
    observation.exception

  def RAISE: Nothing =
    throw getException.getOrElse(new ConsequenceException(Consequence.Failure(this)))

  def RAISEC: Nothing =
    throw new ConsequenceException(Consequence.Failure(this))

  def isMatch(rhs: Conclusion): Boolean = (
    status == rhs.status &&
      observation.isMatch(rhs.observation) &&
      interpretation == rhs.interpretation &&
      disposition == rhs.disposition &&
      previous == rhs.previous
  )


  override def display: String = displayMessage

  override def show: String = s"Conclusion(${display})"

  override final def print: String = display

  // for test migration
  def cause: Cause = observation.cause

  def toCategoryArgument = copy(observation = observation.toCategoryArgument)
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
  object Status {
    val badRequest = Status(WebCode.BadRequest)
  }

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

  // val DefaultInterpretation: Interpretation =
  //   Interpretation(Interpretation.Kind.Defect, Disposition.Responsibility.Developer)

  // val DefaultDisposition: Disposition =
  //   Disposition(userAction = None, responsibility = None)

  /**
   * Boundary-only alias for clarity.
   * Prefer this method when converting a Throwable into a Conclusion.
   */
  def fromThrowable(p: Throwable): Conclusion =
    from(p)

  def from(p: Throwable): Conclusion = {
    val observation = Option(p) match {
      case Some(s) => _make_observation(
        taxonomy = Taxonomy.from(s),
        cause = Cause.from(s),
        severity = Some(Severity.Error)
      )
      case None => Observation.ofcNullPointer
    }
    Conclusion(
      status = Status(webCode = WebCode.InternalError),
      observation = observation,
      interpretation = Interpretation.from(p),
      disposition = Disposition.from(p),
      previous = None
    )
  }

  /**
   * Minimal conclusion for library-level validation failures.
   * No runtime metadata, no side effects.
   */
  def simple(message: String): Conclusion = {
    val observationMessage = Some(message)
    val observation = _make_observation(
      taxonomy = Taxonomy(Taxonomy.Category.Argument, Taxonomy.Symptom.DomainValue),
      cause = Cause.message(observationMessage),
      message = observationMessage,
      exception = None,
      severity = None
    )
    Conclusion(
      status = Status(webCode = WebCode.BadRequest),
      observation = observation,
      interpretation = Interpretation.domainFailure,
      disposition = Disposition.none,
      previous = None
    )
  }

  def combine(a: Conclusion, b: Conclusion): Conclusion = {
    val as = a.causes
    val bs = b.causes
    val all = as ++ bs
    val maxSeverity = all.flatMap(_.observation.severity).reduceOption(Severity.max)
    val combined = all.reduceLeft { (acc, current) =>
      current.copy(previous = Some(acc))
    }
    combined.copy(observation = combined.observation.withSeverity(maxSeverity))
  }

  private def _make_observation(
    taxonomy: Taxonomy,
    cause: Cause,
    severity: Option[Severity]
  ): Observation = _make_observation(taxonomy, cause, None, None, severity)

  private def _make_observation(
    taxonomy: Taxonomy,
    cause: Cause,
    message: Option[String],
    exception: Option[Throwable],
    severity: Option[Severity]
  ): Observation =
    Observation(
      phenomenon = Phenomenon.Failure,
      taxonomy = taxonomy,
      cause = cause.withMessage(message).withException(exception),
      timestamp = Instant.now()
    ).withSeverity(severity)

  // private def makeCause(kind: Cause.Kind, message: Option[String]): Cause =
  //   Cause(kind, message.map(m => Cause.Detail(message = Some(m))))

  def failArgumentMissing: Conclusion =
    Conclusion(
      Status.badRequest,
      Observation.argumentMissing,
      Interpretation.argumentMissing,
      Disposition.argumentMissing
    )

  def failArgumentMissing(name: String): Conclusion =
    Conclusion(
      Status.badRequest,
      Observation.argumentMissing(name),
      Interpretation.argumentMissing,
      Disposition.argumentMissing
    )

  def failArgumentMissingInput(name: String): Conclusion =
    ???

  def failArgumentMissingInput(args: Seq[String]): Conclusion =
    ???

  def failArgumentMissingInput(req: HttpRequest): Conclusion =
    ???

  def failArgumentMissingOperation(name: String, operation: String): Conclusion =
    Conclusion(
      Status.badRequest,
      Observation.argumentMissingOperation(name, operation),
      Interpretation.argumentMissing,
      Disposition.argumentMissing
    )

  def failArgumentRedundantOperation(name: String, operation: String): Conclusion =
    Conclusion(
      Status.badRequest,
      Observation.argumentRedundantOperation(name, operation),
      Interpretation.argumentRedundant,
      Disposition.argumentRedundant
    )

  def failArgumentRedundantOperationInput(operation: String, args: Seq[String]): Conclusion =
    Conclusion(
      Status.badRequest,
      Observation.argumentRedundantOperationInput(operation, args),
      Interpretation.argumentRedundant,
      Disposition.argumentRedundant
    )

  def failArgumentDataType(name: String, value: Any, dt: DataType): Conclusion =
    Conclusion(
      Status.badRequest,
      Observation.argumentDataType(name, value, dt),
      Interpretation.argumentDataType,
      Disposition.argumentDataType
    )

  def failArgumentConstraint(name: String, value: Any, cs: NonEmptyVector[Constraint]): Conclusion =
    Conclusion(
      Status.badRequest,
      Observation.argumentConstraint(name, value, cs),
      Interpretation.argumentConstraint,
      Disposition.argumentConstraint
    )

  def failOperationInvalid(name: String): Conclusion =
    Conclusion(
      Status.badRequest,
      Observation.operationInvalid(name),
      Interpretation.operationInvalid,
      Disposition.operationInvalid
    )

  def failValueInvalid(value: Any, dt: DataType): Conclusion =
    Conclusion(
      Status.badRequest,
      Observation.valueInvalid(value, dt),
      Interpretation.valueInvalid,
      Disposition.valueInvalid
    )

  def failValueFormatError(value: Any, dt: DataType): Conclusion =
    Conclusion(
      Status.badRequest,
      Observation.valueFormatError(value, dt),
      Interpretation.valueFormatError,
      Disposition.valueFormatError
    )
}
