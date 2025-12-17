package org.simplemodeling.datatype

/*
 * @since   Jul. 20, 2025
 * @version Jul. 23, 2025
 * @author  ASAMI, Tomoharu
 */
final case class Rate(value: BigDecimal) extends DataType() {
  require(value >= 0 && value <= 1, s"Rate must be between 0 and 1: $value")

  // def of[C](money: Money[C]): Money[C] =
  //   Money(money.amount * value)

  def +(other: Rate): Rate = Rate(this.value + other.value)
  def -(other: Rate): Rate = Rate(this.value - other.value)

  def inverse: BigDecimal = 1 + value

  override def toString: String = f"${(value * 100)}%.2f %%"
}
