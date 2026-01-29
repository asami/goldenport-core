package org.goldenport.protocol.spec

import cats.data.NonEmptyVector
import java.time.ZonedDateTime
import org.goldenport.{Conclusion, Consequence}
import org.goldenport.datatype.I18nMessage
import org.goldenport.model.value.BaseContent
import org.goldenport.http.HttpRequest
import org.goldenport.provisional.observation.Cause
import org.goldenport.protocol.Request
import org.goldenport.protocol.operation.OperationRequest
import org.goldenport.schema.{CanonicalDataType, Constraint, IntegerDataType, Multiplicity, ValueDomain}

/*
 * @since   Oct.  6, 2018
 *  version Oct. 10, 2018
 *  version Feb. 24, 2019
 *  version Feb. 15, 2020
 *  version Nov. 25, 2023
 *  version Mar. 15, 2025
 *  version Dec. 30, 2025
 * @version Jan. 29, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class OperationDefinition
    extends OperationDefinition.Specification.Holder {
  import OperationDefinition._

  def specification: Specification

  def createOperationRequest(req: Request): Consequence[OperationRequest]

  // Typed ResolvedValue
  protected sealed trait ResolvedValue {
    def domain: ValueDomain
  }
  protected final case class ResolvedSingle(
    value: Any,
    domain: ValueDomain
  ) extends ResolvedValue
  protected final case class ResolvedMultiple(
    values: Vector[Any],
    domain: ValueDomain
  ) extends ResolvedValue
  protected final case class ResolvedEmpty(
    domain: ValueDomain
  ) extends ResolvedValue

  // Typed resolveParameter using DataType as parser/factory
  protected final def resolveParameter(
    p: ParameterDefinition
  )(using req: Request): Consequence[ResolvedValue] = {
    val domain = p.domain
    val values: List[Any] = p.kind match {
      case ParameterDefinition.Kind.Argument =>
        req.arguments.filter(_.name == p.name).map(_.value)
      case ParameterDefinition.Kind.Switch =>
        req.switches.filter(_.name == p.name).map(_.value)
      case ParameterDefinition.Kind.Property =>
        req.properties.filter(_.name == p.name).map(_.value)
    }

    values match {
      case Nil =>
        if (_is_required(p))
          Consequence.failArgumentMissing(p.name)
        else
          Consequence.success(ResolvedEmpty(domain))

      case head :: Nil =>
        for {
          normalized <- _normalize_values(p, Vector(head))
          _ <- _validate_value_domain(p, normalized)
        } yield ResolvedSingle(normalized.head, domain)

      case _ =>
        if (_allows_multiple(p)) {
          for {
            normalized <- _normalize_values(p, values.toVector)
            _ <- _validate_value_domain(p, normalized)
          } yield ResolvedMultiple(normalized, domain)
        } else {
          Consequence.failArgumentRedundantOperation(p.name, name)
        }
    }
  }

  protected final def take_string(
    name: String
  )(using req: Request): Consequence[String] =
    _parameter_definition(name).flatMap { p =>
      resolveParameter(p).flatMap {
        case ResolvedSingle(v: String, _) => Consequence.success(v)
        case ResolvedSingle(v, _) =>
          Consequence.failArgumentFormatError(
            p.name,
            v,
            "parameter is not a string"
          )
        case ResolvedEmpty(_) => Consequence.failArgumentMissing(p.name)
        // Consequence.failure(s"parameter missing: ${p.name}")
        case ResolvedMultiple(_, _) => Consequence.failArgumentMultipleValues(p.name)
        // Consequence.failure(s"multiple values not allowed: ${p.name}")
      }
    }

  protected final def get_datetime(
    name: String
  )(using req: Request): Consequence[Option[ZonedDateTime]] =
    _parameter_definition(name).flatMap { p =>
      resolveParameter(p).map {
        case ResolvedSingle(_, _) => None
        case ResolvedEmpty(_) => None
        case ResolvedMultiple(_, _) => None
      }
    }

  private def _parameter_definition(name: String): Consequence[ParameterDefinition] =
    specification.request.parameters.find(_.name == name) match {
      case Some(p) => Consequence.success(p)
      case None => Consequence.failure(s"parameter not defined: ${name}")
    }

  private def _is_required(p: ParameterDefinition): Boolean = p.multiplicity match {
    case Multiplicity.One => true
    case Multiplicity.OneMore => true
    case Multiplicity.Range(from, _) => from > 0
    case Multiplicity.Ranges(ranges) => ranges.exists(_.head.from > 0)
    case _ => false
  }

  private def _allows_multiple(p: ParameterDefinition): Boolean = p.multiplicity match {
    case Multiplicity.One => false
    case Multiplicity.ZeroOne => false
    case Multiplicity.OneMore => true
    case Multiplicity.ZeroMore => true
    case Multiplicity.Range(_, to) => to > 1
    case Multiplicity.Ranges(ranges) => ranges.exists(_.head.to > 1)
  }

  // Value-domain validation on typed value (default: always succeeds)
  protected def _validate_value_domain(
    p: ParameterDefinition,
    values: Vector[Any]
  ): Consequence[Unit] =
    p.domain.datatype match {
      case dt: IntegerDataType =>
        _validate_integer_domain(values, dt, p.name)
          .flatMap(_ => _validate_constraints(values, p.constraints, p.name))
      case _ =>
        _validate_constraints(values, p.constraints, p.name)
    }

  private def _normalize_values(
    p: ParameterDefinition,
    values: Vector[Any]
  ): Consequence[Vector[Any]] =
    p.datatype match {
      case dt: CanonicalDataType[?] =>
        dt.normalizer.normalizeAll(values).map(_.toVector).recoverConclusion(_.toCategoryArgument)
      case _ =>
        Consequence.success(values.map(_.toString))
    }

  private def _validate_integer_domain(
    values: Vector[Any],
    dt: IntegerDataType,
    name: String
  ): Consequence[Unit] = {
    val normalized = values.collect { case v: BigInt => v }
    if (normalized.size != values.size) {
      Consequence.failArgumentFormatError(name, normalized, dt)
    } else {
      val invalid = normalized.exists(value => !dt.isValid(value))
      if (invalid) {
        Consequence.failArgumentDataType(name, normalized, dt)
      } else {
        Consequence.success(())
      }
    }
  }

  private def _validate_constraints(
    values: Vector[Any],
    constraints: Vector[Constraint],
    name: String
  ): Consequence[Unit] = 
    constraints.headOption match {
      case Some(s) =>
        val invalid =
          constraints.exists { c =>
            values.exists(v => c.validate(v).isLeft)
          }

        if (invalid)
          Consequence.failArgumentConstraint(name, values, NonEmptyVector(s, constraints.tail))
        else
          Consequence.success(())
      case None => Consequence.success(())
    }

  // private def _fail_value_domain[A](name: String): Consequence[A] =
  //   Consequence
  //     .FailureBuilder(Cause.valueDomainError)
  //     .withInput(name)
  //     .build

  // private def _failure_with_cause[A](
  //   message: String,
  //   cause: Cause
  // ): Consequence[A] = {
  //   val base = Conclusion.simple(message)
  //   val observation = base.observation.copy(
  //     cause = Some(cause),
  //     message = Some(I18nMessage(message))
  //   )
  //   Consequence.Failure(base.copy(observation = observation))
  // }
  private def _failure_with_cause[A](
    message: String,
    cause: Cause
  ): Consequence[A] = Consequence.create(message, cause)
}

object OperationDefinition {
  /**
   * Rule represents invariant, model-level constraints of an operation.
   *
   * Rules are defined by models (e.g. CML) and must be enforced
   * before execution. Violating a Rule must always result in rejection.
   *
   * This definition is runtime-agnostic and must not include
   * environment-dependent or operational policies.
   *
   * NOTE:
   * This is intentionally minimal and currently unused.
   * It serves as a vocabulary placeholder to fix design intent.
   */
  final case class Rule(
    kind: Rule.Kind,
    allowBusinessWrite: Boolean = false,
    allowExternalEffect: Boolean = false
  )

  object Rule {
    sealed trait Kind
    case object Command extends Kind
    case object Query extends Kind
  }

  @deprecated("Use BaseContent-based constructor", "2025-12-30")
  def apply(
    name: String,
    request: RequestDefinition,
    response: ResponseDefinition
  ): OperationDefinition =
    Instance(Specification(name, request, response))

  def apply(
    content: BaseContent,
    request: RequestDefinition,
    response: ResponseDefinition
  ): OperationDefinition =
    Instance(
      Specification(
        content = content,
        request = request,
        response = response
      )
    )

  case class Specification(
    content: BaseContent,
    request: RequestDefinition,
    response: ResponseDefinition
  ) extends BaseContent.BareHolder {
    protected def baseContent: BaseContent = content
  }
  object Specification {
    def apply(
      name: String,
      request: RequestDefinition,
      response: ResponseDefinition
    ): Specification =
      Specification(
        content = BaseContent.simple(name),
        request = request,
        response = response
      )

    trait Holder extends BaseContent.BareHolder {
      def specification: Specification

      protected def baseContent: BaseContent = specification.content
      def request: RequestDefinition = specification.request
      def response: ResponseDefinition = specification.response
    }

    case class Builder(
      content: BaseContent.Builder = BaseContent.Builder(),
      request: RequestDefinition = RequestDefinition(),
      response: ResponseDefinition = ResponseDefinition()
    ) {
      def build(): Specification = Specification(
        content.build(),
        request,
        response
      )
    }
    object Builder {
      def apply(name: String): Builder = Builder(content = BaseContent.Builder(name))
    }
  }

  final case class Instance(specification: Specification) extends OperationDefinition {
    // Default / inline implementation.
    //
    // This implementation is intended for use cases where application logic
    // directly consumes OperationRequest without introducing a dedicated
    // Command / Query subclass.
    //
    // NOTE:
    // The full, official implementation — including validation and
    // value-instantiation based on RequestDefinition — is intentionally
    // deferred at this stage.
    //
    // For now, this method simply bridges a syntactic Request into an
    // OperationRequest instance. The behavior here should be treated as
    // provisional and subject to refinement.
    override def createOperationRequest(req: Request): Consequence[OperationRequest] =
      given Request = req
      val validations = specification.request.parameters.map(resolveParameter)
      Consequence.zipN(validations).map { _ =>
        OperationRequest(
          service    = req.service,
          operation  = name,
          arguments  = req.arguments,
          switches   = req.switches,
          properties = req.properties
        )
      }
  }

  case class Builder() {
  }
  object Builder {
    abstract class OperationFactory() {
      def createOperation(
        name: String,
        req: RequestDefinition,
        res: ResponseDefinition
      ): OperationDefinition
    }
    object OperationFactory {
      case object Default extends OperationFactory {
        def createOperation(
          name: String,
          req: RequestDefinition,
          res: ResponseDefinition
        ): OperationDefinition = OperationDefinition(name, req, res)
      }
    }
  }
}

case class OperationDefinitionGroup(
  operations: NonEmptyVector[OperationDefinition]
) {
  def head: OperationDefinition = operations.head

  def add(p: OperationDefinition) = copy(operations = operations :+ p)
}
object OperationDefinitionGroup {
  def apply(p: OperationDefinition, ps: OperationDefinition*): OperationDefinitionGroup =
    OperationDefinitionGroup(NonEmptyVector(p, ps.toVector))

  def create(ops: Seq[OperationDefinition]): OperationDefinitionGroup =
    OperationDefinitionGroup(NonEmptyVector(ops.head, ops.tail.toVector))
}
