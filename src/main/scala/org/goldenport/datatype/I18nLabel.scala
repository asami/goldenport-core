package org.goldenport.datatype

/*
 * @since   Jul. 23, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
case class I18nLabel(value: I18nString = I18nString("")) {
  def toI18nString: I18nString = value
}

object I18nLabel {
  def apply(p: String): I18nLabel = I18nLabel(I18nString(p))
}
