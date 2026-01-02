package org.goldenport.protocol.handler.ingress

import java.net.URL
import org.goldenport.Consequence
import org.goldenport.protocol.{Argument, Property, Switch}
import org.goldenport.protocol.Request
import org.goldenport.protocol.spec.OperationDefinition
import org.goldenport.protocol.spec.ParameterDefinition
import org.goldenport.protocol.spec.ServiceDefinitionGroup
import org.goldenport.http.HttpRequest

/*
 * @since   Dec. 28, 2025
 *  version Dec. 28, 2025
 * @version Jan.  2, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class Ingress[T] {
  def encode(op: OperationDefinition, in: T): Consequence[Request]
}

object Ingress {
  def recordToArguments(record: org.goldenport.record.Record): List[Argument] =
    record.asNameStringVector.map {
      case (k, v) => Argument(k, v, None)
    }.toList

  // positional args -> named Arguments (param1, param2, ...)
  def positionalToArguments(values: Seq[String]): List[Argument] =
    values.zipWithIndex.map {
      case (v, i) => Argument(s"param${i + 1}", v, None)
    }.toList
}

final case class IngressCollection(
  ingresses: Vector[Ingress[?]] = Vector.empty
) {
  def ++(that: IngressCollection): IngressCollection =
    IngressCollection(this.ingresses ++ that.ingresses)

  def ingress(args: Array[String]): Consequence[ArgsIngress] = {
    val _ = args
    ingresses.collectFirst { case ingress: ArgsIngress => ingress } match {
      case Some(ingress) => Consequence.success(ingress)
      case None =>
        Consequence
          .failArgumentMissing
          .withInput("args")
          .build
    }
  }

  def ingress(req: HttpRequest): Consequence[RestIngress] = {
    val _ = req
    ingresses.collectFirst { case ingress: RestIngress => ingress } match {
      case Some(ingress) => Consequence.success(ingress)
      case None =>
        Consequence
          .failArgumentMissing
          .withInput("http")
          .build
    }
  }
}

object IngressCollection {

  /** Canonical empty instance */
  val empty: IngressCollection =
    new IngressCollection(Vector.empty)

  /** Binary-compatible zero-arg constructor */
  def apply(): IngressCollection =
    empty
}

abstract class ArgsIngress extends Ingress[Array[String]] {
  def encode(
    services: ServiceDefinitionGroup,
    args: Array[String]
  ): Consequence[Request] = {
    val (service, operation, opdef, rest) =
      _resolve_service_operation(services, args)

    val parsed =
      opdef match {
        case Some(definition) =>
          parse_args(definition, Array(operation) ++ rest)
        case None =>
          parse_args(Array(operation) ++ rest)
      }

    Consequence.success(
      Request(
        service = service,
        operation = parsed.operation,
        arguments = parsed.arguments,
        switches = parsed.switches,
        properties = parsed.properties
      )
    )
  }

  override def encode(
    op: OperationDefinition,
    args: Array[String]
  ): Consequence[Request] = {
    val parsed = parse_args(op, args)
    Consequence.success(
      Request(
        service = None,
        operation = parsed.operation,
        arguments = parsed.arguments,
        switches = parsed.switches,
        properties = parsed.properties
      )
    )
  }

  def encode(args: Array[String]): Consequence[Request] =
    Consequence.failure(
      new IllegalStateException(
        "ArgsIngress.encode(args) requires ServiceDefinitionGroup; use encode(services, args)"
      )
    )

  protected final case class ParsedArgs(
    operation: String,
    arguments: List[Argument],
    switches: List[Switch],
    properties: List[Property]
  )

  protected def parse_args(args: Array[String]): ParsedArgs = {
    val operation = args.headOption.getOrElse("")
    val rest = args.drop(1)
    val (arguments, switches, properties) = parse_params(rest)
    ParsedArgs(operation, arguments, switches, properties)
  }

  protected def parse_args(
    op: OperationDefinition,
    args: Array[String]
  ): ParsedArgs = {
    val operation = args.headOption.getOrElse("")
    val rest = args.drop(1)
    val (arguments, switches, properties) = parse_params(op, rest)
    ParsedArgs(operation, arguments, switches, properties)
  }

  protected def parse_params(
    params: Array[String]
  ): (List[Argument], List[Switch], List[Property]) = {
    _parse_params(params, Set.empty, Set.empty, Set.empty)
  }

