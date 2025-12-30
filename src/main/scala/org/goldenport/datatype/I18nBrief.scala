package org.goldenport.datatype

/*
 * @since   Aug.  2, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
case class I18nBrief(value: I18nString = I18nString("")) {
  def toI18nString: I18nString = value
}

object I18nBrief {
  def apply(p: String): I18nBrief = I18nBrief(I18nString(p))
}
