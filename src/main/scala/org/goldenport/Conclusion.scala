package org.goldenport

import cats.data.NonEmptyVector
import java.time.Instant
import org.goldenport.datatype.Identifier
import org.goldenport.record.Record
import org.goldenport.error.DetailCode
import org.goldenport.conclusion.{Interpretation, Disposition}
import org.goldenport.observation.{Observation, Taxonomy, Cause, Source, Channel, Substrate, Origin}
import org.goldenport.observation.Occurrence
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
 *  version Apr. 14, 2026
 * @version May. 11, 2026
 * @author  ASAMI, Tomoharu
 */
final class Conclusion private (
  initialStatus: Conclusion.Status,
  val observation: Observation,
  val interpretation: Interpretation,
  val disposition: Disposition,
  val previous: Option[Conclusion] = None
) extends Presentable {
  val status: Conclusion.Status =
    initialStatus.materialize(this)

  def copy(
    status: Conclusion.Status = this.status,
    observation: Observation = this.observation,
    interpretation: Interpretation = this.interpretation,
    disposition: Disposition = this.disposition,
    previous: Option[Conclusion] = this.previous
  ): Conclusion =
    Conclusion(status, observation, interpretation, disposition, previous)

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

  override def equals(rhs: Any): Boolean = rhs match {
    case that: Conclusion =>
      status == that.status &&
        observation == that.observation &&
        interpretation == that.interpretation &&
        disposition == that.disposition &&
        previous == that.previous
    case _ => false
  }

  override def hashCode: Int =
    (status, observation, interpretation, disposition, previous).hashCode
}

object Conclusion {
  def apply(
    status: Conclusion.Status,
    observation: Observation,
    interpretation: Interpretation,
    disposition: Disposition,
    previous: Option[Conclusion] = None
  ): Conclusion =
    new Conclusion(status, observation, interpretation, disposition, previous)

  def unapply(p: Conclusion): Option[(Conclusion.Status, Observation, Interpretation, Disposition, Option[Conclusion])] =
    Some((p.status, p.observation, p.interpretation, p.disposition, p.previous))

  // Status carries protocol-facing status, optional application status, and
  // the generated numeric semantic detail code derived from Conclusion.
  final class Status private (
    val webCode: WebCode,
    val appCode: Option[Long],
    val appStatus: Option[String],
    val detailCode: Option[DetailCode]
  ) {
    def toRecord: Record = Record.data(
      "webCode" -> webCode.code
    ) ++ Record.dataOption(
      "appCode" -> appCode,
      "appStatus" -> appStatus,
      "detailCode" -> detailCode.map(_.code)
    )

    def materialize(conclusion: Conclusion): Status =
      Status.materialized(
        webCode = Status.webCodeOf(conclusion),
        appCode = appCode,
        appStatus = appStatus,
        detailCode = Some(DetailCode.generated(conclusion))
      )

    def copy(
      appCode: Option[Long] = this.appCode,
      appStatus: Option[String] = this.appStatus
    ): Status =
      Status(appCode, appStatus)

    override def equals(rhs: Any): Boolean = rhs match {
      case that: Status =>
        webCode == that.webCode &&
          appCode == that.appCode &&
          appStatus == that.appStatus &&
          detailCode == that.detailCode
      case _ => false
    }

    override def hashCode: Int =
      (webCode, appCode, appStatus, detailCode).hashCode

    override def toString: String =
      s"Status($webCode,$appCode,$appStatus,$detailCode)"
  }
  object Status {
    def apply(
      appCode: Option[Long] = None,
      appStatus: Option[String] = None
    ): Status =
      new Status(WebCode.InternalServerError, appCode, appStatus, None)

    private def materialized(
      webCode: WebCode,
      appCode: Option[Long],
      appStatus: Option[String],
      detailCode: Option[DetailCode]
    ): Status =
      new Status(webCode, appCode, appStatus, detailCode)

    def unapply(p: Status): Option[(WebCode, Option[Long], Option[String], Option[DetailCode])] =
      Some((p.webCode, p.appCode, p.appStatus, p.detailCode))

    def webCodeOf(taxonomy: Taxonomy): WebCode =
      taxonomy.symptom match {
        case Taxonomy.Symptom.NotFound => WebCode.NotFound
        case Taxonomy.Symptom.AuthenticationRequired => WebCode.Unauthorized
        case Taxonomy.Symptom.PermissionDenied => WebCode.Forbidden
        case _ => _web_code_by_category(taxonomy)
      }

