package org.goldenport.datatype

/*
 * @since   Aug.  2, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
case class I18nSummary(value: I18nString = I18nString("")) {
  def toI18nString: I18nString = value
}

object I18nSummary {
  def apply(p: String): I18nSummary = I18nSummary(I18nString(p))
}
