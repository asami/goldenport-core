package org.goldenport.datatype

/*
 * @since   Jul. 23, 2025
 *  version Jul. 23, 2025
 * @version Aug.  2, 2025
 * @author  ASAMI, Tomoharu
 */
abstract class PositiveInt() extends IntDataType() {
  import PositiveInt._

  override protected final def value_min: Int = 1
  override protected final def is_valid(p: Int): Boolean = true
}

object PositiveInt {
  case class Instance(value: Int) extends PositiveInt() {
  }
}
