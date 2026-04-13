package org.goldenport

import cats.data.NonEmptyVector
import java.time.Instant
import org.goldenport.datatype.Identifier
import org.goldenport.record.Record
import org.goldenport.record.Record
import org.goldenport.error.{ErrorCode, ErrorStrategy}
import org.goldenport.provisional.conclusion.{Interpretation, Disposition}
import org.goldenport.provisional.observation.{Observation, Taxonomy, Cause, Source, Channel, Substrate, Origin}
import org.goldenport.provisional.observation.Occurrence
import org.goldenport.provisional.conclusion.{Interpretation, Disposition}
import org.goldenport.observation.Phenomenon
import org.goldenport.observation.{Subject, Agent, Resource, Severity}
import org.goldenport.observation.SourcePosition
import org.goldenport.observation.Descriptor
import org.goldenport.text.Presentable
import org.goldenport.schema.DataType
import org.goldenport.schema.Constraint
import org.goldenport.http.HttpRequest

/*
 * @since   Jul. 19, 2025
 *  version Jul. 20, 2025
 *  version Dec. 30, 2025
 *  version Jan. 31, 2026
 *  version Feb. 28, 2026
 *  version Apr. 11, 2026
 * @version Apr. 14, 2026
 * @author  ASAMI, Tomoharu
 */
case class Conclusion(
  status: Conclusion.Status,
  observation: Observation,
  interpretation: Interpretation,
  disposition: Disposition,
  previous: Option[Conclusion] = None
) extends Presentable {
  def withSourcePosition(p: SourcePosition): Conclusion =
    copy(observation = observation.withSourcePosition(p))

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
    observation.getEffectiveMessage.getOrElse(observation.show)

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

  def toRecord: Record = {
    Record.data(
      "status" -> status.toRecord,
      "observation" -> observation.toRecord,
      "interpretation" -> interpretation.toRecord,
      "disposition" -> disposition.toRecord
    ) ++ Record.dataOption(
      "previous" -> previous.map(_.toRecord)
    )
  }

  def toJsonString: String = toRecord.toJsonString

  def toYamlString: String = toRecord.toYamlString
}

object Conclusion {
  // Status represents semantic classification of an error.
  // It may carry declarative strategies and error codes,
  // but does not select concrete handlers or perform actions.
  case class Status(
    webCode: WebCode,
    detailCodes: List[ErrorCode] = Nil,
    strategies: List[ErrorStrategy] = Nil
  ) {
    def toRecord: Record = Record.data(
      "code" -> webCode.code
    )
  }
  object Status {
    val badRequest = Status(WebCode.BadRequest)
    val unauthorized = Status(WebCode.Unauthorized)
    val forbidden = Status(WebCode.Forbidden)
    val notFound = Status(WebCode.NotFound)
    val conflict = Status(WebCode.Conflict)
    val internalServerError = Status(WebCode.InternalServerError)
    val notImplemented = Status(WebCode.NotImplemented)
    val serviceUnavailable = Status(WebCode.ServiceUnavailable)

    // Hint
    def from(taxonomy: Taxonomy): Status = taxonomy.category match {
      case Taxonomy.Category.Argument => badRequest
      case Taxonomy.Category.Property => badRequest
      case Taxonomy.Category.Configuration => internalServerError
      case Taxonomy.Category.Resource => internalServerError
      case Taxonomy.Category.Reference => serviceUnavailable
      case Taxonomy.Category.State => internalServerError
      case Taxonomy.Category.Value => badRequest
      case Taxonomy.Category.Entity => badRequest
      case Taxonomy.Category.Security => taxonomy.symptom match {
        case Taxonomy.Symptom.AuthenticationRequired => unauthorized
        case _ => forbidden
      }
      case Taxonomy.Category.Record => badRequest
      case Taxonomy.Category.Operation => badRequest
      case Taxonomy.Category.Service => serviceUnavailable
      case Taxonomy.Category.Component => internalServerError
      case Taxonomy.Category.SubSystem => internalServerError
      case Taxonomy.Category.ServiceProvider => serviceUnavailable
      case Taxonomy.Category.System => internalServerError
      case Taxonomy.Category.DataStore => internalServerError // Hint
      case Taxonomy.Category.Network => serviceUnavailable
      case Taxonomy.Category.OutOfControl => internalServerError
    }
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

    val InternalServerError: WebCode = WebCode(500)
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
      status = Status(webCode = WebCode.InternalServerError),
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

  def failure(pos: SourcePosition, o: Observation): Conclusion = {
    val x = o.withSourcePosition(pos)
    Status.from(o.taxonomy).webCode match {
      case WebCode.BadRequest => badRequest(x)
      case WebCode.Unauthorized => unauthorized(x)
      case WebCode.Forbidden => forbidden(x)
      case WebCode.NotFound => notFound(x)
      case WebCode.Conflict => conflict(x)
      case WebCode.InternalServerError => internalServerError(x)
      case WebCode.NotImplemented => notImplemented(x)
      case WebCode.ServiceUnavailable => serviceUnavailable(x)
    }
  }

