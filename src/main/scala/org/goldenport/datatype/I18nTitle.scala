package org.goldenport.datatype

/*
 * @since   Jul. 23, 2025
 *  version Dec. 30, 2025
 * @version Jan. 21, 2026
 * @author  ASAMI, Tomoharu
 */
case class I18nTitle(value: I18nString = I18nString("")) {
}

object I18nTitle {
  def apply(p: String): I18nTitle = I18nTitle(I18nString(p))
}
