package org.goldenport.observation

import cats.data.NonEmptyVector
import java.net.URI
import org.goldenport.schema.DataType
import org.goldenport.schema.Constraint
import org.goldenport.text.Presentable

/**
 * Structural description of where and how a phenomenon was observed.
 * Interpretation-free, value-agnostic.
 */
/*
 * @since   Dec. 28, 2025
 * @version Jan. 29, 2026
 * @author  ASAMI, Tomoharu
 */
case class Descriptor(
  facets: Vector[Descriptor.Facet] = Vector.empty
) {
  import Descriptor.*

  def display: String = facets.map(_.display).mkString(";")

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

  sealed abstract class Facet {
    def display: String
  }

  object Facet {
    case class Parameter(
      kind: Parameter.Kind,
      name: String
    ) extends Facet {
      import Parameter.Kind._

      def display: String = kind match {
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
      def display: String = s"resource:${Presentable.display(uri)}}"
    }

    object Resource {
      enum Kind { case File, Database }
    }

    case class Line(no: Int) extends Facet {
      def display: String = s"line:${no}"
    }


    case class Message(message: String) extends Facet {
      def display: String = s"message:${message}"
    }


    case class Value(value: Any) extends Facet {
      def display: String = s"value:${Presentable.display(value)}"
    }


    case class Id(id: String) extends Facet {
      def display: String = s"id:${id}"
    }


    case class State(state: String) extends Facet {
      def display: String = s"id:${state}"
    }


    case class Exception(e: Throwable) extends Facet {
      def display: String = s"exception:${e}"
    }


    case class Properties(properties: Map[String, String]) extends Facet {
      def display: String = s"properties:${Presentable.display(properties)}"
    }


    case class DataType(datatype: org.goldenport.schema.DataType) extends Facet {
      def display: String = s"datatype:${datatype.display}"
    }


    case class Constraint(constraints: NonEmptyVector[org.goldenport.schema.Constraint]) extends Facet {
      def display: String = s"constraint:${Presentable.display(constraints)}"
    }

    case class Args(args: Seq[String]) extends Facet {
      def display: String = s"""args:${Presentable.display(args)}"""
    }

    // Legacy
    case class File(
      path: Option[URI] = None,
      operation: Option[String] = None
    ) extends Facet {
      def display: String = "not supported yet"
    }

    case class Database(
      database: Option[String] = None,
      table: Option[String] = None,
      operation: Option[String] = None
    ) extends Facet {
      def display: String = "not supported yet"
    }

    case class ExceptionLegacy(
      className: String,
      message: Option[String] = None,
      stackTrace: Option[Vector[String]] = None
    ) extends Facet {
      def display: String = "not supported yet"
    }

    case class Operation(
      name: String,
      phase: Option[String] = None
    ) extends Facet {
      def display: String = "not supported yet"
    }

    case class Input(
      name: Option[String] = None,
      value: Option[String] = None
    ) extends Facet {
      def display: String = "not supported yet"
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
