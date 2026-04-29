package org.goldenport.provisional.observation

import cats.data.NonEmptyVector
import java.time.Instant
import org.goldenport.record.Record
import org.goldenport.text.Presentable
import org.goldenport.datatype.Identifier
import org.goldenport.id.UniversalId
import org.goldenport.http.HttpRequest
import org.goldenport.observation.Phenomenon
import org.goldenport.observation.{TraceId, SpanId}
import org.goldenport.observation.{Subject, Agent, Resource}
import org.goldenport.observation.Severity
import org.goldenport.observation.Descriptor
import org.goldenport.observation.SourcePosition
import org.goldenport.schema.DataType
import org.goldenport.schema.Constraint
import org.goldenport.observation.calltree.CallTree
import org.goldenport.util.SmEnum

/**
 * Unified Observation (Phase 2.9 Design-Fixed Model)
 *
 * Observation represents a factual record of something that was observed.
 * It MUST be interpretation-free, behavior-free, and normalization-free.
 *
 * This model intentionally separates:
 *   - what was factually wrong (Taxonomy)
 *   - how it failed (Cause)
 *   - where the trigger originated (Source)
 *   - how it was accessed or communicated (Channel)
 *   - where it surfaced in the execution stack (Substrate)
 *   - where it was observed in code (Origin)
 *
 * Observability-related fields (timestamp, traceId, spanId, subject, agent, object,
 * environment, etc.) provide contextual facts only and MUST NOT imply responsibility,
 * handling strategy, or disposition.
 *
 * This definition stabilizes semantics during Phase 2.9 and serves as the
 * authoritative reference for Conclusion / Consequence modeling.
 */
/*
 * @since   Jul. 19, 2025
 *  version Jul. 20, 2025
 *  version Dec. 30, 2025
 *  version Jan. 31, 2026
 *  version Feb. 25, 2026
 *  version Mar. 13, 2026
 *  version Apr. 14, 2026
 * @version Apr. 29, 2026
 * @author  ASAMI, Tomoharu
 */
case class Observation(
  phenomenon: Phenomenon,
  taxonomy: Taxonomy,
  cause: Cause,

  // --- Observability core ---
  timestamp: Instant,

  assessment: Assessment = Assessment.empty,
  occurrence: Option[Occurrence] = None,
  involvement: Option[Involvement] = None,

  // --- Observability extensions ---
  origin: Option[Origin] = None,
  traceId: Option[TraceId] = None,
  spanId: Option[SpanId] = None,
  environment: Option[Environment] = None,
  callTree: Option[CallTree] = None,

  // message: Option[String] = None, // I18nMessage
  // exception: Option[Throwable] = None,
  properties: Map[String, String] = Map.empty,

  observationId: Option[ObservationId] = None
) extends Presentable {
  def severity: Option[Severity] = assessment.severity
  def exception: Option[Throwable] = cause.getException
  def getMessage: Option[String] = cause.getMessage
  def getEffectiveMessage: Option[String] = cause.getEffectiveMessage

  def print = s"${taxonomy.print}[${cause.print}]"
  override def show = s"${taxonomy.show}[${cause.show}]"

  def isMatch(rhs: Observation): Boolean = (
    phenomenon == rhs.phenomenon &&
      taxonomy == rhs.taxonomy &&
      cause == rhs.cause &&
      assessment == rhs.assessment &&
      occurrence == rhs.occurrence &&
      involvement == rhs.involvement &&
      origin == rhs.origin &&
      traceId == rhs.traceId &&
      spanId == rhs.spanId &&
      environment == rhs.environment &&
      properties == rhs.properties &&
      observationId == rhs.observationId
  )

  def withSeverity(p: Option[Severity]): Observation = p.fold(this)(withSeverity)
  def withSeverity(p: Severity): Observation = copy(assessment = assessment.withSeverity(p))

  def withSourcePosition(p: SourcePosition): Observation = copy(cause = cause.withSourcePosition(p))

  def toCategoryArgument = copy(taxonomy = taxonomy.toCategoryArgument)

  def toRecord: Record = Record.data(
    "phenomenon" -> phenomenon.name,
    "taxonomy" -> taxonomy.toRecord,
    "cause" -> cause.toRecord,
    "timestamp" -> timestamp,
    "assessment" -> assessment.toRecord,
    "properties" -> Record.create(properties)
  ) ++ Record.dataOption(
    "occurrence" -> occurrence.map(_.toRecord),
    "involvement" -> involvement.map(_.toRecord),
    "origin" -> origin.map(_.toRecord),
    "trace-id" -> traceId.map(_.print),
    "span-id" -> spanId.map(_.print),
    "environment" -> environment.map(_.toRecord),
    "call-tree" -> callTree.map(_.toRecord),
    "observation-id" -> observationId.map(_.print)
  )
}

object Observation {
  // Argument
  def argumentMissing: Observation = rejection(
    Taxonomy.argumentMissing
  )

  def argumentMissing(name: String): Observation = rejection(
    Taxonomy.argumentMissing,
    Descriptor.Facet.Parameter.argument(name)
  )

  def argumentMissingInput(name: String): Observation = rejection(
    Taxonomy.argumentMissing,
    Descriptor.Facet.Input(name = Some(name))
  )

  def argumentMissingInput(args: Seq[String]): Observation = rejection(
    Taxonomy.argumentMissing,
    Descriptor.Facet.Args(args)
  )

  def argumentMissingInput(req: HttpRequest): Observation = rejection(
    Taxonomy.argumentMissing,
    Descriptor.Facet.Input(value = Some(req.toString))
  )

  def argumentMissingOperation(name: String, operation: String): Observation = rejection(
    Taxonomy.argumentMissing,
    Descriptor.argumentOperation(name, operation)
  )

