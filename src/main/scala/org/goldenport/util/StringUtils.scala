package org.goldenport.util

import scala.util.*

/*
 * @since   Apr. 12, 2025
 * @version Apr. 12, 2025
 * @author  ASAMI, Tomoharu
 */
object StringUtils {
  def concatPath(base: String, path: String): String =
    if (base.endsWith("/")) base + path.stripPrefix("/") else base + "/" + path.stripPrefix("/")

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
}