    private def _web_code_by_category(taxonomy: Taxonomy): WebCode =
      taxonomy.category match {
        case Taxonomy.Category.Argument => WebCode.BadRequest
        case Taxonomy.Category.Property => WebCode.BadRequest
        case Taxonomy.Category.Configuration => WebCode.InternalServerError
        case Taxonomy.Category.Resource => WebCode.InternalServerError
        case Taxonomy.Category.Reference => WebCode.ServiceUnavailable
        case Taxonomy.Category.State => WebCode.InternalServerError
        case Taxonomy.Category.Value => WebCode.BadRequest
        case Taxonomy.Category.Entity => WebCode.BadRequest
        case Taxonomy.Category.Security => WebCode.Forbidden
        case Taxonomy.Category.Record => WebCode.BadRequest
        case Taxonomy.Category.Operation => WebCode.BadRequest
        case Taxonomy.Category.Service => WebCode.ServiceUnavailable
        case Taxonomy.Category.Component => WebCode.InternalServerError
        case Taxonomy.Category.SubSystem => WebCode.InternalServerError
        case Taxonomy.Category.ServiceProvider => WebCode.ServiceUnavailable
        case Taxonomy.Category.System => WebCode.InternalServerError
        case Taxonomy.Category.DataStore => WebCode.InternalServerError
        case Taxonomy.Category.Network => WebCode.ServiceUnavailable
        case Taxonomy.Category.OutOfControl => WebCode.InternalServerError
      }

    def webCodeOf(conclusion: Conclusion): WebCode =
      if (conclusion.interpretation.kind == Interpretation.Kind.Success)
        WebCode.Ok
      else
        webCodeOf(conclusion.observation.taxonomy)
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
      status = Status(),
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
      status = Status(),
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
    Status.webCodeOf(o.taxonomy) match {
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
      Status(),
      o,
      Interpretation.domainFailure,
      Disposition.fix
    )

  def unauthorized(o: Observation): Conclusion =
    Conclusion(
      Status(),
      o,
      Interpretation.domainFailure,
      Disposition.fix
    )

  def forbidden(o: Observation): Conclusion =
    Conclusion(
      Status(),
      o,
      Interpretation.domainFailure,
      Disposition.fix
    )

  def notFound(o: Observation): Conclusion =
    Conclusion(
      Status(),
      o,
      Interpretation.domainFailure,
      Disposition.fix
    )

  def conflict(o: Observation): Conclusion =
    Conclusion(
      Status(),
      o,
      Interpretation.domainFailure,
      Disposition.fix
    )

  def internalServerError(o: Observation): Conclusion =
    Conclusion(
      Status(),
      o,
      Interpretation.systemFailure,
      Disposition.fix
    )

  def notImplemented(o: Observation): Conclusion =
    Conclusion(
      Status(),
      o,
      Interpretation.defect,
      Disposition.defect
    )

  def serviceUnavailable(o: Observation): Conclusion =
    Conclusion(
      Status(),
      o,
      Interpretation.systemFailure,
      Disposition.serviceUnavailable
    )

  def failure(pos: SourcePosition, taxonomy: Taxonomy, facets: Seq[Descriptor.Facet]): Conclusion =
    failure(pos, taxonomy, Cause.create(facets))

  def failure(pos: SourcePosition, taxonomy: Taxonomy, cause: Cause): Conclusion =
    Status.webCodeOf(taxonomy) match {
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
      Status(),
      Observation.notImplemented(pos, msg),
      Interpretation.notImplemented,
      Disposition.notImplemented
    )

  def resourceNotFound(
    resource: Descriptor.Facet.Resource,
    facets: Seq[Descriptor.Facet]
  ): Conclusion =
    Conclusion(
      Status(),
      Observation.resourceNotFound(resource, facets),
      Interpretation.resourceNotFound,
      Disposition.fix
    )

  def serviceProviderNotFound(name: String, facets: Seq[Descriptor.Facet]): Conclusion =
    Conclusion(
      Status(),
      Observation.serviceProviderNotFound(name, facets),
      Interpretation.configurationFailure,
      Disposition.serviceUnavailable
    )

  def securityAuthenticationRequired(message: String): Conclusion =
    Conclusion(
      Status(),
      Observation.securityAuthenticationRequired(message),
      Interpretation.domainFailure,
      Disposition.fix
    )

  def securityPermissionDenied(message: String): Conclusion =
    Conclusion(
      Status(),
      Observation.securityPermissionDenied(message),
      Interpretation.domainFailure,
      Disposition.fix
    )

  def securityPermissionDenied(message: String, facets: Seq[Descriptor.Facet]): Conclusion =
    Conclusion(
      Status(),
      Observation.securityPermissionDenied(message, facets),
      Interpretation.domainFailure,
      Disposition.fix
    )

