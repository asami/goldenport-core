package org.goldenport.observation

import cats._
import cats.implicits._
import cats.data.NonEmptyVector
import java.net.URI
import org.goldenport.text.Presentable
import org.goldenport.datatype.Identifier
import org.goldenport.id.UniversalId
import org.goldenport.record.Record
import org.goldenport.schema.DataType
import org.goldenport.schema.Constraint
import org.goldenport.util.SmEnum
import org.goldenport.util.StringUtils

/**
 * Structural description of where and how a phenomenon was observed.
 * Interpretation-free, value-agnostic.
 */
/*
 * @since   Dec. 28, 2025
 *  version Jan. 31, 2026
 *  version Feb. 25, 2026
 *  version Mar. 13, 2026
 * @version Apr. 29, 2026
 * @author  ASAMI, Tomoharu
 */
case class Descriptor(
  facets: Vector[Descriptor.Facet] = Vector.empty
) extends Presentable {
  import Descriptor.*

  def print: String = facets.map(_.print).mkString(";")
  override def display: String = facets.map(_.display).mkString(";")
  override def show: String = facets.map(_.show).mkString(";")

  def add(p: Descriptor.Facet) = copy(facets = facets :+ p)

  def getEffectiveMessage: Option[String] =
    getMessage orElse getException.flatMap(e => Option(e.getMessage))

  def getMessage: Option[String] =
    facets.collect {
      case Facet.Message(msg) => msg
    }.headOption

  def getException: Option[Throwable] =
    facets.collect {
      case Facet.Exception(e) => e
    }.headOption

  def getDataType: Option[DataType] =
    facets.collect {
      case Facet.DataType(d) => d
    }.headOption

  def getConstraints: Option[NonEmptyVector[Constraint]] =
    facets.collect {
      case Facet.Constraint(cs) => cs
    }.headOption

  def toRecord: Record = facets.foldMap(_.toRecord)
}

object Descriptor {
  val empty = Descriptor()

  sealed abstract class Facet extends Presentable {
    def toRecord: Record
  }

  object Facet {
    case class Parameter(
      kind: Parameter.Kind,
      name: String
    ) extends Facet {
      import Parameter.Kind._

      def print: String = kind match {
        case Argument => s"argument:${name}"
        case Property => s"property:${name}"
        case Switch => s"switch:${name}"
      }

      def toRecord: Record = Record.data(
        "parameter" -> Record.data(
          "kind" -> kind.value,
          "name" -> name
        )
      )
    }
    object Parameter {
      enum Kind extends SmEnum { case Argument, Property, Switch }

      def argument(name: String) = Parameter(Kind.Argument, name)
    }

    case class Resource(
      kind: Resource.Kind,
      uri: URI
    ) extends Facet {
      def print: String = s"resource:${kind}:${Presentable.print(uri)}}"

      def toRecord: Record = Record.data(
        "resource" -> Record.data(
          "kind" -> kind.toString.toLowerCase,
          "uri" -> uri.toString
        )
      )
    }
    object Resource {
      enum Kind { case File, Database }
    }

    case class Reference(
      role: Reference.Role,
      id: UniversalId
    ) extends Facet {
      def print = s"ref:${role}:${id.print}"

      def toRecord: Record = Record.data(
        "reference" -> Record.data(
          "role" -> role.toString.toLowerCase,
          "id" -> id.print
        )
      )
    }
    object Reference {
      enum Role { case Participant, Target, Context }
    }

    case class Line(no: Int) extends Facet {
      def print: String = s"line:${no}"

      def toRecord: Record = Record.data("line" -> no)
    }

    case class Message(message: String) extends Facet {
      def print: String = s"message:${message}"

      def toRecord: Record = Record.data("message" -> message)
    }

    case class Key(key: String) extends Facet {
      def print: String = s"key:${key}"

      def toRecord: Record = Record.data("key" -> key)
    }

    case class Value(value: Any) extends Facet {
      def print: String = s"value:${Presentable.print(value)}"

      def toRecord: Record = Record.data("value" -> value)
    }

    case class FieldPath(path: String) extends Facet {
      def print: String = s"field-path:${path}"

      def toRecord: Record = Record.data("fieldPath" -> path)
    }

