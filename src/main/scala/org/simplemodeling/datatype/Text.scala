package org.simplemodeling.datatype

/*
 * @since   Jul. 23, 2025
 * @version Jul. 23, 2025
 * @author  ASAMI, Tomoharu
 */
abstract class Text() extends StringDataType() {
  import Text._

  protected final def length_min: Int = LENGTH_MIN
  protected final def length_max: Int = LENGTH_MAX
  protected final def is_valid(s: String): Boolean = validate_printable(s)
}

object Text {
  val LENGTH_MIN = 0
  val LENGTH_MAX = 8192

  case class Instance(value: String) extends Text() {
  }
}
