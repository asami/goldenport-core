package org.goldenport.datatype

import io.circe.{Codec, Decoder, Encoder}
import org.goldenport.Consequence
import org.goldenport.convert.ValueReader

/*
 * @since   Jul. 15, 2026
 * @version Jul. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final case class PhoneNumber private (value: String) extends StringDataType {
  protected def length_min: Int = PhoneNumber.LENGTH_MIN
  protected def length_max: Int = PhoneNumber.LENGTH_MAX
  protected def is_valid(p: String): Boolean = PhoneNumber.canonical(p).contains(value)
}

object PhoneNumber {
  val LENGTH_MIN = 8
  val LENGTH_MAX = 16

  private val _e164_pattern = "^\\+[1-9]\\d{6,14}$".r

  given Codec[PhoneNumber] = Codec.from(
    Decoder.decodeString.emap { value =>
      canonical(value)
        .map(new PhoneNumber(_))
        .toRight(s"Invalid phone number value: $value")
    },
    Encoder.encodeString.contramap(_.value)
  )

  given ValueReader[PhoneNumber] with
    def readC(p: Any): Consequence[PhoneNumber] = Option(p) match
      case None => Consequence.valueInvalid("Invalid phone number value: null")
      case Some(value) => value match
        case number: PhoneNumber => Consequence.success(number)
        case s: String => parse(s)
        case other => parse(other.toString)

  def parse(p: String): Consequence[PhoneNumber] =
    canonical(p) match
      case Some(value) => Consequence.success(new PhoneNumber(value))
      case None => Consequence.valueInvalid(s"Invalid phone number value: $p")

  def canonical(p: String): Option[String] = {
    val compact = Option(p).map(_.trim).getOrElse("").filterNot(c => c.isWhitespace || "()-".contains(c))
    val international = if (compact.startsWith("00")) s"+${compact.drop(2)}" else compact
    Option.when(_e164_pattern.matches(international))(international)
  }
}
