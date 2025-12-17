package org.simplemodeling.datatype.money

import java.util.Currency

/*
 * @since   Jul. 20, 2025
 * @version Jul. 20, 2025
 * @author  ASAMI, Tomoharu
 */
case class MoneyValue(amount: BigDecimal, currency: Currency)

object MoneyValue:
  def from[C](m: Money[C])(using tag: CurrencyTag[C]): MoneyValue =
    MoneyValue(m.amount, tag.currency)

