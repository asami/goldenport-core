package org.goldenport.convert

import java.net.{URI, URL}
import java.time.LocalTime
import java.util.{Locale, TimeZone}

import org.goldenport.Consequence
import org.goldenport.bag.{Bag, BinaryBag, TextBag}
import org.goldenport.datatype.Urn
import org.goldenport.schema.*

/*
 * @since   Oct. 17, 2025
 *  version Feb. 19, 2026
 * @version Apr. 11, 2026
 * @author  ASAMI, Tomoharu
 */
trait ValueReader[T]:
  def read(value: Any): Option[T] = readC(value).toOption
  def readC(value: Any): Consequence[T]

object ValueReader {
  // private def invalid[T](value: Any, expected: String): Consequence[T] =
  //   Consequence.failure(s"Invalid $expected value: $value")

  given ValueReader[Any] with
    def readC(v: Any): Consequence[Any] = Consequence.success(v)

  given ValueReader[Boolean] with
    def readC(v: Any): Consequence[Boolean] = v match
      case b: Boolean => Consequence.success(b)
      case s: String =>
        s.trim.toLowerCase match
          case "true" | "t" | "yes" | "y" | "1" => Consequence.success(true)
          case "false" | "f" | "no" | "n" | "0" => Consequence.success(false)
          case _ => Consequence.failValueInvalid(v, XBoolean)
      case n: Int => Consequence.success(n != 0)
      case n: Long => Consequence.success(n != 0L)
      case _ => Consequence.failValueInvalid(v, XBoolean)

  given ValueReader[Byte] with
    def readC(v: Any): Consequence[Byte] = v match
      case b: Byte => Consequence.success(b)
      case s: Short if s >= Byte.MinValue && s <= Byte.MaxValue => Consequence.success(s.toByte)
      case i: Int if i >= Byte.MinValue && i <= Byte.MaxValue => Consequence.success(i.toByte)
      case l: Long if l >= Byte.MinValue && l <= Byte.MaxValue => Consequence.success(l.toByte)
      case s: String =>
        val trimmed = s.trim
        trimmed.toByteOption match
          case Some(value) => Consequence.success(value)
          case None => Consequence.failValueInvalid(v, XInt)
      case _ => Consequence.failValueInvalid(v, XInt)

  given ValueReader[Short] with
    def readC(v: Any): Consequence[Short] = v match
      case s: Short => Consequence.success(s)
      case b: Byte => Consequence.success(b.toShort)
      case i: Int if i >= Short.MinValue && i <= Short.MaxValue => Consequence.success(i.toShort)
      case l: Long if l >= Short.MinValue && l <= Short.MaxValue => Consequence.success(l.toShort)
      case s: String =>
        val trimmed = s.trim
        trimmed.toShortOption match
          case Some(value) => Consequence.success(value)
          case None => Consequence.failValueInvalid(v, XInt)
      case _ => Consequence.failValueInvalid(v, XInt)

  given ValueReader[Int] with
    def readC(v: Any): Consequence[Int] = v match
      case i: Int => Consequence.success(i)
      case l: Long if l.isValidInt => Consequence.success(l.toInt)
      case s: String =>
        val trimmed = s.trim
        trimmed.toIntOption match
          case Some(value) => Consequence.success(value)
          case None => Consequence.failValueInvalid(v, XInt)
      case _ => Consequence.failValueInvalid(v, XInt)

  given ValueReader[Long] with
    def readC(v: Any): Consequence[Long] = v match
      case l: Long => Consequence.success(l)
      case i: Int => Consequence.success(i.toLong)
      case s: String =>
        val trimmed = s.trim
        trimmed.toLongOption match
          case Some(value) => Consequence.success(value)
          case None => Consequence.failValueInvalid(v, XLong)
      case _ => Consequence.failValueInvalid(v, XLong)

  given ValueReader[Float] with
    def readC(v: Any): Consequence[Float] = v match
      case f: Float => Consequence.success(f)
      case d: Double if !d.isInfinity && !d.isNaN && d >= -Float.MaxValue && d <= Float.MaxValue =>
        Consequence.success(d.toFloat)
      case i: Int => Consequence.success(i.toFloat)
      case l: Long => Consequence.success(l.toFloat)
      case s: String =>
        val trimmed = s.trim
        trimmed.toFloatOption match
          case Some(value) => Consequence.success(value)
          case None => Consequence.failValueInvalid(v, XDouble)
      case _ => Consequence.failValueInvalid(v, XDouble)

  given ValueReader[Double] with
    def readC(v: Any): Consequence[Double] = v match
      case d: Double => Consequence.success(d)
      case f: Float => Consequence.success(f.toDouble)
      case i: Int => Consequence.success(i.toDouble)
      case l: Long => Consequence.success(l.toDouble)
      case s: String =>
        val trimmed = s.trim
        trimmed.toDoubleOption match
          case Some(value) => Consequence.success(value)
          case None => Consequence.failValueInvalid(v, XDouble)
      case _ => Consequence.failValueInvalid(v, XDouble)

