package org.goldenport.text

import cats.data.*

/*
 * @since   Jul. 24, 2019
 *  version Sep. 18, 2019
 *  version Oct. 16, 2019
 *  version Feb. 23, 2022
 *  version Mar. 19, 2022
 *  version Mar.  2, 2025
 *  version Oct.  3, 2025 (from Showable)
 *  version Oct.  7, 2025
 * @version Jan. 29, 2026
 * @author  ASAMI, Tomoharu
 */
trait Presentable {
  /*
   * Natural representation for data. Show as-is even large data. No security blinding.
   */
  def print: String                          // raw/unmasked, multiline ok

  /*
   * 1 line representation for interaction representation (e.g. REPL). Security blinding.
   */
  def display: String = print                // masked, 1-line

  /*
   * Sufficient short information for debug. Security blinding.
   */
  def show: String = print                   // masked, shorter than display

  /*
   * Literal representation.
   */
  def getLiteral: Option[String] = None

  /*
   * Minimal information for embedding. Security blinding.
   */
  def embed(width: Int = 16): String =
    ??? // StringUtils.toEmbedConsole(display, width)

  // Optional additions (defaults safe)
  def pretty: String = display               // multiline human-friendly
  def tooltip: String = embed(24)            // ultra-short
  def log: String = display                  // k=v k2=v2 … (one line)
  def fingerprint: String = Presentable.sha256(print)

  final override def toString: String = show
}

object Presentable {
  def print(p: Any): String = p match {
    case m: Presentable => m.print
    case m: CharSequence => m.toString
    case m: Seq[?] => m.map(Presentable.print).mkString(",")
    case m: Set[?] => m.map(Presentable.print).mkString(",")
    case m: Map[?, ?] => m.map { (k, v) =>
      s"$k=${Presentable.print(v)}"
    }.mkString(",")
    case m: Iterable[?] => m.map(Presentable.print).mkString(",")
    case m: NonEmptyVector[?] => print(m.toVector)
    case m: NonEmptyList[?] => print(m.toList)
//    case m: NonEmptyChain[?] => print(m.toChain)
    case m => _default(p)
  }

  def display(p: Any): String = p match {
    case m: Presentable => m.display
    case m: CharSequence => m.toString
    case m: Seq[?] => print(m)
    case m: Set[?] => print(m)
    case m: Map[?, ?] => print(m)
    case m: Iterable[?] => print(m)
    case m: NonEmptyVector[?] => print(m)
    case m: NonEmptyList[?] => print(m)
//    case m: NonEmptyChain[?] => print(m)
    case m => _default(p)
  }

    def show(p: Any): String = p match {
    case m: Presentable => m.show
    case m: CharSequence => m.toString
    case m: List[?] => _show_container("List", m)
    case m: Set[?] => _show_container("Set", m)
    case m: Seq[?] => _show_container("Seq", m)
    case m: Map[?, ?] => _show_container("Map", m.map((k,v) => s"$k=${display(v)}"))
    case m: Iterable[?] => _show_container("Seq", m)
    case m: NonEmptyVector[?] => _show_container("NonEmptyVector", m.toVector)
    case m: NonEmptyList[?] => _show_container("NonEmptyList", m.toList)
//    case m: NonEmptyChain[?] => _show_container("NonEmptyChain", m.toChain.toVector)
    case m => _default(p.toString)
  }

  def embed(p: Any, width: Int = 16): String = p match {
    case m: Presentable => m.embed(width)
    case m: CharSequence => _embed(m.toString, width)
    case m: Seq[?] => _embed_container("Seq", m, width)
    case m: Set[?] => _embed_container("Set", m, width)
    case m: Map[?, ?] => _embed_container("Map", m, width)
    case m: Iterable[?] => _embed_container("Iterable", m, width)
    case m: NonEmptyVector[?] => _embed_container("NonEmptyVector", m.length, width)
    case m: NonEmptyList[?] => _embed_container("NonEmptyList", m.size, width)
//    case m: NonEmptyChain[?] => _embed_container("NonEmptyChain", m.length.toInt, width)
    case m => _embed(_default(p), width)
  }

  private def _show_container(name: String, elements: Iterable[Any]): String =
    s"$name[${elements.size}](${_show_elements(elements)})"

  private def _show_container(name: String, size: Int): String =
    s"$name[${size}]"

  private def _show_elements(ps: Iterable[Any]): String = {
    if (ps.size <= 3)
      ps.map(show).mkString(",")
    else
      ps.take(3).map(embed(_)).mkString("", ",", " ...")
  }

  private def _embed(p: String, width: Int): String = {
    val postfix = "..."
    val size = width - postfix.length
    if (p.length <= size)
      p
    else
      p.substring(0, size) + postfix
  }

  private def _embed_container(name: String, elements: Iterable[Any], width: Int): String =
    _embed(s"$name[${elements.size}]", width)

  private def _embed_container(name: String, size: Int, width: Int): String =
    _embed(s"$name[${size}]", width)

  private def _default(p: Any) = p.toString

  import java.security.MessageDigest

  def sha256(s: String): String =
    MessageDigest.getInstance("SHA-256")
      .digest(s.getBytes("UTF-8")).map("%02x".format(_)).mkString
}
