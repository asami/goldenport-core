package org.goldenport.protocol.handler.projection

import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.protocol.spec.{OperationDefinition, ParameterDefinition, ServiceDefinition, ServiceDefinitionGroup}
import org.goldenport.protocol.spec.ParameterDefinition.Default
import org.goldenport.schema.*

/*
 * @since   Dec. 30, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
case object OpenApi extends ProjectionKind[Json] {
  val name = "openapi"
}

class OpenApiProjection extends Projection[Json] {
  val kind: ProjectionKind[Json] = OpenApi

  def project(defs: ServiceDefinitionGroup): Consequence[Json] =
    Consequence.success(
      Json.obj(
        "openapi" -> Json.fromString("3.0.0"),
        "info" -> Json.obj(
          "title" -> Json.fromString("Protocol"),
          "version" -> Json.fromString("0.1.0")
        ),
        "paths" -> _render_paths(defs)
      )
    )

  private def _render_paths(defs: ServiceDefinitionGroup): Json = {
    val entries = defs.services.flatMap { service =>
      service.operations.operations.toVector.map { op =>
        val path = s"/${service.name}/${op.name}"
        path -> Json.obj(
          "post" -> _render_operation(op)
        )
      }
    }
    Json.obj(entries*)
  }

  private def _render_operation(op: OperationDefinition): Json = {
    val params = _render_query_parameters(op.specification.request.parameters)
    val requestBody = _render_request_body(op.specification.request.parameters)
    Json.obj(
      "summary" -> Json.fromString(op.name),
      "parameters" -> Json.arr(params*),
      "requestBody" -> requestBody,
      "responses" -> Json.obj(
        "200" -> Json.obj("description" -> Json.fromString("Success")),
        "400" -> Json.obj("description" -> Json.fromString("Client Error")),
        "500" -> Json.obj("description" -> Json.fromString("Server Error"))
      )
    )
  }

  private def _render_query_parameters(
    params: List[ParameterDefinition]
  ): Vector[Json] =
    params.collect {
      case p if p.kind == ParameterDefinition.Kind.Property =>
        val base = Vector(
          "name" -> Json.fromString(p.name),
          "in" -> Json.fromString("query"),
          "required" -> Json.fromBoolean(_is_required(p)),
          "schema" -> _schema_for(p)
        )
        Json.obj(base*)
      case p if p.kind == ParameterDefinition.Kind.Switch =>
        val base = Vector(
          "name" -> Json.fromString(p.name),
          "in" -> Json.fromString("query"),
          "required" -> Json.fromBoolean(false),
          "schema" -> _schema_for(p, forceBoolean = true)
        )
        Json.obj(base*)
    }.toVector

  private def _render_request_body(
    params: List[ParameterDefinition]
  ): Json = {
    val args = params.filter(_.kind == ParameterDefinition.Kind.Argument)
    val properties = args.map { p =>
      p.name -> _schema_for(p)
    }
    Json.obj(
      "content" -> Json.obj(
        "application/json" -> Json.obj(
          "schema" -> Json.obj(
            "type" -> Json.fromString("object"),
            "properties" -> Json.obj(properties*)
          )
        )
      )
    )
  }

  private def _schema_for(
    p: ParameterDefinition,
    forceBoolean: Boolean = false
  ): Json = {
    val base = Vector("type" -> Json.fromString(_type_name(p, forceBoolean)))
    val format = _format_name(p, forceBoolean).map(v => "format" -> Json.fromString(v))
    val defaults = _render_default(p)
    val constraints = _render_constraints(p)
    Json.obj((base ++ format ++ defaults ++ constraints)*)
  }

  private def _type_name(p: ParameterDefinition, forceBoolean: Boolean): String =
    if (forceBoolean) "boolean"
    else
      p.datatype match {
        case XString => "string"
        case XNonNegativeInteger => "integer"
        case XPositiveInteger => "integer"
        case XDateTime => "string"
        case XLocalDateTime => "string"
        case XYearMonth => "string"
        case _ => "string"
      }

  private def _format_name(p: ParameterDefinition, forceBoolean: Boolean): Option[String] =
    if (forceBoolean) None
    else
      p.datatype match {
        case XDateTime => Some("date-time")
        case XLocalDateTime => Some("date-time")
        case XYearMonth => Some("year-month")
        case _ => None
      }

  private def _render_default(p: ParameterDefinition): Vector[(String, Json)] =
    p.default match {
      case Default.Value(v) => Vector("default" -> _literal_json(v))
      case Default.Empty => Vector("x-default-kind" -> Json.fromString("empty"))
      case Default.Undefined => Vector.empty
    }

  private def _render_constraints(p: ParameterDefinition): Vector[(String, Json)] =
    if (p.constraints.nonEmpty)
      Vector("x-constraints" -> Json.fromString("opaque"))
    else
      Vector.empty

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
}
