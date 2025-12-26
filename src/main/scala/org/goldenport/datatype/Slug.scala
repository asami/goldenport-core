package org.goldenport.datatype

import org.goldenport.Consequence

/*
 * @since   Aug.  2, 2025
 * @version Aug.  2, 2025
 * @author  ASAMI, Tomoharu
 */
abstract class Slug() extends StringDataType() {
  import Slug._

  protected final def length_min: Int = LENGTH_MIN
  protected final def length_max: Int = LENGTH_MAX
  protected final def is_valid(s: String): Boolean = validate_printable(s)
}

object Slug {
  val LENGTH_MIN = 1
  val LENGTH_MAX = 64 // TODO

  case class Instance(value: String) extends Slug() {
  }

  def create(s: String): Consequence[Slug] = Consequence(Instance(s))
}
