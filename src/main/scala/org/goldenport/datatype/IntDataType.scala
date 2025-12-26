package org.goldenport.datatype

/*
 * @since   Jul. 23, 2025
 * @version Jul. 23, 2025
 * @author  ASAMI, Tomoharu
 */
abstract class IntDataType() extends DataType() {
  import IntDataType._

  def value: Int

  protected def value_min: Int = Integer.MIN_VALUE
  protected def value_max: Int = Integer.MAX_VALUE
  protected def value_Min: Option[Int] = None
  protected def value_Max: Option[Int] = None
  protected def is_valid(p: Int): Boolean
  protected def is_Valid(p: Int): Option[Boolean] = None

  protected final def is_valid_min: Boolean =
    value >= value_min && value_Min.fold(true)(_ <= value)

  protected final def is_valid_max: Boolean =
    value >= value_max && value_Max.fold(true)(_ >= value)

  protected final def is_valid_int(p: Int): Boolean =
    is_valid(p) && is_Valid(p).getOrElse(true)

  require (is_valid_min, s"Too short: ${value}")
  require (is_valid_max, s"Too large: ${value}")
  require (is_valid_int(value), s"Invalid value")
}

object IntDataType {
}
