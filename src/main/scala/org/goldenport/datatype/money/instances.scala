package org.goldenport.datatype.money

import scala.math.Numeric

/*
 * @since   Jul. 20, 2025
 * @version Jul. 20, 2025
 * @author  ASAMI, Tomoharu
 */
object instances:
  given [C]: Numeric[Money[C]] with
    def plus(x: Money[C], y: Money[C]) = x + y
    def minus(x: Money[C], y: Money[C]) = x - y
    def times(x: Money[C], y: Money[C]) = Money[C](x.amount * y.amount)
    def negate(x: Money[C]) = Money[C](-x.amount)
    def fromInt(x: Int) = Money[C](BigDecimal(x))
    def parseString(str: String) = scala.util.Try(Money[C](BigDecimal(str))).toOption
    def toInt(x: Money[C]) = x.amount.toInt
    def toLong(x: Money[C]) = x.amount.toLong
    def toFloat(x: Money[C]) = x.amount.toFloat
    def toDouble(x: Money[C]) = x.amount.toDouble
    def compare(x: Money[C], y: Money[C]): Int = x.amount.compare(y.amount)
