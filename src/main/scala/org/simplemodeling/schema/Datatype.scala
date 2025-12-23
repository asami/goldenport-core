package org.simplemodeling.schema

/*
 * Datatype reference used by schema definitions.
 *
 * This is intentionally a lightweight, declarative identifier.
 * Concrete value objects live in `org.simplemodeling.datatype` and may be
 * mapped by CNCF/runtime layers as needed.
 */
sealed trait Datatype {
  def name: String
}

object Datatype {

  /*
   * Declares that a field uses a value object datatype identified by name.
   *
   * Example:
   *   ValueObject("StringValue")
   *   ValueObject("Locale")
   */
  final case class ValueObject(name: String) extends Datatype
}
