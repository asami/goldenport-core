package org.simplemodeling.datatype

import org.simplemodeling.Consequence

/*
 * @since   Jul. 23, 2025
 * @version Jul. 23, 2025
 * @author  ASAMI, Tomoharu
 */
abstract class Token() extends StringDataType() {
  import Token._

  protected final def length_min: Int = LENGTH_MIN
  protected final def length_max: Int = LENGTH_MAX
  protected final def is_valid(s: String): Boolean = validate_token(s)
}

object Token {
  val LENGTH_MIN = 1
  val LENGTH_MAX = 64

  case class Instance(value: String) extends Token() {
  }

  def create(s: String): Consequence[Token] = Consequence(Instance(s))
}