  def argumentRedundant(name: String): Observation = rejection(
    Taxonomy.argumentRedundant,
    Descriptor.Facet.Parameter.argument(name)
  )

  def argumentRedundantOperation(name: String, operation: String): Observation = rejection(
    Taxonomy.argumentRedundant,
    Descriptor.argumentOperation(name, operation)
  )

  def argumentRedundantOperationInput(
    operation: String,
    input: Seq[String]
  ): Observation = rejection(
    Taxonomy.argumentRedundant,
    Descriptor.operationInput(operation, input)
  )

  def argumentInvalid(name: String): Observation = rejection(
    Taxonomy.argumentInvalid,
    Descriptor.Facet.Parameter.argument(name)
  )

  def argumentInvalid(name: String, message: String): Observation = rejection(
    Taxonomy.argumentInvalid,
    Descriptor.Facet.Parameter.argument(name),
    Descriptor.Facet.Message(message)
  )

  // Format / Syntax
  def argumentFormatError(message: String): Observation = rejection(
    Taxonomy.argumentFormatError,
    Descriptor.Facet.Message(message)
  )

  def argumentSyntaxError(message: String): Observation = rejection(
    Taxonomy.argumentSyntaxError,
    Descriptor.Facet.Message(message)
  )

  def argumentDataType(name: String, value: Any, dt: DataType): Observation = rejection(
    Taxonomy.argumentDataType,
    Descriptor.Facet.Parameter.argument(name),
    Descriptor.Facet.Value(value),
    Descriptor.Facet.DataType(dt)
  )

  def argumentConstraint(name: String, value: Any, cs: NonEmptyVector[Constraint]): Observation = rejection(
    Taxonomy.argumentDataType,
    Descriptor.Facet.Parameter.argument(name),
    Descriptor.Facet.Value(value),
    Descriptor.Facet.Constraint(cs)
  )

  // Operation
  def operationInvalid(name: String): Observation = rejection(
    Taxonomy.operationInvalid,
    Descriptor.Facet.Operation(name)
  )

  // Data
  def valueInvalid(value: Any, datatype: DataType): Observation = rejection(
    Taxonomy.valueInvalid,
    Descriptor.Facet.DataType(datatype)
  )

  def valueFormatError(value: Any, datatype: DataType): Observation = rejection(
    Taxonomy.valueFormatError,
    Descriptor.Facet.DataType(datatype)
  )

  // Property
  def propertyNotFound(pos: SourcePosition, key: String): Observation = failure(
    Taxonomy.propertyNotFound,
    Descriptor.Facet.Key(key)
  )

  // Resource
  def resourceNotFound(
    resource: Descriptor.Facet.Resource,
    facets: Seq[Descriptor.Facet]
  ): Observation = rejection(
    Taxonomy.resourceNotFound,
    resource +: facets
  )

  def resourceNotFound(id: String): Observation = rejection( // TODO id
    Taxonomy.resourceNotFound,
    Descriptor.Facet.Id(id)
  )

  def resourceSyntaxError(message: String): Observation = failure(
    Taxonomy.resourceSyntaxError,
    Descriptor.Facet.Message(message)
  )

  def resourceInconsistency(pos: SourcePosition): Observation = failure(
    Taxonomy.resourceInvalid,
    Descriptor.Facet.SrcPos(pos)
  )

  // Reference
  def referenceNotFound(id: UniversalId): Observation = rejection(
    Taxonomy.referenceNotFound,
    Descriptor.Facet.Id(id)
  )

  // Record
  def recordNotFound(pos: SourcePosition, key: String, rec: Record): Observation = failure(
    Taxonomy.recordNotFound,
    Descriptor.Facet.SrcPos(pos),
    Descriptor.Facet.Key(key),
    Descriptor.Facet.RecordData(rec)
  )

  // Operation
  def operationNotFound(pos: SourcePosition, name: String): Observation = failure(
    Taxonomy.operationNotFound,
    Descriptor.Facet.Operation(name)
  )

  // Security
  def securityAuthenticationRequired(message: String): Observation = rejection(
    Taxonomy.securityAuthenticationRequired,
    Descriptor.Facet.Message(message)
  )

  def securityPermissionDenied(message: String): Observation = rejection(
    Taxonomy.securityPermissionDenied,
    Descriptor.Facet.Message(message)
  )

  def securityPermissionDenied(
    message: String,
    facets: Seq[Descriptor.Facet]
  ): Observation = rejection(
    Taxonomy.securityPermissionDenied,
    Descriptor.Facet.Message(message) +: facets
  )

  // State
  def stateConflict(state: String): Observation = failure(
    Taxonomy.stateConflict,
    Descriptor.Facet.State(state)
  )

  // ServiceProvider
  def serviceProviderNotFound(name: String): Observation = failure(
    Taxonomy.serviceProviderNotFound,
    Descriptor.Facet.Name(name)
  )

  def serviceProviderNotFound(name: String, facets: Seq[Descriptor.Facet]): Observation = failure(
    Taxonomy.serviceProviderNotFound,
    Descriptor.Facet.Name(name) +: facets
  )

  def entityNotFound(id: Identifier): Observation = failure(
    Taxonomy.entityNotFound,
    Descriptor.Facet.Id(id)
  )

  // OutOfControl
  def unreachableReached(pos: SourcePosition): Observation = failure(
    Taxonomy.unreachableReached,
    Descriptor.Facet.SrcPos(pos)
  )

  def unreachableReached(msg: String): Observation = failure(Taxonomy.unreachableReached)

  def uninitializedState(pos: SourcePosition): Observation = failure(
    Taxonomy.uninitializedState,
    Descriptor.Facet.SrcPos(pos)
  )

  def impossibleState(msg: String): Observation = failure(Taxonomy.impossibleState)

