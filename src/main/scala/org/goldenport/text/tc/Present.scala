package org.goldenport.text.tc

/*
 * @since   Oct.  3, 2025
 *  version Oct.  7, 2025
 * @version Jan. 15, 2026
 * @author  ASAMI, Tomoharu
 */
trait Present[-A] {
  def print(a: A): String
  def display(a: A): String
  def show(a: A): String
  def literal(a: A): Option[String] = None
  def embed(a: A, width: Int = 16): String =
    ??? // StringUtils.toEmbedConsole(display(a), width)

  // Optional (mirror OO
  def pretty(a: A): String = display(a)
  def tooltip(a: A): String = embed(a, 24)
  def log(a: A): String = display(a)
  def fingerprint(a: A): String = org.goldenport.text.Presentable.sha256(print(a))
}

object Present {
  def instance[A](
    p: A => String, d: A => String, s: A => String,
    lit: A => Option[String] = (_: A) => None
  ): Present[A] = new Present[A] {
    def print(a: A) = p(a); def display(a: A) = d(a); def show(a: A) = s(a)
    override def literal(a: A) = lit(a)
  }

  object syntax {
    implicit final class Ops[A](private val a: A) extends AnyVal {
      def printString(implicit ev: Present[A])   = ev.print(a)
      def displayString(implicit ev: Present[A]) = ev.display(a)
      def showString(implicit ev: Present[A])    = ev.show(a)
      def embedString(width: Int = 16)(implicit ev: Present[A]) = ev.embed(a, width)
      def prettyString(implicit ev: Present[A])  = ev.pretty(a)
      def tooltipString(implicit ev: Present[A]) = ev.tooltip(a)
      def logString(implicit ev: Present[A])     = ev.log(a)
    }
  }

  // Bridge from OO to TC
  given fromPresentable[A <: org.goldenport.text.Presentable]: Present[A] with
    def print(a: A)   = a.print
    def display(a: A) = a.display
    def show(a: A)    = a.show
    override def literal(a: A) = a.getLiteral
}

