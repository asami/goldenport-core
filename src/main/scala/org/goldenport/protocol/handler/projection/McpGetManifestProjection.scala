package org.goldenport.protocol.handler.projection

import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.protocol.spec.{OperationDefinition, ParameterDefinition, ServiceDefinition, ServiceDefinitionGroup}
import org.goldenport.protocol.spec.ParameterDefinition.Default
import org.goldenport.schema.Multiplicity

/*
 * @since   Dec. 30, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
case object McpGetManifestProjectionKind extends ProjectionKind[Json] {
  val name = "mcp-get-manifest"
}

class McpGetManifestProjection extends Projection[Json] {
  val kind: ProjectionKind[Json] = McpGetManifestProjectionKind

  def project(defs: ServiceDefinitionGroup): Consequence[Json] =
    Consequence.success(_render_manifest(defs))

  private def _render_manifest(defs: ServiceDefinitionGroup): Json = {
    val service = defs.services.headOption
    val serviceJson = service.map(_render_service).getOrElse(Json.obj())
    val opsJson = service.map(s => Json.arr(s.operations.operations.toVector.map(_render_operation)*))
      .getOrElse(Json.arr())
    Json.obj(
      "service" -> serviceJson,
      "operations" -> opsJson
    )
  }

  private def _render_service(service: ServiceDefinition): Json = {
    val m = service.metadata
    val base = Vector(
      Some("name" -> Json.fromString(service.name)),
      m.author.map(v => "author" -> Json.fromString(v)),
      m.homepage.map(v => "homepage" -> Json.fromString(v.toString)),
      m.license.map(v => "license" -> Json.fromString(v)),
      m.descriptionLong.map(v => "description_long" -> Json.fromString(v))
    ).flatten
    Json.obj(base*)
  }

  private def _render_operation(op: OperationDefinition): Json = {
    val params = op.specification.request.parameters.map(_render_parameter)
    Json.obj(
      "name" -> Json.fromString(op.name),
      "parameters" -> Json.arr(params*)
    )
  }

  private def _render_parameter(p: ParameterDefinition): Json = {
    val kind = p.kind match {
      case ParameterDefinition.Kind.Argument => "argument"
      case ParameterDefinition.Kind.Property => "property"
      case ParameterDefinition.Kind.Switch => "switch"
    }
    val base = Vector(
      "name" -> Json.fromString(p.name),
      "kind" -> Json.fromString(kind),
      "datatype" -> Json.fromString(p.datatype.name),
      "required" -> Json.fromBoolean(_is_required(p)),
      "multiple" -> Json.fromBoolean(_allows_multiple(p))
    )
    val default = _render_default(p).map("default" -> _)
    val constraints = _render_constraints(p).map("constraints" -> _)
    Json.obj((base ++ default ++ constraints)*)
  }

  private def _render_default(p: ParameterDefinition): Option[Json] =
    p.default match {
      case Default.Value(v) => Some(_literal_json(v))
      case Default.Empty => Some(Json.obj("kind" -> Json.fromString("empty")))
      case Default.Undefined => None
    }

  private def _render_constraints(p: ParameterDefinition): Option[Json] =
    if (p.constraints.nonEmpty)
      Some(Json.obj("opaque" -> Json.fromBoolean(true)))
    else
      None

  private def _literal_json(value: Any): Json = value match {
    case v: String => Json.fromString(v)
    case v: Boolean => Json.fromBoolean(v)
    case v: Int => Json.fromInt(v)
    case v: Long => Json.fromLong(v)
    case v: BigInt => Json.fromBigInt(v)
    case v: BigDecimal => Json.fromBigDecimal(v)
    case v => Json.fromString(v.toString)
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
}