  def unsupported(msg: String): Observation = failure(Taxonomy.unsupported)

  def notImplemented(pos: SourcePosition): Observation = failure(
    Taxonomy.notImplemented,
    Descriptor.Facet.SrcPos(pos)
  )

  def notImplemented(pos: SourcePosition, msg: String): Observation = failure(
    Taxonomy.notImplemented,
    Descriptor.Facet.SrcPos(pos),
    Descriptor.Facet.Message(msg)
  )

  def notImplemented(msg: String): Observation = failure(Taxonomy.notImplemented)

  def invariantViolation(msg: String): Observation = failure(Taxonomy.invariantViolation)

  def preconditionViolation(msg: String): Observation = failure(Taxonomy.preconditionViolation)

  def postconditionViolation(msg: String): Observation = failure(Taxonomy.postconditionViolation)

  def ofcNullPointer: Observation = failure(Taxonomy.ofcNullPointer)

  def rejection(
    t: Taxonomy,
    facet: Descriptor.Facet,
    facets: Descriptor.Facet*
  ): Observation =
    rejection(t, Cause.create(facet, facets))

  def rejection(
    t: Taxonomy,
    facets: Seq[Descriptor.Facet]
  ): Observation =
    rejection(t, Cause.create(facets))

  def rejection(
    t: Taxonomy,
    descriptor: Descriptor
  ): Observation =
    rejection(t, Cause(None, descriptor))

  def rejection(t: Taxonomy, c: Cause): Observation = Observation(
    Phenomenon.Rejection, t, c, Instant.now()
  )

  def rejection(t: Taxonomy, c: Cause, o: Option[Occurrence]): Observation = Observation(
    Phenomenon.Rejection, t, c, Instant.now(),
    occurrence = o
  )

  def rejection(t: Taxonomy): Observation = Observation(
    Phenomenon.Rejection, t, Cause.empty, Instant.now()
  )

  def failure(t: Taxonomy): Observation = failure(t, Cause.empty)

  def failure(
    t: Taxonomy,
    facet: Descriptor.Facet,
    facets: Descriptor.Facet*
  ): Observation =
    failure(t, Cause.create(facet, facets))

  def failure(
    t: Taxonomy,
    facets: Seq[Descriptor.Facet]
  ): Observation =
    failure(t, Cause.create(facets))

  def failure(t: Taxonomy, c: Cause): Observation = Observation(
    Phenomenon.Failure,
    t,
    c,
    Instant.now(),
    Assessment.failure
  )

  def failure(t: Taxonomy, c: Cause, o: Option[Occurrence]): Observation = Observation(
    Phenomenon.Failure,
    t,
    c,
    Instant.now(),
    occurrence = o
  )
}

/**
 * Taxonomy classifies what is factually wrong.
 *
 * It consists of two independent axes:
 *   - Category: the domain in which the problem is observed
 *   - Symptom: the observable form of the problem
 *
 * Taxonomy MUST NOT encode interpretation, responsibility, severity,
 * or handling decisions.
 */
case class Taxonomy(
  category: Taxonomy.Category,
  symptom: Taxonomy.Symptom
) extends Presentable {
  def print = s"$category.$symptom"

  def toCategoryArgument = copy(category = Taxonomy.Category.Argument)

  def toRecord: Record = Record.data(
    "category" -> category.name,
    "symptom" -> symptom.name
  )
}
object Taxonomy {
  enum Category(val name: String, val value: Int) {
    /** The problem is observed in input arguments explicitly provided to an operation or function.
      * This category is used when the observed issue concerns values supplied by the caller at invocation time.
      * It does not imply caller responsibility or fault.
      */
    case Argument extends Category("argument", 1)

    /** The problem is observed in properties or attributes of a domain object or entity.
      * This category is used when the issue concerns stored or derived attribute values, not direct input arguments.
      */
    case Property extends Category("property", 2)

    /** The problem is observed in runtime or environment configuration.
      * This category is used for issues related to configuration values, settings, or parameters that control system behavior.
      */
    case Configuration extends Category("configuration", 3)

    /** The problem is observed in a resource or reference to a resource.
      * This category covers existence, availability, integrity, and reference-related issues of internal or external resources.
      */
    case Resource extends Category("resource", 4)

    case Reference extends Category("reference", 12)

    /** The problem is observed in the state of a system, component, or domain entity.
      * This category is used when the issue concerns invalid or prohibited states or state transitions.
      */
    case State extends Category("state", 5)

    case Value extends Category("value", 7)

    case Entity extends Category("entity", 17)

    case Security extends Category("security", 18)

    case Record extends Category("value", 8)

    /** The problem is observed at the system level and cannot be meaningfully attributed to argument, property, configuration, resource, or state.
      * This category is used as a last resort for fundamental or infrastructural failures.
      */
    case System extends Category("system", 6)

    case DataStore extends Category("datastore", 11)

    case Network extends Category("operation", 10)

    case OutOfControl extends Category("out-of-control", 8)

    case Operation extends Category("operation", 9)

    case Service extends Category("service", 13)

    case Component extends Category("component", 14)

    case SubSystem extends Category("subsytem", 15)

    case ServiceProvider extends Category("service-provider", 16)
  }

