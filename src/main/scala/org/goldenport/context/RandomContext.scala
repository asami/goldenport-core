package org.goldenport.context

/*
 * @since   Dec. 30, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
trait RandomContext {
  def nextInt(): Int
  def nextInt(bound: Int): Int
  def nextLong(): Long
  def nextDouble(): Double
  def nextBoolean(): Boolean
}

object RandomContext {
  private case object FixedRandomContext extends RandomContext {
    def nextInt(): Int = 0
    def nextInt(bound: Int): Int = 0
    def nextLong(): Long = 0L
    def nextDouble(): Double = 0.0d
    def nextBoolean(): Boolean = false
  }

  def from(name: String): RandomContext =
    name match {
      case "fixed" => FixedRandomContext
      case "deterministic" => FixedRandomContext
      case _ => FixedRandomContext
    }
}
