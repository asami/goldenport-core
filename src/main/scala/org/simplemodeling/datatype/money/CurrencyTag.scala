package org.simplemodeling.datatype.money

import java.util.Currency

/*
 * @since   Jul. 20, 2025
 * @version Jul. 20, 2025
 * @author  ASAMI, Tomoharu
 */
trait CurrencyTag[C]:
  def currency: Currency

object CurrencyTag:
  inline def apply[C](using tag: CurrencyTag[C]): Currency = tag.currency
