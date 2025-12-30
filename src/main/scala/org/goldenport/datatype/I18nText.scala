package org.goldenport.datatype

/*
 * @since   Aug.  2, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
case class I18nText(value: I18nString = I18nString("")) {
  def toI18nString: I18nString = value
}

object I18nText {
  def apply(p: String): I18nText = I18nText(I18nString(p))
}
