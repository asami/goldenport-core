package org.goldenport.schema

import java.time.{LocalDateTime, OffsetDateTime, YearMonth, ZonedDateTime}
import scala.util.Try
import org.goldenport.{Conclusion, Consequence}
import org.goldenport.datatype.I18nMessage
import org.goldenport.observation.Cause

/*
 * Datatype reference used by schema definitions.
 *
 * This is intentionally a lightweight, declarative identifier.
 * Concrete value objects live in `org.simplemodeling.datatype` and may be
 * mapped by CNCF/runtime layers as needed.
 */
/*
 * @since   Nov. 23, 2012
 *  version Dec. 18, 2012
 *  version Jan. 29, 2013
 *  version Feb. 20, 2013
 *  version Mar. 12, 2013
 *  version Dec. 31, 2013
 *  version Jan. 29, 2014
 *  version Feb.  6, 2014
 *  version May. 15, 2014
 *  version Jul. 27, 2014
 *  version Sep. 25, 2015
 *  version Jun. 16, 2016
 *  version Jan. 23, 2017
 *  version Sep. 21, 2017
 *  version Oct. 22, 2017
 *  version Nov. 13, 2017
 *  version Jan. 21, 2018
 *  version Jan. 10, 2019
 *  version Jul. 31, 2019
 *  version Aug. 16, 2019
 *  version Nov.  4, 2019
 *  version Jan.  9, 2020
 *  version Feb. 27, 2021
 *  version Mar. 21, 2021
 *  version Apr. 29, 2021
 *  version Oct. 31, 2021
 *  version Nov.  5, 2021
 *  version Sep.  6, 2024
 *  version Sep. 17, 2025
 * @version Dec. 29, 2025
 * @author  ASAMI, Tomoharu
 */
sealed trait DataType {
  def name: String
}

object DataType {

  // /*
  //  * Declares that a field uses a value object datatype identified by name.
  //  *
  //  * Example:
  //  *   ValueObject("StringValue")
  //  *   ValueObject("Locale")
  //  */
  // final case class ValueObject(name: String) extends Datatype
}

case object XString extends DataType {
  def name = "string"
}

trait CanonicalNormalizer[T] {
  def normalize(value: Any): Consequence[T]

  def normalizeAll(values: Vector[Any]): Consequence[Vector[T]] =
    Consequence.zipN(values.map(normalize)).map(_.toVector)
}

private object CanonicalFailure {
  def format[A](message: String): Consequence[A] = {
    val base = Conclusion.simple(message)
    val observation = base.observation.copy(
      cause = Some(Cause.FormatError),
      message = Some(I18nMessage(message))
    )
    Consequence.Failure(base.copy(observation = observation))
  }
}

object BigIntNormalizer extends CanonicalNormalizer[BigInt] {
  def normalize(value: Any): Consequence[BigInt] =
    value match {
      case v: BigInt => Consequence.success(v)
      case v: Int => Consequence.success(BigInt(v))
      case v: Long => Consequence.success(BigInt(v))
      case v: Short => Consequence.success(BigInt(v.toInt))
      case v: String =>
        Try(BigInt(v)) match {
          case scala.util.Success(result) => Consequence.success(result)
          case scala.util.Failure(_) => CanonicalFailure.format("format error: integer value")
        }
      case _ =>
        CanonicalFailure.format("format error: integer value")
    }
}

trait CanonicalDataType[T] extends DataType {
  def normalizer: CanonicalNormalizer[T]
}

abstract class IntegerDataType extends CanonicalDataType[BigInt] {
  def normalizer: CanonicalNormalizer[BigInt] = BigIntNormalizer
  def isValid(value: BigInt): Boolean
}

case object XNonNegativeInteger extends IntegerDataType {
  def name = "nonNegativeInteger"
  def isValid(value: BigInt): Boolean = value >= 0
}

case object XPositiveInteger extends IntegerDataType {
  def name = "positiveInteger"
  def isValid(value: BigInt): Boolean = value > 0
}

object ZonedDateTimeNormalizer extends CanonicalNormalizer[ZonedDateTime] {
  def normalize(value: Any): Consequence[ZonedDateTime] =
    value match {
      case v: ZonedDateTime => Consequence.success(v)
      case v: OffsetDateTime => Consequence.success(v.toZonedDateTime)
      case v: String =>
        Try(ZonedDateTime.parse(v)) match {
          case scala.util.Success(result) => Consequence.success(result)
          case scala.util.Failure(_) =>
            Try(OffsetDateTime.parse(v)) match {
              case scala.util.Success(result) => Consequence.success(result.toZonedDateTime)
              case scala.util.Failure(_) =>
                CanonicalFailure.format("format error: datetime value")
            }
        }
      case _ =>
        CanonicalFailure.format("format error: datetime value")
    }
}

object LocalDateTimeNormalizer extends CanonicalNormalizer[LocalDateTime] {
  def normalize(value: Any): Consequence[LocalDateTime] =
    value match {
      case v: LocalDateTime => Consequence.success(v)
      case v: String =>
        Try(LocalDateTime.parse(v)) match {
          case scala.util.Success(result) => Consequence.success(result)
          case scala.util.Failure(_) =>
            CanonicalFailure.format("format error: local datetime value")
        }
      case _ =>
        CanonicalFailure.format("format error: local datetime value")
    }
}

object YearMonthNormalizer extends CanonicalNormalizer[YearMonth] {
  def normalize(value: Any): Consequence[YearMonth] =
    value match {
      case v: YearMonth => Consequence.success(v)
      case v: String =>
        Try(YearMonth.parse(v)) match {
          case scala.util.Success(result) => Consequence.success(result)
          case scala.util.Failure(_) =>
            CanonicalFailure.format("format error: year month value")
        }
      case _ =>
        CanonicalFailure.format("format error: year month value")
    }
}

case object XDateTime extends CanonicalDataType[ZonedDateTime] {
  def name = "dateTime"
  def normalizer: CanonicalNormalizer[ZonedDateTime] = ZonedDateTimeNormalizer
}

case object XLocalDateTime extends CanonicalDataType[LocalDateTime] {
  def name = "localDateTime"
  def normalizer: CanonicalNormalizer[LocalDateTime] = LocalDateTimeNormalizer
}

case object XYearMonth extends CanonicalDataType[YearMonth] {
  def name = "yearMonth"
  def normalizer: CanonicalNormalizer[YearMonth] = YearMonthNormalizer
}
