package org.goldenport.observation

import cats.data.NonEmptyVector
import java.net.URI
import org.goldenport.schema.DataType
import org.goldenport.schema.Constraint
import org.goldenport.text.Presentable
import org.goldenport.util.StringUtils

/**
 * Structural description of where and how a phenomenon was observed.
 * Interpretation-free, value-agnostic.
 */
/*
 * @since   Dec. 28, 2025
 *  version Jan. 31, 2026
 * @version Feb.  5, 2026
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
}

object Descriptor {
  val empty = Descriptor()

  sealed abstract class Facet extends Presentable {
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
    }
    object Parameter {
      enum Kind { case Argument, Property, Switch }

      def argument(name: String) = Parameter(Kind.Argument, name)
    }

    case class Resource(
      kind: Resource.Kind,
      uri: URI
    ) extends Facet {
      def print: String = s"resource:${Presentable.print(uri)}}"
    }

    object Resource {
      enum Kind { case File, Database }
    }

    case class Line(no: Int) extends Facet {
      def print: String = s"line:${no}"
    }

    case class Message(message: String) extends Facet {
      def print: String = s"message:${message}"
    }

    case class Key(key: String) extends Facet {
      def print: String = s"key:${key}"
    }

    case class Value(value: Any) extends Facet {
      def print: String = s"value:${Presentable.print(value)}"
    }

    case class Id(id: String) extends Facet {
      def print: String = s"id:${id}"
    }

    case class State(state: String) extends Facet {
      def print: String = s"id:${state}"
    }

    case class Exception(e: Throwable) extends Facet {
      def print: String = s"exception:${e}"
    }

    case class Properties(properties: Map[String, String]) extends Facet {
      def print: String = s"properties:${Presentable.print(properties)}"
    }

    case class DataType(datatype: org.goldenport.schema.DataType) extends Facet {
      def print: String = s"datatype:${datatype.print}"
    }

    case class Constraint(constraints: NonEmptyVector[org.goldenport.schema.Constraint]) extends Facet {
      def print: String = s"constraint:${Presentable.print(constraints)}"
    }

    case class Args(args: Seq[String]) extends Facet {
      def print: String = s"""args:${Presentable.print(args)}"""
    }

    case class SrcPos(pos: SourcePosition) extends Facet {
      def print: String = s"""source:${pos.print}"""
      override def show: String = s"""source:${pos.show}"""
    }

    case class Record(record: org.goldenport.record.Record) extends Facet {
      def print: String = s"""record:${record.print}"""
      override def show: String = s"""record:${record.show}"""
    }

    // Legacy
    case class File(
      path: Option[URI] = None,
      operation: Option[String] = None
    ) extends Facet {
      def print: String = "not supported yet"
    }

    case class Database(
      database: Option[String] = None,
      table: Option[String] = None,
      operation: Option[String] = None
    ) extends Facet {
      def print: String = "not supported yet"
    }

    case class ExceptionLegacy(
      className: String,
      message: Option[String] = None,
      stackTrace: Option[Vector[String]] = None
    ) extends Facet {
      def print: String = "not supported yet"
    }

    case class Operation(
      name: String,
      phase: Option[String] = None
    ) extends Facet {
      def print: String = "not supported yet"
    }

    case class Input(
      name: Option[String] = None,
      value: Option[String] = None
    ) extends Facet {
      def print: String = "not supported yet"
    }
  }

  def apply(p: Facet): Descriptor = Descriptor(Vector(p))

  def apply(p: Facet, ps: Seq[Facet]): Descriptor =
    Descriptor((p +: ps).toVector)

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
