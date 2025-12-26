package org.goldenport.datatype.money

import java.util.Currency

/*
 * @since   Jul. 20, 2025
 * @version Jul. 20, 2025
 * @author  ASAMI, Tomoharu
 */
object USD
object EUR
object JPY

given CurrencyTag[USD.type] with {
  def currency: Currency = Currency.getInstance("USD")
}

given CurrencyTag[EUR.type] with {
  def currency: Currency = Currency.getInstance("EUR")
}

given CurrencyTag[JPY.type] with {
  def currency: Currency = Currency.getInstance("JPY")
}
