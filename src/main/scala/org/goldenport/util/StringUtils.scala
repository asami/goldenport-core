package org.goldenport.util

import scala.util.*

/*
 * @since   Apr. 12, 2025
 * @version Feb.  4, 2026
 * @author  ASAMI, Tomoharu
 */
object StringUtils {
  def concatPath(base: String, path: String): String =
    if (base.endsWith("/")) base + path.stripPrefix("/") else base + "/" + path.stripPrefix("/")

  def pathLeaf(path: String): String =
    path
      .stripSuffix("/")
      .split("/")
      .lastOption
      .getOrElse("")

  def tokeyvalue(s: String, delimiter: String): (String, String) = {
    val xs = s.split(java.util.regex.Pattern.quote(delimiter), 2)
    if (xs.length == 2) (xs(0), xs(1)) else (s, "")
  }

  def addUrlParams(base: String, params: Vector[(String, String)]): String =
    if (params.isEmpty) base
    else {
      val query = params.map { case (k, v) => s"${k}=${v}" }.mkString("&")
      if (base.contains("?")) s"${base}&${query}" else s"${base}?${query}"
    }

  def toKebabCase(value: String): String =
    _tokenize_for_kebab(value).mkString("-")

  /*
   * Class Name
   */
  def objectToSnakeName(postfix: String, o: Object): String =
    classToSnakeName(postfix, o.getClass)

  def classToSnakeName(postfix: String, klass: Class[?]): String = {
    val name = {
      val n = klass.getSimpleName
      if (n.endsWith("$"))
        n.substring(0, n.length - 1)
      else
        n
    }
    camelToSnake(postfix, name)
  }

  def camelToSnake(postfix: String, name: String): String =
    if (name.toLowerCase.endsWith(postfix.toLowerCase))
      camelToSnake(name.substring(0, name.length - postfix.length))
    else
      camelToSnake(name)

  def camelToSnake(p: String): String =
    p.foldLeft(camel.InitState("_"))(_.apply(_)).result

  object camel {
    def InitState(d: String): ParseState = NeutralState(d)

    sealed trait ParseState {
      def delimiter: String
      def result: String
      def apply(rhs: Char): ParseState
      protected def to_string(xs: Vector[String]) = xs.map(_.toLowerCase).mkString(delimiter)
    }
    case class NeutralState(
      delimiter: String,
      z: Vector[String] = Vector.empty
    ) extends ParseState {
      def apply(rhs: Char) = InWordState(delimiter, z, Vector(rhs))
      def result: String = to_string(z)
    }
    case class InWordState(
      delimiter: String,
      z: Vector[String],
      x: Vector[Char]
    ) extends ParseState {
      def apply(rhs: Char) = {
        if (rhs.isUpper)
          copy(delimiter, z :+ x.mkString, Vector(rhs))
        else
          copy(delimiter, z, x :+ rhs)
      }

      def result: String = to_string(z :+ x.mkString)
    }
  }

  private def _tokenize_for_kebab(value: String): Vector[String] = {
    val normalizedSeparators = value.trim.replaceAll("[\\s_./\\\\]+", "-")
    val result = Vector.newBuilder[String]
    val current = new StringBuilder

    def flush(): Unit =
      if (current.nonEmpty) {
        result += current.toString
        current.clear()
      }

    def isBoundary(c: Char, prev: Option[Char], next: Option[Char]): Boolean =
      if (current.isEmpty || !c.isUpper) {
        false
      } else {
        val prevIsLowerOrDigit = prev.exists(ch => ch.isLower || ch.isDigit)
        val prevIsUpper = prev.exists(_.isUpper)
        val nextIsLower = next.exists(_.isLower)
        prevIsLowerOrDigit || (prevIsUpper && nextIsLower && current.length > 1)
      }

    var index = 0
    var prevAlphaNum: Option[Char] = None
    while (index < normalizedSeparators.length) {
      val c = normalizedSeparators.charAt(index)
      if (c.isLetterOrDigit) {
        val next = _next_alnum(normalizedSeparators, index + 1)
        if (isBoundary(c, prevAlphaNum, next)) {
          flush()
        }
        current.append(c.toLower)
        prevAlphaNum = Some(c)
      } else {
        flush()
        prevAlphaNum = None
      }
      index += 1
    }
    flush()
    result.result().filter(_.nonEmpty)
  }

  private def _next_alnum(value: String, from: Int): Option[Char] = {
    var index = from
    while (index < value.length) {
      val c = value.charAt(index)
      if (c.isLetterOrDigit) return Some(c)
      index += 1
    }
    None
  }
}