  given ValueReader[BigInt] with
    def readC(v: Any): Consequence[BigInt] = v match
      case bi: BigInt => Consequence.success(bi)
      case i: Int => Consequence.success(BigInt(i))
      case l: Long => Consequence.success(BigInt(l))
      case s: String =>
        try
          Consequence.success(BigInt(s.trim))
        catch
          case _: NumberFormatException => Consequence.failValueInvalid(v, XInteger)
      case _ => Consequence.failValueInvalid(v, XInteger)

  given ValueReader[BigDecimal] with
    def readC(v: Any): Consequence[BigDecimal] = v match
      case bd: BigDecimal => Consequence.success(bd)
      case bi: BigInt => Consequence.success(BigDecimal(bi))
      case d: Double => Consequence.success(BigDecimal(d))
      case f: Float => Consequence.success(BigDecimal.decimal(f))
      case i: Int => Consequence.success(BigDecimal(i))
      case l: Long => Consequence.success(BigDecimal(l))
      case s: String =>
        try
          Consequence.success(BigDecimal(s.trim))
        catch
          case _: NumberFormatException => Consequence.failValueInvalid(v, XDecimal)
      case _ => Consequence.failValueInvalid(v, XDecimal)

  given ValueReader[URL] with
    def readC(v: Any): Consequence[URL] = v match
      case url: URL => Consequence.success(url)
      case uri: URI => Consequence.success(uri.toURL)
      case s: String =>
        try Consequence.success(URI.create(s.trim).toURL)
        catch case _: Throwable => Consequence.failValueInvalid(v, XString)
      case _ => Consequence.failValueInvalid(v, XString)

  given ValueReader[URI] with
    def readC(v: Any): Consequence[URI] = v match
      case uri: URI => Consequence.success(uri)
      case url: URL => Consequence.success(url.toURI)
      case s: String =>
        try Consequence.success(URI.create(s.trim))
        catch case _: Throwable => Consequence.failValueInvalid(v, XString)
      case _ => Consequence.failValueInvalid(v, XString)

  given ValueReader[Urn] with
    def readC(v: Any): Consequence[Urn] = v match
      case urn: Urn => Consequence.success(urn)
      case uri: URI => Urn.parse(uri.toString)
      case s: String => Urn.parse(s)
      case _ => Consequence.failValueInvalid(v, XString)

  given ValueReader[LocalTime] with
    def readC(v: Any): Consequence[LocalTime] = v match
      case t: LocalTime => Consequence.success(t)
      case s: String =>
        try Consequence.success(LocalTime.parse(s.trim))
        catch case _: Throwable => Consequence.failValueInvalid(v, XString)
      case _ => Consequence.failValueInvalid(v, XString)

  given ValueReader[Locale] with
    def readC(v: Any): Consequence[Locale] = v match
      case locale: Locale => Consequence.success(locale)
      case s: String => Consequence.success(Locale.forLanguageTag(s.trim))
      case _ => Consequence.failValueInvalid(v, XString)

  given ValueReader[TimeZone] with
    def readC(v: Any): Consequence[TimeZone] = v match
      case tz: TimeZone => Consequence.success(tz)
      case s: String =>
        val id = s.trim
        val tz = TimeZone.getTimeZone(id)
        if id.nonEmpty && (tz.getID == id || id.equalsIgnoreCase("GMT") || id.startsWith("GMT")) then
          Consequence.success(tz)
        else
          Consequence.failValueInvalid(v, XString)
      case _ => Consequence.failValueInvalid(v, XString)

  given ValueReader[Bag] with
    def readC(v: Any): Consequence[Bag] = v match
      case bag: Bag => Consequence.success(bag)
      case bytes: Array[Byte] => Consequence.success(Bag.fromBytes(bytes))
      case s: String => Consequence.success(Bag.text(s))
      case _ => Consequence.failValueInvalid(v, XString)

  given ValueReader[BinaryBag] with
    def readC(v: Any): Consequence[BinaryBag] = v match
      case bag: BinaryBag => Consequence.success(bag)
      case bag: Bag => Consequence.success(bag.promoteToBinary())
      case bytes: Array[Byte] => Consequence.success(Bag.binary(bytes))
      case _ => Consequence.failValueInvalid(v, XString)

  given ValueReader[TextBag] with
    def readC(v: Any): Consequence[TextBag] = v match
      case bag: TextBag => Consequence.success(bag)
      case bag: Bag => Consequence.success(TextBag(bag))
      case s: String => Consequence.success(Bag.text(s))
      case _ => Consequence.failValueInvalid(v, XString)

  given ValueReader[String] with
    def readC(v: Any): Consequence[String] = v match
      case null => Consequence.failValueInvalid(v, XString)
      case s: String => Consequence.success(s)
      case other => Consequence.success(other.toString)

}
