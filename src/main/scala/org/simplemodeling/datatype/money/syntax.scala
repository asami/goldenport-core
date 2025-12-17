package org.simplemodeling.datatype.money

import java.util.Currency

/*
 * @since   Jul. 20, 2025
 * @version Jul. 20, 2025
 * @author  ASAMI, Tomoharu
 */
object syntax:
  extension [C](m: Money[C])(using tag: CurrencyTag[C])
    def currency: Currency = tag.currency
    def toValue: MoneyValue = MoneyValue.from(m)