  enum Symptom(val name: String, val value: Int) {
    /** The observed data or structure does not conform to the required syntactic rules and cannot be parsed as intended. */
    case SyntaxError extends Symptom("syntax-error", 1)
    /** The observed data is syntactically valid but does not match the expected format or representation. */
    case FormatError extends Symptom("format-error", 2)
    /** The observed value violates domain-level constraints or rules, despite being syntactically and structurally valid. */
    case DomainValue extends Symptom("domain-value", 3) // unused, use Invalid instead.
    /** A required value or element is not provided or not present where it is expected to exist. */
    case Missing extends Symptom("missing", 4)
    /** ??? */
    case Redundant extends Symptom("missing", 14) // TODO
    /** An observed value, element, or condition appears that is not expected in the given context. */
    case Unexpected extends Symptom("unexpected", 5)
    /** The observed value, operation, or condition is explicitly not supported by the system or component. */
    case Unsupported extends Symptom("unsupported", 6)
    /** An attempt to locate or retrieve a target confirms that the target does not exist. */
    case NotFound extends Symptom("not-found", 7)
    /** The target may exist, but its existence or accessibility cannot be confirmed due to reachability or availability issues. */
    case Unavailable extends Symptom("unavailable", 8)
    /** The observed condition represents a conflict, such as a concurrency conflict or a violation of consistency constraints. */
    case Conflict extends Symptom("conflict", 9)
    /** A reference points to a target that is invalid, inconsistent, or cannot be resolved as a valid reference. */
    case InvalidReference extends Symptom("invalid-reference", 10)
    /** The observed operation or state is explicitly prohibited by defined rules or constraints. */
    case Illegal extends Symptom("illegal", 11)
    /** The observed value or state does not satisfy the conditions required to be considered valid or well-formed. */
    case Invalid extends Symptom("invalid", 12)
    /** The observed data or resource is damaged, inconsistent, or internally broken in a way that prevents correct use. */
    case Corrupted extends Symptom("corrupted", 13)
    case Duplicate extends Symptom("duplicate", 22)
    case AuthenticationRequired extends Symptom("authentication-required", 23)
    case PermissionDenied extends Symptom("permission-denied", 24)

    // OutOfControl
    case UnreachableReached extends Symptom("unreachable-reached", 14)

    case UninitializedState extends Symptom("uninitialized-state", 16)

    case ImpossibleState extends Symptom("impossible-state", 21)

    case NotImplemented extends Symptom("not-implemented", 20)

    case InvariantViolation extends Symptom("invariant-violation", 17)
    case PreconditionViolation extends Symptom("precondition-violation", 18)
    case PostconditionViolation extends Symptom("postcondition-violation", 19)

    case NullPointer extends Symptom("null-pointer", 15)
  }

  // Argument
  val argumentMissing: Taxonomy = Taxonomy(
    Category.Argument,
    Symptom.Missing
  )
  val argumentRedundant: Taxonomy = Taxonomy(
    Category.Argument,
    Symptom.Redundant
  )
  val argumentInvalid: Taxonomy = Taxonomy(
    Category.Argument,
    Symptom.Invalid
  )
  val argumentDataType: Taxonomy = Taxonomy(
    Category.Argument,
    Symptom.Invalid
  )
  val argumentConstraint: Taxonomy = Taxonomy(
    Category.Argument,
    Symptom.Invalid
  )
  val argumentFormatError: Taxonomy = Taxonomy(
    Category.Argument,
    Symptom.FormatError
  )
  val argumentSyntaxError: Taxonomy = Taxonomy(
    Category.Argument,
    Symptom.FormatError
  )

  // Property
  val propertyNotFound: Taxonomy = Taxonomy(
    Category.Property,
    Symptom.NotFound
  )

  // Operation
  val operationInvalid: Taxonomy = Taxonomy(
    Category.Operation,
    Symptom.Invalid
  )

  val operationNotFound: Taxonomy = Taxonomy(
    Category.Operation,
    Symptom.NotFound
  )

  // Security
  val securityAuthenticationRequired: Taxonomy = Taxonomy(
    Category.Security,
    Symptom.AuthenticationRequired
  )

  val securityPermissionDenied: Taxonomy = Taxonomy(
    Category.Security,
    Symptom.PermissionDenied
  )

  // Resource
  val resourceNotFound: Taxonomy = Taxonomy(
    Category.Resource,
    Symptom.NotFound
  )
  val resourceSyntaxError: Taxonomy = Taxonomy(
    Category.Resource,
    Symptom.SyntaxError
  )
  val resourceInvalid: Taxonomy = Taxonomy(
    Category.Resource,
    Symptom.Invalid
  )

  // Record
  val recordNotFound: Taxonomy = Taxonomy(
    Category.Record,
    Symptom.NotFound
  )

  // Component / Repository class-loading
  val componentUnavailable: Taxonomy = Taxonomy(
    Category.Component,
    Symptom.Unavailable
  )
  val componentInvalid: Taxonomy = Taxonomy(
    Category.Component,
    Symptom.Invalid
  )
  val componentCorrupted: Taxonomy = Taxonomy(
    Category.Component,
    Symptom.Corrupted
  )

  // Service
  val serviceUnavailable: Taxonomy = Taxonomy(
    Category.Service,
    Symptom.Unavailable
  )

  // State
  val stateConflict: Taxonomy = Taxonomy(
    Category.State,
    Symptom.Conflict
  )

  // ServiceProvider
  val serviceProviderNotFound: Taxonomy = Taxonomy(
    Category.ServiceProvider,
    Symptom.NotFound
  )

  // Value
  val valueInvalid: Taxonomy = Taxonomy(
    Category.Value,
    Symptom.Invalid
  )

  val valueFormatError: Taxonomy = Taxonomy(
    Category.Value,
    Symptom.FormatError
  )

  // DataStore
  val dataStoreNotFound: Taxonomy = Taxonomy(
    Category.DataStore,
    Symptom.NotFound
  )

  val dataStoreUnavailable: Taxonomy = Taxonomy(
    Category.DataStore,
    Symptom.Unavailable
  )

  val dataStoreDuplicate: Taxonomy = Taxonomy(
    Category.DataStore,
    Symptom.Duplicate
  )

