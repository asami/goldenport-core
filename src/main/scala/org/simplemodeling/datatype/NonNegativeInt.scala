package org.goldenport.datatype

/*
 * @since   Jul. 23, 2025
 *  version Jul. 23, 2025
 *  version Aug.  2, 2025
 * @version Sep. 17, 2025
 * @author  ASAMI, Tomoharu
 */
abstract class NonNegativeInt() extends IntDataType() {
  import NonNegativeInt._

  override protected final def value_min: Int = MIN_DEFAULT
  override protected final def is_valid(p: Int): Boolean = true
}

object NonNegativeInt {
  final val MIN_DEFAULT = 0

  case class Instance(value: Int) extends NonNegativeInt() {
  }
}
