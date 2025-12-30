package org.goldenport.protocol.handler.projection

import org.goldenport.Consequence
import org.goldenport.protocol.spec.{OperationDefinition, ServiceDefinition, ServiceDefinitionGroup}
import org.goldenport.protocol.spec.ParameterDefinition
import org.goldenport.schema.Multiplicity

/*
 * @since   Dec. 30, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
case object CliHelp extends ProjectionKind[String] {
  val name = "cli-help"
}

class CliHelpProjection extends Projection[String] {
  val kind: ProjectionKind[String] = CliHelp

  def project(defs: ServiceDefinitionGroup): Consequence[String] =
    Consequence.success(
      _render_protocol(defs).mkString("\n")
    )

  private def _render_protocol(defs: ServiceDefinitionGroup): Vector[String] = {
    val header = Vector(
      "Protocol Help",
      "Usage: <service> <operation> [args]"
    )
    val services = _render_services(defs.services)
    (header ++ services).filter(_.nonEmpty)
  }

  private def _render_services(services: Vector[ServiceDefinition]): Vector[String] = {
    val header = Vector("Services:")
    val names = services.map(s => s"- ${s.name}")
    val details = services.flatMap(_render_service)
    (header ++ names ++ Vector("") ++ details).filter(_.nonEmpty)
  }

  private def _render_service(service: ServiceDefinition): Vector[String] = {
    val header = Vector(s"Service: ${service.name}", "Operations:")
    val operations = service.operations.operations.toVector.flatMap(_render_operation)
    (header ++ operations ++ Vector("")).filter(_.nonEmpty)
  }

  private def _render_operation(op: OperationDefinition): Vector[String] = {
    val header = Vector(s"Operation: ${op.name}")
    val params = _render_parameters(op.specification.request.parameters)
    val note = Vector("Arguments can be specified by position or by name (--arg).")
    (header ++ params ++ note).filter(_.nonEmpty)
  }

  private def _render_parameters(params: List[ParameterDefinition]): Vector[String] = {
    val args = params.filter(_.kind == ParameterDefinition.Kind.Argument)
    val props = params.filter(_.kind == ParameterDefinition.Kind.Property)
    val switches = params.filter(_.kind == ParameterDefinition.Kind.Switch)

    val argLines =
      if (args.nonEmpty)
        Vector("Arguments:") ++ args.map(_render_argument)
      else
        Vector("Arguments: (none)")

    val optLines =
      if (props.nonEmpty || switches.nonEmpty)
        Vector("Options:") ++ props.map(_render_property) ++ switches.map(_render_switch)
      else
        Vector("Options: (none)")

    argLines ++ optLines
  }

  private def _render_argument(p: ParameterDefinition): String = {
    val required = if (_is_required(p)) "required" else "optional"
    s"- ${p.name}: ${_datatype_name(p)} (${required})"
  }

  private def _render_property(p: ParameterDefinition): String =
    s"--${p.name} <${_datatype_name(p)}>"

  private def _render_switch(p: ParameterDefinition): String =
    s"--${p.name}"

  private def _datatype_name(p: ParameterDefinition): String =
    p.datatype.name

  private def _is_required(p: ParameterDefinition): Boolean = p.multiplicity match {
    case Multiplicity.One => true
    case Multiplicity.OneMore => true
    case Multiplicity.Range(from, _) => from > 0
    case Multiplicity.Ranges(ranges) => ranges.exists(_.head.from > 0)
    case _ => false
  }
}
