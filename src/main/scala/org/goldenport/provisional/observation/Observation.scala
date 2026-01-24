package org.goldenport.provisional.observation

import java.time.Instant
import org.goldenport.text.Presentable
import org.goldenport.id.UniversalId
import org.goldenport.observation.{TraceId, SpanId}
import org.goldenport.observation.{Subject, Agent, Resource}
import org.goldenport.observation.Severity

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
 * @version Jan. 25, 2026
 * @author  ASAMI, Tomoharu
 */
case class Observation(
  taxonomy: Taxonomy,
  cause: Cause,
  source: Source,
  channel: Channel,
  substrate: Substrate,
  origin: Origin,

  // --- Observability core ---
  timestamp: Instant,

  subject: Subject,
  agent: Agent,
  `object`: Resource,

  // --- Observability extensions ---
  severity: Option[Severity] = None, // Log level
  traceId: Option[TraceId] = None,
  spanId: Option[SpanId] = None,
  environment: Option[Environment] = None,

  message: Option[String] = None,
  exception: Option[Throwable] = None,
  properties: Map[String, String] = Map.empty,

  observationId: Option[ObservationId] = None
)

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
  def print = s"$category-$symptom"
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
    /** The problem is observed in the state of a system, component, or domain entity.
      * This category is used when the issue concerns invalid or prohibited states or state transitions.
      */
    case State extends Category("state", 5)
    /** The problem is observed at the system level and cannot be meaningfully attributed to argument, property, configuration, resource, or state.
      * This category is used as a last resort for fundamental or infrastructural failures.
      */
    case System extends Category("system", 6)
  }

  enum Symptom(val name: String, val value: Int) {
    /** The observed data or structure does not conform to the required syntactic rules and cannot be parsed as intended. */
    case SyntaxError extends Symptom("syntax-error", 1)
    /** The observed data is syntactically valid but does not match the expected format or representation. */
    case FormatError extends Symptom("format-error", 2)
    /** The observed value violates domain-level constraints or rules, despite being syntactically and structurally valid. */
    case DomainValue extends Symptom("domain-value", 3)
    /** A required value or element is not provided or not present where it is expected to exist. */
    case Missing extends Symptom("missing", 4)
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
  }
}

case class Cause(
  kind: Cause.Kind,
  detail: Option[Cause.Detail]
)
object Cause {
  enum Kind(val name: String, val value: Int) {
    case Parse extends Kind("parse", 1)
    case Validate extends Kind("validate", 2)
    case Lookup extends Kind("lookup", 3)
    case Resolve extends Kind("resolve", 4)
    case Execute extends Kind("execute", 5)
    case Communicate extends Kind("communicate", 6)
    case Timeout extends Kind("timeout", 7)
    case Conflict extends Kind("conflict", 8)
    case Unknown extends Kind("unknown", 9)
  }

  /**
   * Optional, non-authoritative detail about the failure mechanism.
   * This information is descriptive only and must not affect interpretation.
   */
  case class Detail(
    message: Option[String] = None,
    data: Map[String, String] = Map.empty
  )
}

/**
 * Interaction represents a concrete form of interaction or access
 * (e.g. HTTP, database access, filesystem access).
 *
 * This model is shared across multiple observation axes (Source, Channel),
 * where its semantic meaning is determined by the enclosing axis.
 */
sealed abstract class Interaction

object Interaction {

  /** HTTP-based interaction. */
  case class Http(
    method: Option[String],
    url: String
  ) extends Interaction

  /** Database interaction (SQL or driver-based). */
  case class Database(
    operation: Option[String],
    target: Option[String]
  ) extends Interaction

  /** Filesystem interaction. */
  case class FileSystem(
    operation: Option[String],
    path: String
  ) extends Interaction

  /** Messaging or event-based interaction. */
  case class Messaging(
    system: String,
    operation: Option[String]
  ) extends Interaction

  /**
   * Opaque interaction for cases that do not fit predefined structures.
   * This preserves factual information without forcing premature modeling.
   */
  case class Opaque(
    description: String,
    properties: Map[String, String] = Map.empty
  ) extends Interaction
}

/**
 * Source classifies where the triggering interaction originated.
 *
 * This axis describes the origin domain of input data or interaction,
 * independent of channel, substrate, origin, or responsibility.
 */
case class Source(
  kind: Source.Kind,
  detail: Option[Source.Detail]
)
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

  sealed abstract class Detail
  object Detail {
    /**
      * Detail indicating that this source involves a concrete interaction.
      *
      * The semantic meaning of the interaction is defined by Source.Kind
      * (i.e. what triggered the observation), not by the interaction itself.
      */
    case class InteractionDetail(
      interaction: Interaction
    ) extends Detail
  }
}

/**
 * Channel classifies how the interaction or access was performed.
 *
 * This axis describes the access or communication mechanism itself,
 * regardless of source, substrate, origin, or interpretation.
 */
case class Channel(
  kind: Channel.Kind,
  detail: Option[Channel.Detail]
)

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

  sealed abstract class Detail
  object Detail {
    /**
      * Detail indicating that this source involves a concrete interaction.
      *
      * The semantic meaning of the interaction is defined by Source.Kind
      * (i.e. what triggered the observation), not by the interaction itself.
      */
    case class InteractionDetail(
      interaction: Interaction
    ) extends Detail
  }
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
  detail: Option[Substrate.Detail]
)

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
  sealed abstract class Detail

  // --- Typical, structured substrate details ---

  /** JVM runtime manifestation. */
  case class JvmRuntime(
    exceptionClass: Option[String] = None
  ) extends Detail

  /** OS-level manifestation (process or syscall related). */
  case class OsProcess(
    pid: Option[Int] = None,
    signal: Option[String] = None
  ) extends Detail

  /** Network stack manifestation. */
  case class NetworkStack(
    layer: Option[String] = None
  ) extends Detail

  /** Storage-level manifestation. */
  case class StorageDevice(
    path: Option[String] = None
  ) extends Detail

  /** Middleware-level manifestation (driver, runtime, library). */
  case class MiddlewareComponent(
    name: Option[String] = None
  ) extends Detail

  /** Container or orchestration-level manifestation. */
  case class ContainerRuntime(
    name: Option[String] = None
  ) extends Detail

  /**
   * Opaque substrate detail for cases that do not fit predefined structures.
   * This preserves factual information without forcing premature modeling.
   */
  case class Opaque(
    description: String,
    properties: Map[String, String] = Map.empty
  ) extends Detail
}

/**
 * Origin represents the code location where the observation was recorded.
 *
 * This axis is intended for automatic population (e.g. via macros) and
 * refers to the point in code where the observation was made.
 * It MUST NOT encode responsibility, interpretation, or handling.
 */
sealed abstract class Origin

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
  ) extends Origin

  /**
   * Opaque origin for cases where structured code location
   * information cannot be obtained or is intentionally suppressed.
   */
  case class Opaque(
    description: String
  ) extends Origin

  /**
   * Origin is unknown or unavailable.
   */
  case object Unknown extends Origin
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
)
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
  )

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
  )

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
  )
}

case class ObservationId(major: String, minor: String) extends UniversalId(major, minor, "observation")
