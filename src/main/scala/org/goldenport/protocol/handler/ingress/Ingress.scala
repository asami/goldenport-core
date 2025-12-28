package org.goldenport.protocol.handler.ingress

import java.net.URL
import org.goldenport.Consequence
import org.goldenport.protocol.{Argument, Property, Switch}
import org.goldenport.protocol.Request
import org.goldenport.protocol.spec.OperationDefinition
import org.goldenport.protocol.spec.ParameterDefinition

/*
 * @since   Dec. 28, 2025
 * @version Dec. 28, 2025
 * @author  ASAMI, Tomoharu
 */
abstract class Ingress[T] {
  def encode(op: OperationDefinition, in: T): Consequence[Request]
}

case class IngressCollection(
  ingresses: Vector[Ingress[?]]
) {
  def takeIngress(args: Array[String]): Consequence[ArgsIngress] = {
    val _ = args
    ingresses.collectFirst { case ingress: ArgsIngress => ingress } match {
      case Some(ingress) => Consequence.success(ingress)
      case None => Consequence.failure("ingress not found")
    }
  }
}

abstract class ArgsIngress extends Ingress[Array[String]] {
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

  def encode(args: Array[String]): Consequence[Request] = {
    val parsed = parse_args(args)
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
    _parse_params(params, Set.empty, Set.empty)
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
    _parse_params(params, switchnames, propertynames)
  }

  private def _parse_params(
    params: Array[String],
    switchnames: Set[String],
    propertynames: Set[String]
  ): (List[Argument], List[Switch], List[Property]) = {
    var arguments: List[Argument] = Nil
    var switches: List[Switch] = Nil
    var properties: List[Property] = Nil

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
          properties = properties :+ Property(key, value, None)
          i += 1
        } else if (propertynames.contains(key)) {
          properties = properties :+ Property(key, "", None)
        } else {
          switches = switches :+ Switch(key, true, None)
        }
      } else {
        arguments = arguments :+ Argument(s, s, None)
      }
      i += 1
    }

    (arguments, switches, properties)
  }
}

abstract class UrlIngress extends Ingress[URL] {
  override def encode(op: OperationDefinition, url: URL): Consequence[Request] = {
    val _ = op
    encode(url)
  }

  def encode(url: URL): Consequence[Request]
}

abstract class RestIngress extends UrlIngress {
}
