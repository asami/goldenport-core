package org.goldenport.convert

/*
 * @since   Oct. 17, 2025
 * @version Oct. 17, 2025
 * @author  ASAMI, Tomoharu
 */
trait ValueReader[T]:
  def read(value: Any): Option[T]

object ValueReader {
  given ValueReader[Boolean] with
    def read(v: Any): Option[Boolean] = v match
      case b: Boolean => Some(b)
      case s: String => s.trim.toLowerCase match
        case "true" | "t" | "yes" | "y" | "1" => Some(true)
        case "false" | "f" | "no" | "n" | "0" => Some(false)
        case _ => None
      case n: Int => Some(n != 0)
      case n: Long => Some(n != 0L)
      case _ => None

  given ValueReader[Int] with
    def read(v: Any): Option[Int] = v match
      case i: Int => Some(i)
      case s: String => s.toIntOption
      case l: Long if l.isValidInt => Some(l.toInt)
      case _ => None

  given ValueReader[Long] with
    def read(v: Any): Option[Long] = v match
      case l: Long => Some(l)
      case i: Int => Some(i.toLong)
      case s: String => s.toLongOption
      case _ => None

  given ValueReader[Double] with
    def read(v: Any): Option[Double] = v match
      case d: Double => Some(d)
      case f: Float => Some(f.toDouble)
      case i: Int => Some(i.toDouble)
      case l: Long => Some(l.toDouble)
      case s: String => s.toDoubleOption
      case _ => None

  given ValueReader[BigInt] with
    def read(v: Any): Option[BigInt] = v match
      case bi: BigInt => Some(bi)
      case i: Int => Some(BigInt(i))
      case l: Long => Some(BigInt(l))
      case s: String =>
        try Some(BigInt(s.trim)) catch { case _: NumberFormatException => None }
      case _ => None

  given ValueReader[BigDecimal] with
    def read(v: Any): Option[BigDecimal] = v match
      case bd: BigDecimal => Some(bd)
      case bi: BigInt => Some(BigDecimal(bi))
      case d: Double => Some(BigDecimal(d))
      case f: Float => Some(BigDecimal.decimal(f))
      case i: Int => Some(BigDecimal(i))
      case l: Long => Some(BigDecimal(l))
      case s: String =>
        try Some(BigDecimal(s.trim)) catch { case _: NumberFormatException => None }
      case _ => None

  given ValueReader[String] with
    def read(v: Any): Option[String] = v match
      case s: String => Some(s)
      case null => None
      case other => Some(other.toString)
}