  // Netowrk
  val networkUnavailable: Taxonomy = Taxonomy(
    Category.Network,
    Symptom.Unavailable
  )

  // Reference
  val referenceNotFound: Taxonomy = Taxonomy(
    Category.Reference,
    Symptom.NotFound
  )

  // Entity
  val entityNotFound: Taxonomy = Taxonomy(
    Category.Entity,
    Symptom.NotFound
  )

  // Out of Control
  val unreachableReached: Taxonomy = Taxonomy(
    Category.OutOfControl,
    Symptom.UnreachableReached
  )

  val uninitializedState: Taxonomy = Taxonomy(
    Category.OutOfControl,
    Symptom.UninitializedState
  )

  val impossibleState: Taxonomy = Taxonomy(
    Category.OutOfControl,
    Symptom.ImpossibleState
  )

  val unsupported: Taxonomy = Taxonomy(
    Category.OutOfControl,
    Symptom.Unsupported
  )

  val notImplemented: Taxonomy = Taxonomy(
    Category.OutOfControl,
    Symptom.NotImplemented
  )

  val invariantViolation: Taxonomy = Taxonomy(
    Category.OutOfControl,
    Symptom.InvariantViolation
  )

  val preconditionViolation: Taxonomy = Taxonomy(
    Category.OutOfControl,
    Symptom.PreconditionViolation
  )

  val postconditionViolation: Taxonomy = Taxonomy(
    Category.OutOfControl,
    Symptom.PostconditionViolation
  )

  val ofcNullPointer: Taxonomy = Taxonomy(
    Category.OutOfControl,
    Symptom.NullPointer
  )

  def from(p: Throwable): Taxonomy =
    Taxonomy(Taxonomy.Category.System, Taxonomy.Symptom.Corrupted) // TODO
}

case class Cause(
  kind: Option[Cause.Kind] = None,
//  detail: Option[Cause.Detail] = None
  descriptor: Descriptor = Descriptor.empty
) extends Presentable {
  // def addFacet(p: Descriptor.Facet) = detail match {
  //   case None => copy(detail = Some(Cause.Detail(p)))
  //   case Some(s) => copy(detail = Some(s.addFacet(p)))
  // }

  def print = s"${descriptor.display}"
  override def show = s"${descriptor.show}"

  def addFacet(p: Descriptor.Facet): Cause = copy(descriptor = descriptor.add(p))

  def withMessage(p: Option[String]): Cause = p.fold(this)(withMessage)

  def withMessage(p: String): Cause = addFacet(Descriptor.Facet.Message(p))

  def withException(p: Option[Throwable]): Cause = p.fold(this)(withException)

  def withException(p: Throwable): Cause = addFacet(Descriptor.Facet.Exception(p))

  def withSourcePosition(p: SourcePosition): Cause = addFacet(Descriptor.Facet.SrcPos(p))

  def getMessage: Option[String] = descriptor.getMessage
  def getEffectiveMessage: Option[String] = descriptor.getEffectiveMessage
  def getException: Option[Throwable] = descriptor.getException
  def getDataType: Option[DataType] = descriptor.getDataType
  def getConstraints: Option[NonEmptyVector[Constraint]] = descriptor.getConstraints

  def toRecord: Record = Record.dataOption(
    "kind" -> kind.map(_.name)
  ) ++ descriptor.toRecord
}
object Cause {
  val empty = Cause()

  enum Kind(val name: String, val value: Int) {
    case Limit extends Kind("limit", 1)
    case Exhaustion extends Kind("exhaustion", 2)
    case Timeout extends Kind("timeout", 3)
    case Conflict extends Kind("conflict", 4)
    case Inconsistency extends Kind("inconsistency", 5)
    case Corruption extends Kind("corruption", 6)
    case Format extends Kind("format", 7)
    case Policy extends Kind("policy", 8)
    case Capability extends Kind("capability", 9)
    case Permission extends Kind("permission", 10)
    case Guard extends Kind("guard", 11)
    case Relation extends Kind("relation", 12)
    case Unknown extends Kind("unknown", 99)
  }

  // enum Kind(val name: String, val value: Int) {
  //   case Parse extends Kind("parse", 1)
  //   case Validate extends Kind("validate", 2)
  //   case Lookup extends Kind("lookup", 3)
  //   case Resolve extends Kind("resolve", 4)
  //   case Execute extends Kind("execute", 5)
  //   case Communicate extends Kind("communicate", 6)
  //   case Timeout extends Kind("timeout", 7)
  //   case Conflict extends Kind("conflict", 8)
  //   case Unknown extends Kind("unknown", 9)
  // }

  /**
   * Optional, non-authoritative detail about the failure mechanism.
   * This information is descriptive only and must not affect interpretation.
   */
  case class Detail(
    descriptor: Descriptor = Descriptor.empty
  ) {
    def addFacet(p: Descriptor.Facet) = copy(descriptor = descriptor.add(p))
  }
  object Detail {
    val empty = Detail()

    def apply(p: Descriptor.Facet, ps: Descriptor.Facet*): Detail =
      Detail(Descriptor(p, ps))

    def createMessage(msg: Option[String]): Detail =
      msg.fold(empty)(x => Detail(Descriptor.Facet.Message(x)))
  }

  // private def withDetail(kind: Cause.Kind, message: Option[String] = None): Cause =
  //   Cause(kind, message.map(m => Cause.Detail(message = Some(m))))

  // val argumentInvalid: Cause = Cause(Kind.Validate)
  // val argumentMissing: Cause = Cause(Kind.Validate)
  // val argumentRedundant: Cause = Cause(Kind.Validate)
  // val argumentValidationError: Cause = Cause(Kind.Validate)
  // val syntaxError: Cause = Cause(Kind.Parse)
  // val formatError: Cause = Cause(Kind.Validate)
  // val valueDomainError: Cause = Cause(Kind.Validate)
  // val resourceNotFound: Cause = Cause(Kind.Lookup)
  // val stateConflict: Cause = Cause(Kind.Conflict)