  protected def parse_params(
    op: OperationDefinition,
    params: Array[String]
  ): (List[Argument], List[Switch], List[Property]) = {
    val definitions = op.specification.request.parameters
    val switchnames = definitions.collect {
      case p if p.kind == ParameterDefinition.Kind.Switch => p.name
    }.toSet
    val propertynames = definitions.collect {
      case p if p.kind == ParameterDefinition.Kind.Property => p.name
    }.toSet
    val argumentnames = definitions.collect {
      case p if p.kind == ParameterDefinition.Kind.Argument => p.name
    }.toSet
    _parse_params(params, switchnames, propertynames, argumentnames)
  }

  private def _parse_params(
    params: Array[String],
    switchnames: Set[String],
    propertynames: Set[String],
    argumentnames: Set[String]
  ): (List[Argument], List[Switch], List[Property]) = {
    var arguments: List[Argument] = Nil
    var switches: List[Switch] = Nil
    var properties: List[Property] = Nil
    var posIndex = 0
    var consumedArguments: Set[String] = Set.empty

    var i = 0
    while (i < params.length) {
      val s = params(i)
      if (s.startsWith("--") && s.contains("=")) {
        val parts = s.drop(2).split("=", 2)
        val key = if (parts.nonEmpty) parts(0) else ""
        val value = if (parts.length > 1) parts(1) else ""
        if (switchnames.contains(key)) {
          switches = switches :+ Switch(key, true, None)
        } else {
          properties = properties :+ Property(key, value, None)
        }
      } else if (s.startsWith("--")) {
        val key = s.drop(2)
        if (switchnames.contains(key)) {
          switches = switches :+ Switch(key, true, None)
        } else if (i + 1 < params.length && !params(i + 1).startsWith("--")) {
          val value = params(i + 1)
          if (argumentnames.contains(key)) {
            arguments = arguments :+ Argument(key, value, None)
            consumedArguments = consumedArguments + key
          } else {
            properties = properties :+ Property(key, value, None)
          }
          i += 1
        } else if (propertynames.contains(key)) {
          properties = properties :+ Property(key, "", None)
        } else {
          switches = switches :+ Switch(key, true, None)
        }
      } else {
        // positional argument
        val remaining =
          argumentnames.diff(consumedArguments).toList.sorted
        remaining match {
          case name :: _ =>
            arguments = arguments :+ Argument(name, s, None)
            consumedArguments = consumedArguments + name
          case Nil =>
            posIndex += 1
            arguments = arguments :+ Argument(s"param$posIndex", s, None)
        }
      }
      i += 1
    }

    (arguments, switches, properties)
  }

  private def _resolve_service_operation(
    services: ServiceDefinitionGroup,
    args: Array[String]
  ): (Option[String], String, Option[OperationDefinition], Array[String]) = {
    if (args.isEmpty)
      return (None, "", None, Array.empty[String])

    val head = args(0)
    services.services.find(_.name == head) match {
      case Some(service) =>
        val operation = args.lift(1).getOrElse("")
        val opdef = service.operations.operations.find(_.name == operation)
        val rest = if (args.length > 2) args.drop(2) else Array.empty[String]
        (Some(service.name), operation, opdef, rest)
      case None =>
        val operation = head
        val opdef =
          services.services
            .iterator
            .flatMap(_.operations.operations.toVector)
            .find(_.name == operation)
        (None, operation, opdef, args.drop(1))
    }
  }
}
object ArgsIngress {
  case class Instance() extends ArgsIngress() {
  }

  def apply(): ArgsIngress = new Instance()
}

// compatibility
case class DefaultArgsIngress() extends ArgsIngress()

abstract class UrlIngress extends Ingress[URL] {
  override def encode(op: OperationDefinition, url: URL): Consequence[Request] = {
    val _ = op
    encode(url)
  }

  def encode(url: URL): Consequence[Request]
}

abstract class RestIngress extends Ingress[HttpRequest] {
  override def encode(
    op: OperationDefinition,
    req: HttpRequest
  ): Consequence[Request] = {
    val inputs =
      req.method match {
        case HttpRequest.GET     => req.query
        case HttpRequest.POST    => req.form
        case HttpRequest.PUT     => req.form
        case HttpRequest.DELETE  => req.query
      }

    val arguments =
      Ingress.recordToArguments(inputs)

    Consequence.success(
      Request(
        service = None,
        operation = op.name,
        arguments = arguments,
        switches = Nil,
        properties = Nil
      )
    )
  }
}

object RestIngress {
  case class Instance() extends RestIngress {
  }

  def apply(): RestIngress = Instance()
}
