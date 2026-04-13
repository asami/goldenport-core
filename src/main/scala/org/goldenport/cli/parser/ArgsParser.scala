package org.goldenport.cli.parser

import org.goldenport.protocol.Request
import org.goldenport.protocol.Argument
import org.goldenport.protocol.Switch
import org.goldenport.protocol.Property
import org.goldenport.protocol.spec.{OperationDefinition, ParameterDefinition, ServiceDefinition}

/*
 * @since   Dec. 23, 2025
 *  version Dec. 25, 2025
 *  version Mar. 13, 2026
 * @version Apr. 14, 2026
 * @author  ASAMI, Tomoharu
 */
class ArgsParser(config: ArgsParser.Config) {
  private val MaxParamLength = 2048

  /**
   * Parse CLI args into a protocol-level Request
   * using the given Service definition.
   */
  def parse(service: ServiceDefinition, args: List[String]): Request = {
    var arguments: List[Argument] = Nil
    var switches: List[Switch] = Nil
    var properties: List[Property] = Nil

    val in = args.toVector
    var i = 0
    while (i < in.length) {
      val current = in(i)
      if (current.startsWith("--") && current.contains("=")) {
        val parts = current.drop(2).split("=", 2)
        val key = if (parts.nonEmpty) parts(0) else ""
        val value = if (parts.length > 1) parts(1) else ""
        properties = properties :+ Property(key, value, None)
      } else if (current.startsWith("--")) {
        if (i + 1 < in.length && !in(i + 1).startsWith("--")) {
          val key = current.drop(2)
          val value = in(i + 1)
          properties = properties :+ Property(key, value, None)
          i = i + 1
        } else {
          switches = switches :+ Switch(current.drop(2), true, None)
        }
      } else {
        arguments = arguments :+ Argument(current, current, None)
      }
      i = i + 1
    }

    Request(
      component  = None,
      service    = Some(service.name),
      operation  = args.headOption.getOrElse(""),
      arguments  = arguments,
      switches   = switches,
      properties = properties
    )
  }

  def parse(
    op: OperationDefinition,
    args: List[String]
  ): Request = {
    val (arguments, switches, properties) = _parse_params(args.toVector, op)
    Request(
      component = None,
      service = None,
      operation = op.name,
      arguments = arguments,
      switches = switches,
      properties = properties
    )
  }

  def parse(
    service: ServiceDefinition,
    op: OperationDefinition,
    args: List[String]
  ): Request = {
    val (arguments, switches, properties) = _parse_params(args.toVector, op)
    Request(
      component = None,
      service = Some(service.name),
      operation = op.name,
      arguments = arguments,
      switches = switches,
      properties = properties
    )
  }

  private def _parse_params(
    params: Vector[String],
    op: OperationDefinition
  ): (List[Argument], List[Switch], List[Property]) = {
    val definitions = op.specification.request.parameters
    val switchnames = _name_map(definitions, ParameterDefinition.Kind.Switch)
    val propertynames = _name_map(definitions, ParameterDefinition.Kind.Property)
    val argumentnames = _name_map(definitions, ParameterDefinition.Kind.Argument)
    _parse_params(params, switchnames, propertynames, argumentnames)
  }

  private def _parse_params(
    params: Vector[String],
    switchnames: Map[String, String],
    propertynames: Map[String, String],
    argumentnames: Map[String, String]
  ): (List[Argument], List[Switch], List[Property]) = {
    var arguments: List[Argument] = Nil
    var switches: List[Switch] = Nil
    var properties: List[Property] = Nil
    var posindex = 0
    var consumedarguments: Set[String] = Set.empty

    var i = 0
    while (i < params.length) {
      val current = params(i)
      if (current.startsWith("--") && current.contains("=")) {
        val parts = current.drop(2).split("=", 2)
        val key = if (parts.nonEmpty) parts(0) else ""
        val value = if (parts.length > 1) parts(1) else ""
        switchnames.get(key) match {
          case Some(name) =>
            switches = switches :+ Switch(name, true, None)
          case None =>
            argumentnames.get(key) match {
              case Some(name) =>
                arguments = arguments :+ Argument(name, value, None)
                consumedarguments = consumedarguments + name
              case None =>
                val name = propertynames.getOrElse(key, key)
                properties = properties :+ Property(name, value, None)
            }
        }
      } else if (current.startsWith("--")) {
        val key = current.drop(2)
        switchnames.get(key) match {
          case Some(name) =>
            switches = switches :+ Switch(name, true, None)
          case None if i + 1 < params.length && !params(i + 1).startsWith("-") =>
            val value = params(i + 1)
            argumentnames.get(key) match {
              case Some(name) =>
                arguments = arguments :+ Argument(name, value, None)
                consumedarguments = consumedarguments + name
              case None =>
                val name = propertynames.getOrElse(key, key)
                properties = properties :+ Property(name, value, None)
            }
            i = i + 1
          case None =>
            propertynames.get(key) match {
              case Some(name) =>
                properties = properties :+ Property(name, "", None)
              case None =>
                switches = switches :+ Switch(key, true, None)
            }
        }
      } else {
        val remaining = argumentnames.values.toSet.diff(consumedarguments).toList.sorted
        remaining match {
          case name :: _ =>
            arguments = arguments :+ Argument(name, current, None)
            consumedarguments = consumedarguments + name
          case Nil =>
            posindex = posindex + 1
            arguments = arguments :+ Argument(s"param$posindex", current, None)
        }
      }
      i = i + 1
    }

    (arguments, switches, properties)
  }

  private def _name_map(
    definitions: List[ParameterDefinition],
    kind: ParameterDefinition.Kind
  ): Map[String, String] =
    definitions.collect { case p if p.kind == kind => p.names.map(_ -> p.name) }.flatten.toMap


  // Legacy validation-based implementation (kept for reference)
  // Validation is handled in CliLogic using structured Consequence errors.
}

object ArgsParser {
  case class Config() {
  }
  object Config {
    val default = Config()
  }
}