  // def from(p: Taxonomy): Cause = p.symptom match {
  //   case Taxonomy.Symptom.SyntaxError => Cause(Kind.Parse)
  //   case Taxonomy.Symptom.FormatError => Cause(Kind.Validate)
  //   case Taxonomy.Symptom.DomainValue => Cause(Kind.Validate)
  //   case Taxonomy.Symptom.Missing => Cause(Kind.Validate)
  //   case Taxonomy.Symptom.Redundant => Cause(Kind.Validate)
  //   case Taxonomy.Symptom.Unexpected => ???
  //   case Taxonomy.Symptom.NotFound => Cause(Kind.Lookup)
  //   case Taxonomy.Symptom.Unavailable => ???
  //   case Taxonomy.Symptom.Conflict => ???
  //   case Taxonomy.Symptom.InvalidReference => ???
  //   case Taxonomy.Symptom.Illegal => ???
  //   case Taxonomy.Symptom.Invalid => Cause(Kind.Validate)
  //   case Taxonomy.Symptom.Corrupted => ???
  // }

  // // Argument
  // def argumentMissing(name: String): Cause = create(???, ???, ???)
  // def argumentRedundant(name: String): Cause = create(???, ???, ???)
  // def argumentInvalid(name: String, message: String): Cause = create(???, ???, ???)

  // // Format / Syntax
  // def formatError(message: String): Cause = create(???, ???, ???)
  // def syntaxError(message: String): Cause = create(???, ???, ???)

  // // Resource
  // def resourceNotFound(id: String): Cause = create(???, ???, ???)

  // // State
  // def stateConflict(state: String): Cause = create(???, ???, ???)

  // def create(t: Cause, c: Cause, msg: String): Cause = {
  //   ???
  // }

  // def createWithMessage(kind: Cause.Kind, message: Option[String]): Cause =
  //   Cause(kind, Some(Cause.Detail.createMessage(message)))

  def apply(p: Descriptor.Facet, ps: Descriptor.Facet*): Cause =
    Cause(Descriptor(p, ps))

  def apply(p: Descriptor): Cause = Cause(None, p)

  def from(p: Throwable): Cause = Cause(Descriptor.from(p))

  def create(p: Descriptor.Facet, ps: Seq[Descriptor.Facet]): Cause =
    Cause(Descriptor(p, ps))

  def create(ps: Seq[Descriptor.Facet]): Cause = ps.toList match {
    case Nil => Cause.empty
    case x :: xs => Cause(Descriptor(x, xs))
  }

  def message(msg: String): Cause =
    Cause(Descriptor.message(msg))

  def message(msg: Option[String]): Cause =
    msg.fold(Cause.empty)(x => Cause(Descriptor.message(x)))
}

case class Assessment(
  severity: Option[Severity] = None,
  nature: Option[Nature] = None
) {
  def withSeverity(p: Severity) = copy(severity = Some(p))

  def toRecord: Record = Record.dataOption(
    "severity" -> severity.map(_.value),
    "nature" -> nature.map(_.value)
  )
}
object Assessment {
  val empty = Assessment()
  val failure = Assessment(Some(Severity.Error), Some(Nature.Fault))
}

enum Nature extends SmEnum {
  case Fault, Defect, Anomaly
}

case class Occurrence(
  source: Source,
  channel: Channel,
  substrate: Substrate
) {
  def toRecord: Record = Record.data(
    "source" -> source.toRecord,
    "channel" -> channel.toRecord,
    "substrate" -> substrate.toRecord
  )
}
object Occurrence {
  val network = Occurrence(
    Source.externalSystem,
    Channel.network,
    Substrate.network
  )
}

case class Involvement(
  subject: Subject,
  agent: Agent,
  `object`: Resource
) {
  def toRecord: Record = Record.data(
    "subject" -> subject.value,
    "agent" -> agent.toRecord,
    "object" -> `object`.toRecord
  )
}

/**
 * Interaction represents a concrete form of interaction or access
 * (e.g. HTTP, database access, filesystem access).
 *
 * This model is shared across multiple observation axes (Source, Channel),
 * where its semantic meaning is determined by the enclosing axis.
 */
sealed abstract class Interaction {
  def toRecord: Record
}

object Interaction {

  /** HTTP-based interaction. */
  case class Http(
    method: Option[String],
    url: String
  ) extends Interaction {
    def toRecord: Record = Record.dataOption(
      "kind" -> Some("http"),
      "method" -> method,
      "url" -> Some(url)
    )
  }

  /** Database interaction (SQL or driver-based). */
  case class Database(
    operation: Option[String],
    target: Option[String]
  ) extends Interaction {
    def toRecord: Record = Record.dataOption(
      "kind" -> Some("database"),
      "operation" -> operation,
      "target" -> target
    )
  }

  /** Filesystem interaction. */
  case class FileSystem(
    operation: Option[String],
    path: String
  ) extends Interaction {
    def toRecord: Record = Record.dataOption(
      "kind" -> Some("file-system"),
      "operation" -> operation,
      "path" -> Some(path)
    )
  }

  /** Messaging or event-based interaction. */
  case class Messaging(
    system: String,
    operation: Option[String]
  ) extends Interaction {
    def toRecord: Record = Record.dataOption(
      "kind" -> Some("messaging"),
      "system" -> Some(system),
      "operation" -> operation
    )
  }