  def entityNotFound(id: Identifier): Conclusion =
    Conclusion(
      Status(),
      Observation.entityNotFound(id),
      Interpretation.notFound,
      Disposition.fix
    )

  def argumentMissing: Conclusion =
    Conclusion(
      Status(),
      Observation.argumentMissing,
      Interpretation.argumentMissing,
      Disposition.argumentMissing
    )

  def argumentMissing(name: String): Conclusion =
    Conclusion(
      Status(),
      Observation.argumentMissing(name),
      Interpretation.argumentMissing,
      Disposition.argumentMissing
    )

  def argumentMissingInput(name: String): Conclusion =
    Conclusion(
      Status(),
      Observation.argumentMissingInput(name),
      Interpretation.argumentMissing,
      Disposition.argumentMissing
    )

  def argumentMissingInput(args: Seq[String]): Conclusion =
    Conclusion(
      Status(),
      Observation.argumentMissingInput(args),
      Interpretation.argumentMissing,
      Disposition.argumentMissing
    )

  def argumentMissingInput(req: HttpRequest): Conclusion =
    Conclusion(
      Status(),
      Observation.argumentMissingInput(req),
      Interpretation.argumentMissing,
      Disposition.argumentMissing
    )

  def argumentMissingOperation(name: String, operation: String): Conclusion =
    Conclusion(
      Status(),
      Observation.argumentMissingOperation(name, operation),
      Interpretation.argumentMissing,
      Disposition.argumentMissing
    )

  def argumentRedundantOperation(name: String, operation: String): Conclusion =
    Conclusion(
      Status(),
      Observation.argumentRedundantOperation(name, operation),
      Interpretation.argumentRedundant,
      Disposition.argumentRedundant
    )

  def argumentRedundantOperationInput(operation: String, args: Seq[String]): Conclusion =
    Conclusion(
      Status(),
      Observation.argumentRedundantOperationInput(operation, args),
      Interpretation.argumentRedundant,
      Disposition.argumentRedundant
    )

  def argumentDataType(name: String, value: Any, dt: DataType): Conclusion =
    Conclusion(
      Status(),
      Observation.argumentDataType(name, value, dt),
      Interpretation.argumentDataType,
      Disposition.argumentDataType
    )

  def argumentConstraint(name: String, value: Any, cs: NonEmptyVector[Constraint]): Conclusion =
    Conclusion(
      Status(),
      Observation.argumentConstraint(name, value, cs),
      Interpretation.argumentConstraint,
      Disposition.argumentConstraint
    )

  def operationInvalid(name: String): Conclusion =
    Conclusion(
      Status(),
      Observation.operationInvalid(name),
      Interpretation.operationInvalid,
      Disposition.operationInvalid
    )

  def resourceInconsistency(pos: SourcePosition): Conclusion =
    Conclusion(
      Status(),
      Observation.resourceInconsistency(pos),
      Interpretation.resourceInconsistency,
      Disposition.resourceInconsistency
    )

  def stateInvalid(
    message: String,
    facets: Seq[Descriptor.Facet] = Nil,
    previous: Option[Conclusion] = None
  ): Conclusion =
    Conclusion(
      Status(),
      Observation.stateInvalid(message, facets),
      Interpretation.stateInvalid,
      Disposition.stateInvalid,
      previous
    )

  def recordNotFound(pos: SourcePosition, key: String, rec: Record): Conclusion =
    Conclusion(
      Status(),
      Observation.recordNotFound(pos, key, rec),
      Interpretation.recordNotFound,
      Disposition.recordNotFound
    )

  def operationNotFound(pos: SourcePosition, name: String): Conclusion =
    Conclusion(
      Status(),
      Observation.operationNotFound(pos, name),
      Interpretation.operationNotFound,
      Disposition.operationNotFound
    )

  def valueInvalid(value: Any, dt: DataType): Conclusion =
    Conclusion(
      Status(),
      Observation.valueInvalid(value, dt),
      Interpretation.valueInvalid,
      Disposition.valueInvalid
    )

  def valueFormatError(value: Any, dt: DataType): Conclusion =
    Conclusion(
      Status(),
      Observation.valueFormatError(value, dt),
      Interpretation.valueFormatError,
      Disposition.valueFormatError
    )

  def unreachableReached(pos: SourcePosition): Conclusion =
    Conclusion(
      Status(),
      Observation.unreachableReached(pos),
      Interpretation.unreachableReached,
      Disposition.unreachableReached
    )

