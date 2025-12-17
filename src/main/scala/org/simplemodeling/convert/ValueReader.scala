package org.simplemodeling.convert

/*
 * @since   Oct. 17, 2025
 * @version Oct. 17, 2025
 * @author  ASAMI, Tomoharu
 */
trait ValueReader[T]:
  def read(value: Any): Option[T]

object ValueReader {
  given ValueReader[Int] with
    def read(v: Any): Option[Int] = v match
      case i: Int => Some(i)
      case s: String => s.toIntOption
      case _ => None

  given ValueReader[String] with
    def read(v: Any): Option[String] = v match
      case s: String => Some(s)
      case null => None
      case other => Some(other.toString)
}
