package org.goldenport.datatype

import io.circe.{Codec, Decoder, Encoder}
import org.goldenport.Consequence
import org.goldenport.convert.ValueReader

/*
 * @since   Jul. 15, 2026
 * @version Jul. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final case class EmailAddress private (value: String) extends StringDataType {
  protected def length_min: Int = EmailAddress.LENGTH_MIN
  protected def length_max: Int = EmailAddress.LENGTH_MAX
  protected def is_valid(p: String): Boolean = EmailAddress.canonical(p).contains(value)
}

object EmailAddress {
  val LENGTH_MIN = 3
  val LENGTH_MAX = 254

  private val _local_pattern = "^[^@\\s]+$".r
  private val _domain_pattern = "^[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?$".r

  given Codec[EmailAddress] = Codec.from(
    Decoder.decodeString.emap { value =>
      canonical(value)
        .map(new EmailAddress(_))
        .toRight(s"Invalid email address value: $value")
    },
    Encoder.encodeString.contramap(_.value)
  )

  given ValueReader[EmailAddress] with
    def readC(p: Any): Consequence[EmailAddress] = Option(p) match
      case None => Consequence.valueInvalid("Invalid email address value: null")
      case Some(value) => value match
        case address: EmailAddress => Consequence.success(address)
        case s: String => parse(s)
        case other => parse(other.toString)

  def parse(p: String): Consequence[EmailAddress] =
    canonical(p) match
      case Some(value) => Consequence.success(new EmailAddress(value))
      case None => Consequence.valueInvalid(s"Invalid email address value: $p")

  def canonical(p: String): Option[String] = {
    val source = Option(p).map(_.trim).getOrElse("")
    val separator = source.lastIndexOf('@')
    if (separator <= 0 || separator >= source.length - 1 || source.length > LENGTH_MAX)
      None
    else {
      val local = source.substring(0, separator)
      val domain = source.substring(separator + 1).toLowerCase(java.util.Locale.ROOT)
      if (_local_pattern.matches(local) && _domain_pattern.matches(domain) && domain.contains('.'))
        Some(s"$local@$domain")
      else
        None
    }
  }
}
