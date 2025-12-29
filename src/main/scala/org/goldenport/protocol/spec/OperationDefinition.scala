package org.goldenport.protocol.spec

import cats.data.NonEmptyVector
import java.time.ZonedDateTime
import org.goldenport.{Conclusion, Consequence}
import org.goldenport.datatype.I18nMessage
import org.goldenport.http.HttpRequest
import org.goldenport.observation.Cause
import org.goldenport.protocol.Request
import org.goldenport.protocol.operation.OperationRequest
import org.goldenport.schema.{CanonicalDataType, IntegerDataType, Multiplicity, ValueDomain}

/*
 * @since   Oct.  6, 2018
 *  version Oct. 10, 2018
 *  version Feb. 24, 2019
 *  version Feb. 15, 2020
 *  version Nov. 25, 2023
 *  version Mar. 15, 2025
 * @version Dec. 29, 2025
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
    val domain = ValueDomain(
      datatype = p.datatype,
      multiplicity = p.multiplicity,
      constraints = Nil
    )
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
          Consequence
            .failArgumentMissing
            .withInput(p.name)
            .build
        else
          Consequence.success(ResolvedEmpty(domain))

      case head :: Nil =>
        for {
          normalized <- _normalize_values(p, Vector(head))
          _ <- _validate_value_domain(domain, normalized)
        } yield ResolvedSingle(normalized.head, domain)

      case _ =>
        if (_allows_multiple(p)) {
          for {
            normalized <- _normalize_values(p, values.toVector)
            _ <- _validate_value_domain(domain, normalized)
          } yield ResolvedMultiple(normalized, domain)
        } else {
          Consequence
            .failArgumentRedundant
            .withInput(p.name)
            .build
        }
    }
  }

  protected final def take_string(
    name: String
  )(using req: Request): Consequence[String] =
    _parameter_definition(name).flatMap { p =>
      resolveParameter(p).flatMap {
        case ResolvedSingle(v: String, _) => Consequence.success(v)
        case ResolvedSingle(_, _) =>
          _failure_with_cause(
            s"parameter is not a string: ${p.name}",
            Cause.FormatError
          )
        case ResolvedEmpty(_) => Consequence.failure(s"parameter missing: ${p.name}")
        case ResolvedMultiple(_, _) => Consequence.failure(s"multiple values not allowed: ${p.name}")
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
    domain: ValueDomain,
    values: Vector[Any]
  ): Consequence[Unit] =
    domain.datatype match {
      case dt: IntegerDataType =>
        _validate_integer_domain(values, dt)
      case _ =>
        Consequence.success(())
    }

  private def _normalize_values(
    p: ParameterDefinition,
    values: Vector[Any]
  ): Consequence[Vector[Any]] =
    p.datatype match {
      case dt: CanonicalDataType[?] =>
        dt.normalizer.normalizeAll(values).map(_.toVector)
      case _ =>
        Consequence.success(values.map(_.toString))
    }

  private def _validate_integer_domain(
    values: Vector[Any],
    dt: IntegerDataType
  ): Consequence[Unit] = {
    val normalized = values.collect { case v: BigInt => v }
    if (normalized.size != values.size) {
      _failure_with_cause("format error: integer value", Cause.FormatError)
    } else {
      val invalid = normalized.exists(value => !dt.isValid(value))
      if (invalid) {
        _failure_with_cause("value domain error: integer range", Cause.ValueDomainError)
      } else {
        Consequence.success(())
      }
    }
  }

  private def _failure_with_cause[A](
    message: String,
    cause: Cause
  ): Consequence[A] = {
    val base = Conclusion.simple(message)
    val observation = base.observation.copy(
      cause = Some(cause),
      message = Some(I18nMessage(message))
    )
    Consequence.Failure(base.copy(observation = observation))
  }
}

object OperationDefinition {
  def apply(
    name: String,
    request: RequestDefinition,
    response: ResponseDefinition
  ): OperationDefinition =
    Instance(
      Specification(
        name = name,
        request = request,
        response = response
      )
    )

  case class Specification(
    name: String,
    request: RequestDefinition,
    response: ResponseDefinition
  )
  object Specification {
    trait Holder {
      def specification: Specification

      def name: String = specification.name
      def request: RequestDefinition = specification.request
      def response: ResponseDefinition = specification.response
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
      Consequence.success(
        OperationRequest(
          service    = req.service,
          operation  = name,
          arguments  = req.arguments,
          switches   = req.switches,
          properties = req.properties
        )
      )
  }
}

case class OperationDefinitionGroup(
  operations: NonEmptyVector[OperationDefinition]
) {
  def head: OperationDefinition = operations.head
}