  /**
   * Opaque interaction for cases that do not fit predefined structures.
   * This preserves factual information without forcing premature modeling.
   */
  case class Opaque(
    description: String,
    properties: Map[String, String] = Map.empty
  ) extends Interaction {
    def toRecord: Record = Record.dataOption(
      "kind" -> Some("opaque"),
      "description" -> Some(description),
      "properties" -> Some(Record.create(properties))
    )
  }
}

/**
 * Source classifies where the triggering interaction originated.
 *
 * This axis describes the origin domain of input data or interaction,
 * independent of channel, substrate, origin, or responsibility.
 */
case class Source(
  kind: Source.Kind,
  detail: Option[Source.Detail] = None
) {
  def toRecord: Record = Record.dataAuto(
    "kind" -> kind.name,
    "detail" -> detail.map(_.toRecord)
  )
}
object Source {
  enum Kind(val name: String, val value: Int) {

    case UserInput extends Kind("user-input", 1)
    case ExternalSystem extends Kind("external-system", 2)
    case Event extends Kind("event", 3)
    case Message extends Kind("message", 4)
    case InMemory extends Kind("in-memory", 5)
    case Database extends Kind("database", 6)
    case Filesystem extends Kind("filesystem", 7)
    case Unknown extends Kind("unknown", 8)
  }

  sealed abstract class Detail {
    def toRecord: Record
  }
  object Detail {
    /**
      * Detail indicating that this source involves a concrete interaction.
      *
      * The semantic meaning of the interaction is defined by Source.Kind
      * (i.e. what triggered the observation), not by the interaction itself.
      */
    case class InteractionDetail(
      interaction: Interaction
    ) extends Detail {
      def toRecord: Record = Record.data(
        "interaction" -> interaction.toRecord
      )
    }
  }

  val externalSystem = Source(Kind.ExternalSystem)
  val inMemory = Source(Kind.InMemory)
}

/**
 * Channel classifies how the interaction or access was performed.
 *
 * This axis describes the access or communication mechanism itself,
 * regardless of source, substrate, origin, or interpretation.
 */
case class Channel(
  kind: Channel.Kind,
  detail: Option[Channel.Detail] = None
) {
  def toRecord: Record = Record.dataAuto(
    "kind" -> kind.name,
    "detail" -> detail.map(_.toRecord)
  )
}

object Channel {

  /**
   * Kind is a coarse-grained classification of access or communication style.
   * This enum is intentionally small and stable.
   */
  enum Kind(val name: String, val value: Int) {

    case InProcess extends Kind("in-process", 1)
    case Network extends Kind("network", 2)
    case Storage extends Kind("storage", 3)
    case Messaging extends Kind("messaging", 4)
    case Command extends Kind("command", 5)
    case Unknown extends Kind("unknown", 6)
  }

  sealed abstract class Detail {
    def toRecord: Record
  }
  object Detail {
    /**
      * Detail indicating that this source involves a concrete interaction.
      *
      * The semantic meaning of the interaction is defined by Source.Kind
      * (i.e. what triggered the observation), not by the interaction itself.
      */
    case class InteractionDetail(
      interaction: Interaction
    ) extends Detail {
      def toRecord: Record = Record.data(
        "interaction" -> interaction.toRecord
      )
    }
  }

  val network = Channel(Kind.Network)
  val command = Channel(Kind.Command)
}

/**
 * Substrate classifies where the condition manifested in the execution stack.
 *
 * This axis describes the execution or runtime substrate where the
 * observed condition surfaced, independently of source, channel,
 * origin, or interpretation.
 */
case class Substrate(
  kind: Substrate.Kind,
  detail: Option[Substrate.Detail] = None
) {
  def toRecord: Record = Record.dataAuto(
    "kind" -> kind.name,
    "detail" -> detail.map(_.toRecord)
  )
}

object Substrate {

  /**
   * Kind is a coarse-grained classification of execution substrate.
   * This enum is intentionally small and stable.
   */
  enum Kind(val name: String, val value: Int) {

    case Jvm extends Kind("jvm", 1)
    case Os extends Kind("os", 2)
    case Network extends Kind("network", 3)
    case Storage extends Kind("storage", 4)
    case Middleware extends Kind("middleware", 5)
    case Container extends Kind("container", 6)
    case Unknown extends Kind("unknown", 7)
  }

  /**
   * Detail represents optional, concrete information about the substrate.
   *
   * This information is descriptive only and MUST NOT introduce interpretation.
   * Typical, well-known patterns are modeled explicitly;
   * all others are captured via Opaque.
   */
  sealed abstract class Detail {
    def toRecord: Record
  }

  // --- Typical, structured substrate details ---

  /** JVM runtime manifestation. */
  case class JvmRuntime(
    exceptionClass: Option[String] = None
  ) extends Detail {
    def toRecord: Record = Record.dataOption(
      "kind" -> Some("jvm-runtime"),
      "exception-class" -> exceptionClass
    )
  }

  /** OS-level manifestation (process or syscall related). */
  case class OsProcess(
    pid: Option[Int] = None,
    signal: Option[String] = None
  ) extends Detail {
    def toRecord: Record = Record.dataOption(
      "kind" -> Some("os-process"),
      "pid" -> pid,
      "signal" -> signal
    )
  }

  /** Network stack manifestation. */
  case class NetworkStack(
    layer: Option[String] = None
  ) extends Detail {
    def toRecord: Record = Record.dataOption(
      "kind" -> Some("network-stack"),
      "layer" -> layer
    )
  }

  /** Storage-level manifestation. */
  case class StorageDevice(
    path: Option[String] = None
  ) extends Detail {
    def toRecord: Record = Record.dataOption(
      "kind" -> Some("storage-device"),
      "path" -> path
    )
  }

