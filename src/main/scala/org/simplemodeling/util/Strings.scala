package org.simplemodeling.util

/*
 * @since   Dec. 23, 2025
 * @version Dec. 23, 2025
 * @author  ASAMI, Tomoharu
 */
object Strings {
  def totokens(s: String, delimiter: String): Vector[String] =
    if (s.isEmpty) Vector.empty
    else s.split(java.util.regex.Pattern.quote(delimiter)).toVector

  def cutstring(s: String, maxlen: Int): String =
    if (s.length <= maxlen) s else s.take(maxlen) + "..."
}