  def unreachableReached(msg: String): Conclusion =
    Conclusion(
      Status(),
      Observation.unreachableReached(msg),
      Interpretation.unreachableReached,
      Disposition.unreachableReached
    )

  def uninitializedState(pos: SourcePosition): Conclusion =
    Conclusion(
      Status(),
      Observation.uninitializedState(pos),
      Interpretation.uninitializedState,
      Disposition.uninitializedState
    )

  def impossibleState(msg: String): Conclusion =
    Conclusion(
      Status(),
      Observation.impossibleState(msg),
      Interpretation.impossibleState,
      Disposition.impossibleState
    )

  def unsupported(msg: String): Conclusion =
    Conclusion(
      Status(),
      Observation.unsupported(msg),
      Interpretation.unsupported,
      Disposition.unsupported
    )

  def notImplemented(pos: SourcePosition): Conclusion =
    Conclusion(
      Status(),
      Observation.notImplemented(pos),
      Interpretation.notImplemented,
      Disposition.notImplemented
    )

  def notImplemented(msg: String): Conclusion =
    Conclusion(
      Status(),
      Observation.notImplemented(msg),
      Interpretation.notImplemented,
      Disposition.notImplemented
    )

  def invariantViolation(msg: String): Conclusion =
    Conclusion(
      Status(),
      Observation.invariantViolation(msg),
      Interpretation.invariantViolation,
      Disposition.invariantViolation
    )

  def preconditionViolation(msg: String): Conclusion =
    Conclusion(
      Status(),
      Observation.preconditionViolation(msg),
      Interpretation.preconditionViolation,
      Disposition.preconditionViolation
    )

  def postconditionViolation(msg: String): Conclusion =
    Conclusion(
      Status(),
      Observation.postconditionViolation(msg),
      Interpretation.postconditionViolation,
      Disposition.postconditionViolation
    )

  // Deprecated compatibility aliases for the old fail* naming style.
  // New code should call semantic utilities such as argumentMissing,
  // operationInvalid, valueInvalid, recordNotFound, or notImplemented.
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failArgumentMissing: Conclusion = argumentMissing
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failArgumentMissing(name: String): Conclusion = argumentMissing(name)
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failArgumentMissingInput(name: String): Conclusion = argumentMissingInput(name)
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failArgumentMissingInput(args: Seq[String]): Conclusion = argumentMissingInput(args)
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failArgumentMissingInput(req: HttpRequest): Conclusion = argumentMissingInput(req)
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failArgumentMissingOperation(name: String, operation: String): Conclusion =
    argumentMissingOperation(name, operation)
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failArgumentRedundantOperation(name: String, operation: String): Conclusion =
    argumentRedundantOperation(name, operation)
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failArgumentRedundantOperationInput(operation: String, args: Seq[String]): Conclusion =
    argumentRedundantOperationInput(operation, args)
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failArgumentDataType(name: String, value: Any, dt: DataType): Conclusion =
    argumentDataType(name, value, dt)
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failArgumentConstraint(name: String, value: Any, cs: NonEmptyVector[Constraint]): Conclusion =
    argumentConstraint(name, value, cs)
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failOperationInvalid(name: String): Conclusion = operationInvalid(name)
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failResourceInconsistency(pos: SourcePosition): Conclusion = resourceInconsistency(pos)
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failRecordNotFound(pos: SourcePosition, key: String, rec: Record): Conclusion =
    recordNotFound(pos, key, rec)
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failOperationNotFound(pos: SourcePosition, name: String): Conclusion =
    operationNotFound(pos, name)
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failValueInvalid(value: Any, dt: DataType): Conclusion = valueInvalid(value, dt)
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failValueFormatError(value: Any, dt: DataType): Conclusion = valueFormatError(value, dt)
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failUnreachableReached(pos: SourcePosition): Conclusion = unreachableReached(pos)
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failUnreachableReached(msg: String): Conclusion = unreachableReached(msg)
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failUninitializedState(pos: SourcePosition): Conclusion = uninitializedState(pos)
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failImpossibleState(msg: String): Conclusion = impossibleState(msg)
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failUnsupported(msg: String): Conclusion = unsupported(msg)
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failNotImplemented(pos: SourcePosition): Conclusion = notImplemented(pos)
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failNotImplemented(msg: String): Conclusion = notImplemented(msg)
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failInvariantViolation(msg: String): Conclusion = invariantViolation(msg)
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failPreconditionViolation(msg: String): Conclusion = preconditionViolation(msg)
  @deprecated("Use semantic Conclusion utilities instead.", "Apr. 14, 2026")
  def failPostconditionViolation(msg: String): Conclusion = postconditionViolation(msg)
}