  /** Middleware-level manifestation (driver, runtime, library). */
  case class MiddlewareComponent(
    name: Option[String] = None
  ) extends Detail {
    def toRecord: Record = Record.dataOption(
      "kind" -> Some("middleware-component"),
      "name" -> name
    )
  }

  /** Container or orchestration-level manifestation. */
  case class ContainerRuntime(
    name: Option[String] = None
  ) extends Detail {
    def toRecord: Record = Record.dataOption(
      "kind" -> Some("container-runtime"),
      "name" -> name
    )
  }

  /**
   * Opaque substrate detail for cases that do not fit predefined structures.
   * This preserves factual information without forcing premature modeling.
   */
  case class Opaque(
    description: String,
    properties: Map[String, String] = Map.empty
  ) extends Detail {
    def toRecord: Record = Record.dataOption(
      "kind" -> Some("opaque"),
      "description" -> Some(description),
      "properties" -> Some(Record.create(properties))
    )
  }

  val jvm = Substrate(Kind.Jvm)
  val network = Substrate(Kind.Network)
}

/**
 * Origin represents the code location where the observation was recorded.
 *
 * This axis is intended for automatic population (e.g. via macros) and
 * refers to the point in code where the observation was made.
 * It MUST NOT encode responsibility, interpretation, or handling.
 */
sealed abstract class Origin {
  def toRecord: Record
}

object Origin {

  /**
   * CodeLocation represents a concrete source code location
   * where the observation was emitted.
   *
   * This typically corresponds to the top frame of a stack trace.
   */
  case class CodeLocation(
    className: String,
    methodName: String,
    fileName: Option[String] = None,
    lineNumber: Option[Int] = None
  ) extends Origin {
    def toRecord: Record = Record.dataOption(
      "kind" -> Some("code-location"),
      "class-name" -> Some(className),
      "method-name" -> Some(methodName),
      "file-name" -> fileName,
      "line-number" -> lineNumber
    )
  }

  /**
   * Opaque origin for cases where structured code location
   * information cannot be obtained or is intentionally suppressed.
   */
  case class Opaque(
    description: String
  ) extends Origin {
    def toRecord: Record = Record.dataOption(
      "kind" -> Some("opaque"),
      "description" -> Some(description)
    )
  }

  /**
   * Origin is unknown or unavailable.
   */
  case object Unknown extends Origin {
    def toRecord: Record = Record.data(
      "kind" -> "unknown"
    )
  }
}

/**
 * Environment represents operational and deployment context only and aggregates
 * mode, physical/network location, and execution platform.
 *
 * It captures the factual context of execution and deployment in which an
 * observation occurred, without encoding interpretation, handling, or normalization.
 */
case class Environment(
  mode: Option[Environment.Mode] = None,
  location: Option[Environment.Location] = None,
  platform: Option[Environment.Platform] = None,
  properties: Map[String, String] = Map.empty
) {
  def toRecord: Record = Record.dataAuto(
    "mode" -> mode.map(_.name),
    "location" -> location.map(_.toRecord),
    "platform" -> platform.map(_.toRecord),
    "properties" -> Record.create(properties)
  )
}
object Environment {

  /**
   * Execution mode of the running system.
   *
   * This enum reflects widely accepted deployment modes.
   *
   * NOTE:
   * CNCF may define its own runtime mode abstraction in the future.
   * When that happens, this enum should be aligned or mapped
   * to the CNCF definition rather than replaced.
   */
  enum Mode(val name: String, val value: Int) {
    case Production extends Mode("production", 1)
    case Staging extends Mode("staging", 2)
    case Development extends Mode("development", 3)
    case Demo extends Mode("demo", 4)
    case Test extends Mode("test", 5)
    case Other extends Mode("other", 6)
    case Unknown extends Mode("unknown", 7)
  }

  /**
   * Location represents the physical or network location
   * where the observation occurred.
   *
   * This captures concrete endpoints and addresses,
   * independent of logical system structure.
   */
  case class Location(
    uri: Option[java.net.URI] = None,
    url: Option[String] = None,
    ipAddress: Option[String] = None,
    hostName: Option[String] = None,
    port: Option[Int] = None
  ) {
    def toRecord: Record = Record.dataOption(
      "uri" -> uri.map(_.toString),
      "url" -> url,
      "ip-address" -> ipAddress,
      "host-name" -> hostName,
      "port" -> port
    )
  }

  /**
   * Platform represents the execution stack on which
   * the observation occurred.
   *
   * This includes OS, VM, runtime, middleware, container,
   * and related layers. All fields are descriptive only
   * and MUST NOT introduce interpretation or normalization.
   */
  case class Platform(
    os: Option[PlatformElement] = None,
    vm: Option[PlatformElement] = None,
    runtime: Option[PlatformElement] = None,
    middleware: Option[PlatformElement] = None,
    container: Option[PlatformElement] = None,
    properties: Map[String, String] = Map.empty
  ) {
    def toRecord: Record = Record.dataOption(
      "os" -> os.map(_.toRecord),
      "vm" -> vm.map(_.toRecord),
      "runtime" -> runtime.map(_.toRecord),
      "middleware" -> middleware.map(_.toRecord),
      "container" -> container.map(_.toRecord),
      "properties" -> Some(Record.create(properties))
    )
  }

  /**
   * PlatformElement represents a single layer in the execution stack.
   *
   * Examples:
   *   - os: linux / 6.6
   *   - vm: jvm / 21
   *   - runtime: scala / 3.4.2
   *   - middleware: cncf / 0.9.0
   *   - container: docker / 25.0
   */
  case class PlatformElement(
    name: String,
    version: Option[String] = None
  ) {
    def toRecord: Record = Record.dataOption(
      "name" -> Some(name),
      "version" -> version
    )
  }
}

case class ObservationId(major: String, minor: String) extends UniversalId(major, minor, "observation")