    case class Expected(value: Any) extends Facet {
      def print: String = s"expected:${Presentable.print(value)}"

      def toRecord: Record = Record.data("expected" -> value)
    }

    case class Actual(value: Any) extends Facet {
      def print: String = s"actual:${Presentable.print(value)}"

      def toRecord: Record = Record.data("actual" -> value)
    }

    case class Limit(value: Any) extends Facet {
      def print: String = s"limit:${Presentable.print(value)}"

      def toRecord: Record = Record.data("limit" -> value)
    }

    case class Policy(name: String) extends Facet {
      def print: String = s"policy:${name}"

      def toRecord: Record = Record.data("policy" -> name)
    }

    case class Algorithm(name: String) extends Facet {
      def print: String = s"algorithm:${name}"

      def toRecord: Record = Record.data("algorithm" -> name)
    }

    case class Reason(name: String) extends Facet {
      def print: String = s"reason:${name}"

      def toRecord: Record = Record.data("reason" -> name)
    }

    case class Capability(name: String) extends Facet {
      def print: String = s"capability:${name}"

      def toRecord: Record = Record.data("capability" -> name)
    }

    case class Permission(name: String) extends Facet {
      def print: String = s"permission:${name}"

      def toRecord: Record = Record.data("permission" -> name)
    }

    case class Guard(name: String) extends Facet {
      def print: String = s"guard:${name}"

      def toRecord: Record = Record.data("guard" -> name)
    }

    case class Relation(name: String) extends Facet {
      def print: String = s"relation:${name}"

      def toRecord: Record = Record.data("relation" -> name)
    }

    case class Id(id: String) extends Facet {
      def print: String = s"id:${id}"

      def toRecord: Record = Record.data("id" -> id)
    }
    object Id {
      def apply(id: UniversalId): Id = Id(id.print)
      def apply(id: Identifier): Id = Id(id.print)
    }

    case class Name(name: String) extends Facet {
      def print: String = s"name:${name}"

      def toRecord: Record = Record.data("name" -> name)
    }

    case class State(state: String) extends Facet {
      def print: String = s"id:${state}"

      def toRecord: Record = Record.data("state" -> state)
    }

    case class Exception(e: Throwable) extends Facet {
      def print: String = s"exception:${e}"

      def toRecord: Record = Record.data(
        "exception" -> Record.dataOption(
          "class" -> Some(e.getClass.getName),
          "message" -> Option(e.getMessage)
        )
      )
    }

    case class Properties(properties: Map[String, String]) extends Facet {
      def print: String = s"properties:${Presentable.print(properties)}"

      def toRecord: Record = Record.data("properties" -> Record.create(properties))
    }

    case class DataType(datatype: org.goldenport.schema.DataType) extends Facet {
      def print: String = s"datatype:${datatype.print}"

      def toRecord: Record = Record.data("datatype" -> datatype.print)
    }

    case class Constraint(constraints: NonEmptyVector[org.goldenport.schema.Constraint]) extends Facet {
      def print: String = s"constraint:${Presentable.print(constraints)}"

      def toRecord: Record = Record.data("constraint" -> constraints.toVector.map(_.toString))
    }

    case class Args(args: Seq[String]) extends Facet {
      def print: String = s"""args:${Presentable.print(args)}"""

      def toRecord: Record = Record.data("args" -> args.toVector)
    }

    case class SrcPos(pos: SourcePosition) extends Facet {
      def print: String = s"""source:${pos.print}"""
      override def show: String = s"""source:${pos.show}"""

      def toRecord: Record = Record.data(
        "source" -> Record.data(
          "file" -> pos.file,
          "line" -> pos.line,
          "column" -> pos.column
        )
      )
    }

    case class RecordData(record: Record) extends Facet {
      def print: String = s"""record:${record.print}"""
      override def show: String = s"""record:${record.show}"""

      def toRecord: Record = Record.data("record" -> record)
    }

    case class Component(name: String) extends Facet {
      def print: String = s"""component:${name}"""
      override def show: String = s"""component:${name}"""

      def toRecord: Record = Record.data("component" -> name)
    }

