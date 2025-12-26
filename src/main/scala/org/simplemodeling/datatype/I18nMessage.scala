package org.goldenport.datatype

import cats.data.NonEmptyVector
/*
 * @since   Feb.  7, 2017
 *  version Apr.  5, 2017
 *  version May. 25, 2017
 *  version Jul. 11, 2017
 *  version Aug. 29, 2017
 *  version Sep.  1, 2017
 *  version Oct. 15, 2018
 *  version Apr. 30, 2019
 *  version Jun.  8, 2019
 *  version Aug. 16, 2019
 *  version Sep. 23, 2019
 *  version Feb. 18, 2020
 *  version Mar. 30, 2020
 *  version Apr. 17, 2020
 *  version May.  4, 2020
 *  version Feb. 15, 2021
 *  version Apr. 29, 2021
 *  version May. 30, 2021
 *  version Jun. 19, 2021
 *  version Feb.  1, 2022
 *  version Dec.  8, 2022
 *  version Mar.  8, 2025
 *  since   Jul. 23, 2025
 *  version Aug. 30, 2025
 *  version Sep. 14, 2025
 * @version Dec. 25, 2025
 * @author  ASAMI, Tomoharu
 */
case class I18nMessage(
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

object I18nMessage {
  def apply(p: String): I18nMessage =
    I18nMessage(cats.data.NonEmptyVector.one(java.util.Locale.ROOT -> p))
}