  def badRequest(o: Observation): Conclusion =
    Conclusion(
      Status.badRequest,
      o,
      Interpretation.domainFailure,
      Disposition.fix
    )

  def unauthorized(o: Observation): Conclusion =
    Conclusion(
      Status.unauthorized,
      o,
      Interpretation.domainFailure,
      Disposition.fix
    )

  def forbidden(o: Observation): Conclusion =
    Conclusion(
      Status.forbidden,
      o,
      Interpretation.domainFailure,
      Disposition.fix
    )

  def notFound(o: Observation): Conclusion =
    Conclusion(
      Status.notFound,
      o,
      Interpretation.domainFailure,
      Disposition.fix
    )

  def conflict(o: Observation): Conclusion =
    Conclusion(
      Status.conflict,
      o,
      Interpretation.domainFailure,
      Disposition.fix
    )

  def internalServerError(o: Observation): Conclusion =
    Conclusion(
      Status.internalServerError,
      o,
      Interpretation.systemFailure,
      Disposition.fix
    )

  def notImplemented(o: Observation): Conclusion =
    Conclusion(
      Status.notImplemented,
      o,
      Interpretation.defect,
      Disposition.defect
    )

  def serviceUnavailable(o: Observation): Conclusion =
    Conclusion(
      Status.serviceUnavailable,
      o,
      Interpretation.systemFailure,
      Disposition.serviceUnavailable
    )

  def failure(pos: SourcePosition, taxonomy: Taxonomy, facets: Seq[Descriptor.Facet]): Conclusion =
    failure(pos, taxonomy, Cause.create(facets))

  def failure(pos: SourcePosition, taxonomy: Taxonomy, cause: Cause): Conclusion =
    Status.from(taxonomy).webCode match {
      case WebCode.BadRequest => badRequest(pos, taxonomy, cause)
      case WebCode.Unauthorized => unauthorized(pos, taxonomy, cause)
      case WebCode.Forbidden => forbidden(pos, taxonomy, cause)
      case WebCode.NotFound => notFound(pos, taxonomy, cause)
      case WebCode.Conflict => conflict(pos, taxonomy, cause)
      case WebCode.InternalServerError => internalServerError(pos, taxonomy, cause)
      case WebCode.NotImplemented => notImplemented(pos, taxonomy, cause)
      case WebCode.ServiceUnavailable => serviceUnavailable(pos, taxonomy, cause)
    }

  def badRequest(pos: SourcePosition, taxonomy: Taxonomy, cause: Cause): Conclusion =
    badRequest(_make_rejection_observation(taxonomy, cause.withSourcePosition(pos)))

  def unauthorized(pos: SourcePosition, taxonomy: Taxonomy, cause: Cause): Conclusion =
    unauthorized(_make_rejection_observation(taxonomy, cause.withSourcePosition(pos)))

  def forbidden(pos: SourcePosition, taxonomy: Taxonomy, cause: Cause): Conclusion =
    forbidden(_make_rejection_observation(taxonomy, cause.withSourcePosition(pos)))

  def notFound(pos: SourcePosition, taxonomy: Taxonomy, cause: Cause): Conclusion =
    notFound(_make_rejection_observation(taxonomy, cause.withSourcePosition(pos)))

  def conflict(pos: SourcePosition, taxonomy: Taxonomy, cause: Cause): Conclusion =
    conflict(_make_rejection_observation(taxonomy, cause.withSourcePosition(pos)))

  def internalServerError(pos: SourcePosition, taxonomy: Taxonomy, cause: Cause): Conclusion =
    internalServerError(_make_failure_observation(taxonomy, cause.withSourcePosition(pos)))

  def notImplemented(pos: SourcePosition, taxonomy: Taxonomy, cause: Cause): Conclusion =
    notImplemented(_make_failure_observation(taxonomy, cause.withSourcePosition(pos)))

  def serviceUnavailable(pos: SourcePosition, taxonomy: Taxonomy, cause: Cause): Conclusion =
    serviceUnavailable(_make_failure_observation(taxonomy, cause.withSourcePosition(pos)))

  private def _make_rejection_observation(
    taxonomy: Taxonomy,
    cause: Cause
  ): Observation = {
    val occurrence = _make_occurence(taxonomy)
    Observation.rejection(taxonomy, cause, occurrence)
  }

  private def _make_failure_observation(
    taxonomy: Taxonomy,
    cause: Cause
  ): Observation = {
    val occurrence = _make_occurence(taxonomy)
    Observation.failure(taxonomy, cause, occurrence)
  }

  private def _make_occurence(taxonomy: Taxonomy): Option[Occurrence] = 
    taxonomy.category match {
      case Taxonomy.Category.Network => Some(Occurrence.network)
      case _ => None
    }

