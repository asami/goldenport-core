package org.simplemodeling.datatype.money

/*
 * @since   Jul. 20, 2025
 * @version Jul. 20, 2025
 * @author  ASAMI, Tomoharu
 */
opaque type Money[C] = BigDecimal

object Money:
  def apply[C](amount: BigDecimal): Money[C] = amount

  extension [C](m: Money[C])
    def amount: BigDecimal = m
    def +(that: Money[C]): Money[C] = m + that
    def -(that: Money[C]): Money[C] = m - that
    def *(factor: BigDecimal): Money[C] = m * factor
    def /(divisor: BigDecimal): Money[C] = m / divisor
