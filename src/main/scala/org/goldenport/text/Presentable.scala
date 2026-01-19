package org.goldenport.text

/*
 * @since   Jul. 24, 2019
 *  version Sep. 18, 2019
 *  version Oct. 16, 2019
 *  version Feb. 23, 2022
 *  version Mar. 19, 2022
 *  version Mar.  2, 2025
 *  version Oct.  3, 2025 (from Showable)
 *  version Oct.  7, 2025
 * @version Jan. 20, 2026
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
    case m => _default(p)
  }

  def display(p: Any): String = p match {
    case m: Presentable => m.display
    case m => _default(p)
  }

  def show(p: Any): String = p match {
    case m: Presentable => m.show
    case m => _default(p.toString)
  }

  private def _default(p: Any) = p.toString

  import java.security.MessageDigest

  def sha256(s: String): String =
    MessageDigest.getInstance("SHA-256")
      .digest(s.getBytes("UTF-8")).map("%02x".format(_)).mkString
}