    case class Service(name: String) extends Facet {
      def print: String = s"""service:${name}"""
      override def show: String = s"""service:${name}"""

      def toRecord: Record = Record.data("service" -> name)
    }

    case class Operation(name: String) extends Facet {
      def print: String = s"""operation:${name}"""
      override def show: String = s"""operation:${name}"""

      def toRecord: Record = Record.data("operation" -> name)
    }

    case class ClassName(name: String) extends Facet {
      def print: String = s"""class-name:${name}"""
      override def show: String = s"""class-name:${name}"""

      def toRecord: Record = Record.data("class-name" -> name)
    }

    case class Artifact(identity: String) extends Facet {
      def print: String = s"""artifact:${identity}"""
      override def show: String = s"""artifact:${identity}"""

      def toRecord: Record = Record.data("artifact" -> identity)
    }

    case class RepositoryType(name: String) extends Facet {
      def print: String = s"""repository-type:${name}"""
      override def show: String = s"""repository-type:${name}"""

      def toRecord: Record = Record.data("repository-type" -> name)
    }

    // Legacy
    case class File(
      path: Option[URI] = None,
      operation: Option[String] = None
    ) extends Facet {
      def print: String = "not supported yet"

      def toRecord: Record = Record.data(
        "file" -> Record.dataOption(
          "path" -> path.map(_.toString),
          "operation" -> operation
        )
      )
    }

    case class Database(
      database: Option[String] = None,
      table: Option[String] = None,
      operation: Option[String] = None
    ) extends Facet {
      def print: String = "not supported yet"

      def toRecord: Record = Record.data(
        "database" -> Record.dataOption(
          "database" -> database,
          "table" -> table,
          "operation" -> operation
        )
      )
    }

    case class ExceptionLegacy(
      className: String,
      message: Option[String] = None,
      stackTrace: Option[Vector[String]] = None
    ) extends Facet {
      def print: String = "not supported yet"

      def toRecord: Record = Record.data(
        "exception-legacy" -> Record.dataOption(
          "class-name" -> Some(className),
          "message" -> message,
          "stack-trace" -> stackTrace.map(_.mkString("\n"))
        )
      )
    }

    // case class Operation(
    //   name: String,
    //   phase: Option[String] = None
    // ) extends Facet {
    //   def print: String = "not supported yet"

    //   def toRecord: Record = Record.data(
    //     "operation" -> Record.dataOption(
    //       "name" -> Some(name),
    //       "phase" -> phase
    //     )
    //   )
    // }

    case class Input(
      name: Option[String] = None,
      value: Option[String] = None
    ) extends Facet {
      def print: String = "not supported yet"

      def toRecord: Record = Record.data(
        "input" -> Record.dataOption(
          "name" -> name,
          "value" -> value
        )
      )
    }

    case class HttpMethod(method: String) extends Facet {
      def print: String = s"http_method:${method}"

      def toRecord: Record = Record.data("http-method" -> method)
    }

    case class HttpUrl(url: String) extends Facet {
      def print: String = s"http_url:${url}"

      def toRecord: Record = Record.data("http-url" -> url)
    }

    case class HttpStatus(code: Int) extends Facet {
      def print: String = s"http_status:${code}"

      def toRecord: Record = Record.data("http-status" -> code)
    }
  }

  def apply(p: Facet): Descriptor = Descriptor(Vector(p))

  def apply(p: Facet, ps: Seq[Facet]): Descriptor =
    Descriptor((p +: ps).toVector)

  def apply(ps: Seq[Facet]): Descriptor = Descriptor(ps.toVector)

  def from(e: Throwable): Descriptor = Descriptor(Facet.Exception(e))

  def message(msg: String): Descriptor = Descriptor(Facet.Message(msg))

  def argumentOperation(name: String, operation: String): Descriptor =
    Descriptor(Vector(Facet.Parameter.argument(name), Facet.Operation(operation)))

  def operationInput(operation: String, args: Seq[String]): Descriptor =
    Descriptor(Vector(Facet.Operation(operation), Facet.Args(args)))
}

case class SourcePosition(
  file: String,
  line: Int,
  column: Int
) extends Presentable {
  def print = s"$file[$line,$column]"
  override def show = s"${StringUtils.pathLeaf(file)}[$line,$column]"
}
