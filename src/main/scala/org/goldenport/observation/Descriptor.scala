package org.goldenport.observation

/*
 * @since   Dec. 28, 2025
 * @version Dec. 28, 2025
 * @author  ASAMI, Tomoharu
 */
case class Descriptor(
  aspects: Vector[Descriptor.Aspect] = Vector.empty
)

object Descriptor {

  sealed trait Aspect

  object Aspect {
    case class File(
      path: Option[String] = None,
      operation: Option[String] = None
    ) extends Aspect

    case class Database(
      database: Option[String] = None,
      table: Option[String] = None,
      operation: Option[String] = None
    ) extends Aspect

    case class Exception(
      className: String,
      message: Option[String] = None,
      stackTrace: Option[Vector[String]] = None
    ) extends Aspect

    case class Operation(
      name: String,
      phase: Option[String] = None
    ) extends Aspect

    case class Input(
      name: Option[String] = None,
      value: Option[String] = None
    ) extends Aspect
  }
}
