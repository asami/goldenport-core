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
 *  version Jan.  2, 2026
 * @version Jan.  7, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class Ingress[T] {
  def inputClass: Class[T] = classOf[Any].asInstanceOf[Class[T]]
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

  def findByInput[I](clazz: Class[I]): Option[Ingress[I]] =
    ingresses.collectFirst {
      case ingress if ingress.inputClass == clazz =>
        ingress.asInstanceOf[Ingress[I]]
    }

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
  override def inputClass: Class[Array[String]] = classOf[Array[String]]

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
    _parse_params(params, Map.empty, Map.empty, Map.empty)
  }

  protected def parse_params(
    op: OperationDefinition,
    params: Array[String]
  ): (List[Argument], List[Switch], List[Property]) = {
    val definitions = op.specification.request.parameters
    val switchnames = _name_map(definitions, ParameterDefinition.Kind.Switch)
    val propertynames = _name_map(definitions, ParameterDefinition.Kind.Property)
    val argumentnames = _name_map(definitions, ParameterDefinition.Kind.Argument)
    _parse_params(params, switchnames, propertynames, argumentnames)
  }

  private def _parse_params(
    params: Array[String],
    switchnames: Map[String, String],
    propertynames: Map[String, String],
    argumentnames: Map[String, String]
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
        switchnames.get(key) match {
          case Some(name) =>
            switches = switches :+ Switch(name, true, None)
          case None =>
            val name = propertynames.getOrElse(key, key)
            properties = properties :+ Property(name, value, None)
        }
      } else if (s.startsWith("--")) {
        val key = s.drop(2)
        switchnames.get(key) match {
          case Some(name) =>
            switches = switches :+ Switch(name, true, None)
          case None if i + 1 < params.length && !params(i + 1).startsWith("-") =>
            val value = params(i + 1)
            argumentnames.get(key) match {
              case Some(name) =>
                arguments = arguments :+ Argument(name, value, None)
                consumedArguments = consumedArguments + name
              case None =>
                val name = propertynames.getOrElse(key, key)
                properties = properties :+ Property(name, value, None)
            }
            i += 1
          case None =>
            propertynames.get(key) match {
              case Some(name) =>
                properties = properties :+ Property(name, "", None)
              case None =>
                switches = switches :+ Switch(key, true, None)
            }
        }
      } else if (s.startsWith("-") && s.length > 1) {
        val keyvalue = s.drop(1)
        if (keyvalue.contains("=")) {
          val parts = keyvalue.split("=", 2)
          val key = if (parts.nonEmpty) parts(0) else ""
          val value = if (parts.length > 1) parts(1) else ""
          switchnames.get(key) match {
            case Some(name) =>
              switches = switches :+ Switch(name, true, None)
            case None =>
              val name = propertynames.getOrElse(key, key)
              properties = properties :+ Property(name, value, None)
          }
        } else {
          val key = keyvalue
          switchnames.get(key) match {
            case Some(name) =>
              switches = switches :+ Switch(name, true, None)
            case None if i + 1 < params.length && !params(i + 1).startsWith("-") =>
              val value = params(i + 1)
              argumentnames.get(key) match {
                case Some(name) =>
                  arguments = arguments :+ Argument(name, value, None)
                  consumedArguments = consumedArguments + name
                case None =>
                  val name = propertynames.getOrElse(key, key)
                  properties = properties :+ Property(name, value, None)
              }
              i += 1
            case None =>
              propertynames.get(key) match {
                case Some(name) =>
                  properties = properties :+ Property(name, "", None)
                case None =>
                  switches = switches :+ Switch(key, true, None)
              }
          }
        }
      } else {
        // positional argument
        val remaining =
          argumentnames.values.toSet.diff(consumedArguments).toList.sorted
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

  private def _name_map(
    definitions: List[ParameterDefinition],
    kind: ParameterDefinition.Kind
  ): Map[String, String] =
    definitions.collect {
      case p if p.kind == kind => p.names.map(_ -> p.name)
    }.flatten.toMap

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
  override def inputClass: Class[URL] = classOf[URL]

  override def encode(op: OperationDefinition, url: URL): Consequence[Request] = {
    val _ = op
    encode(url)
  }

  def encode(url: URL): Consequence[Request]
}

abstract class RestIngress extends Ingress[HttpRequest] {
  override def inputClass: Class[HttpRequest] = classOf[HttpRequest]

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
