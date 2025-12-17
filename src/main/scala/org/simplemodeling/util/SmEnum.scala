package org.simplemodeling.util

/*
 * @since   Jul. 19, 2025
 * @version Jul. 19, 2025
 * @author  ASAMI, Tomoharu
 */
trait SmEnum { self: Product =>
  val label: String = StringUtils.camelToSnake(self.productPrefix)

  override def toString(): String = label
}

trait SmEnumClass[E <: Enum[E]] {
  def values: Array[E]

  def from(str: String): Option[E] =
    val normalized = str.toLowerCase
    values.find(e => _to_snake_case(e.toString) == normalized)

  private def _to_snake_case(s: String): String =
    StringUtils.camelToSnake(s)
}
