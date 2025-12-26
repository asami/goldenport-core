package org.simplemodeling.datatype

import cats.data.NonEmptyVector
/*
 * @since   Apr. 17, 2020
 *  version Jun.  1, 2020
 *  version Mar. 27, 2021
 *  version Jun. 20, 2021
 *  version Feb.  9, 2022
 *  version Jun. 13, 2022
 *  version Dec. 28, 2022
 *  version May. 11, 2025
 *  version Jul. 23, 2025
 * @version Dec. 25, 2025
 * @author  ASAMI, Tomoharu
 */
case class I18nString(
  entries: NonEmptyVector[(java.util.Locale, String)]
) {
  def displayMessage: String = {
    val prioritized = Vector(java.util.Locale.ROOT, java.util.Locale.ENGLISH, java.util.Locale.JAPANESE)
    val byLocale = entries.toVector.toMap
    prioritized.iterator
      .map(byLocale.get)
      .collectFirst { case Some(value) => value }
      .getOrElse(entries.head._2)
  }
}

object I18nString {
  def apply(p: String): I18nString =
    I18nString(NonEmptyVector.one(java.util.Locale.ROOT -> p))
}