  // fail
  def notImplemented(pos: SourcePosition, msg: String): Conclusion =
    Conclusion(
      Status.internalServerError,
      Observation.notImplemented(pos, msg),
      Interpretation.notImplemented,
      Disposition.notImplemented
    )

  def unreachableReached(msg: String): Conclusion = failUnreachableReached(msg)

  def resourceNotFound(
    resource: Descriptor.Facet.Resource,
    facets: Seq[Descriptor.Facet]
  ): Conclusion =
    Conclusion(
      Status.notFound,
      Observation.resourceNotFound(resource, facets),
      Interpretation.resourceNotFound,
      Disposition.fix
    )

  def serviceProviderNotFound(name: String, facets: Seq[Descriptor.Facet]): Conclusion =
    Conclusion(
      Status.internalServerError,
      Observation.serviceProviderNotFound(name, facets),
      Interpretation.configurationFailure,
      Disposition.serviceUnavailable
    )

  def securityAuthenticationRequired(message: String): Conclusion =
    Conclusion(
      Status.unauthorized,
      Observation.securityAuthenticationRequired(message),
      Interpretation.domainFailure,
      Disposition.fix
    )

  def securityPermissionDenied(message: String): Conclusion =
    Conclusion(
      Status.forbidden,
      Observation.securityPermissionDenied(message),
      Interpretation.domainFailure,
      Disposition.fix
    )

  def securityPermissionDenied(message: String, facets: Seq[Descriptor.Facet]): Conclusion =
    Conclusion(
      Status.forbidden,
      Observation.securityPermissionDenied(message, facets),
      Interpretation.domainFailure,
      Disposition.fix
    )

  def entityNotFound(id: Identifier): Conclusion =
    Conclusion(
      Status.notFound,
      Observation.entityNotFound(id),
      Interpretation.notFound,
      Disposition.fix
    )

  // fail : obsolated
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

  def failResourceInconsistency(pos: SourcePosition): Conclusion =
    Conclusion(
      Status.internalServerError,
      Observation.resourceInconsistency(pos),
      Interpretation.resourceInconsistency,
      Disposition.resourceInconsistency
    )

  def failRecordNotFound(pos: SourcePosition, key: String, rec: Record): Conclusion =
    Conclusion(
      Status.badRequest,
      Observation.recordNotFound(pos, key, rec),
      Interpretation.recordNotFound,
      Disposition.recordNotFound
    )

  def failOperationNotFound(pos: SourcePosition, name: String): Conclusion =
    Conclusion(
      Status.badRequest,
      Observation.operationNotFound(pos, name),
      Interpretation.operationNotFound,
      Disposition.operationNotFound
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

  def failUnreachableReached(pos: SourcePosition): Conclusion =
    Conclusion(
      Status.internalServerError,
      Observation.unreachableReached(pos),
      Interpretation.unreachableReached,
      Disposition.unreachableReached
    )

  def failUnreachableReached(msg: String): Conclusion =
    Conclusion(
      Status.internalServerError,
      Observation.unreachableReached(msg),
      Interpretation.unreachableReached,
      Disposition.unreachableReached
    )

  def failUninitializedState(pos: SourcePosition): Conclusion =
    Conclusion(
      Status.internalServerError,
      Observation.uninitializedState(pos),
      Interpretation.uninitializedState,
      Disposition.uninitializedState
    )

  def failImpossibleState(msg: String): Conclusion =
    Conclusion(
      Status.internalServerError,
      Observation.impossibleState(msg),
      Interpretation.impossibleState,
      Disposition.impossibleState
    )

  def failUnsupported(msg: String): Conclusion =
    Conclusion(
      Status.internalServerError,
      Observation.unsupported(msg),
      Interpretation.unsupported,
      Disposition.unsupported
    )

  def failNotImplemented(pos: SourcePosition): Conclusion =
    Conclusion(
      Status.internalServerError,
      Observation.notImplemented(pos),
      Interpretation.notImplemented,
      Disposition.notImplemented
    )

  def failNotImplemented(msg: String): Conclusion =
    Conclusion(
      Status.internalServerError,
      Observation.notImplemented(msg),
      Interpretation.notImplemented,
      Disposition.notImplemented
    )

  def failInvariantViolation(msg: String): Conclusion =
    Conclusion(
      Status.internalServerError,
      Observation.invariantViolation(msg),
      Interpretation.invariantViolation,
      Disposition.invariantViolation
    )
  def failPreconditionViolation(msg: String): Conclusion =
    Conclusion(
      Status.internalServerError,
      Observation.preconditionViolation(msg),
      Interpretation.preconditionViolation,
      Disposition.preconditionViolation
    )
  def failPostconditionViolation(msg: String): Conclusion =
    Conclusion(
      Status.internalServerError,
      Observation.postconditionViolation(msg),
      Interpretation.postconditionViolation,
      Disposition.postconditionViolation
    )
}
