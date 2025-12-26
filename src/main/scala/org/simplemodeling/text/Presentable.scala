package org.goldenport.text

/*
 * @since   Oct.  3, 2025
 * @version Oct.  7, 2025
 * @author  ASAMI, Tomoharu
 */
trait Presentable {
  def print: String                          // raw/unmasked, multiline ok
  def display: String                        // masked, 1-line
  def show: String                           // masked, shorter than display
  def getLiteral: Option[String] = None

  def embed(width: Int = 16): String =
    ??? // StringUtils.toEmbedConsole(display, width)

  // Optional additions (defaults safe)
  def pretty: String = display               // multiline human-friendly
  def tooltip: String = embed(24)            // ultra-short
  def log: String = display                  // k=v k2=v2 … (one line)
  def fingerprint: String = PresentDefaults.sha256(print)
}

private object PresentDefaults {
  import java.security.MessageDigest
  def sha256(s: String): String =
    MessageDigest.getInstance("SHA-256")
      .digest(s.getBytes("UTF-8")).map("%02x".format(_)).mkString
}

