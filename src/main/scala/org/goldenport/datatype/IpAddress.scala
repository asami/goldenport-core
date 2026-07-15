package org.goldenport.datatype

import java.net.{Inet6Address, InetAddress}
import io.circe.{Codec, Decoder, Encoder}
import org.goldenport.Consequence
import org.goldenport.convert.ValueReader

/*
 * @since   Jul. 15, 2026
 * @version Jul. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final case class IpAddress private (value: String) extends StringDataType {
  protected def length_min: Int = IpAddress.LENGTH_MIN
  protected def length_max: Int = IpAddress.LENGTH_MAX
  protected def is_valid(p: String): Boolean = IpAddress.canonical(p).contains(value)
}

object IpAddress {
  val LENGTH_MIN = 2
  val LENGTH_MAX = 45

  given Codec[IpAddress] = Codec.from(
    Decoder.decodeString.emap { value =>
      canonical(value)
        .map(new IpAddress(_))
        .toRight(s"Invalid IP address value: $value")
    },
    Encoder.encodeString.contramap(_.value)
  )

  given ValueReader[IpAddress] with
    def readC(p: Any): Consequence[IpAddress] = Option(p) match
      case None => Consequence.valueInvalid("Invalid IP address value: null")
      case Some(value) => value match
        case address: IpAddress => Consequence.success(address)
        case s: String => parse(s)
        case other => parse(other.toString)

  def parse(p: String): Consequence[IpAddress] =
    canonical(p) match
      case Some(value) => Consequence.success(new IpAddress(value))
      case None => Consequence.valueInvalid(s"Invalid IP address value: $p")

  def canonical(p: String): Option[String] = {
    val source = Option(p).map(_.trim).getOrElse("")
    if (source.contains(":"))
      _ipv6(source)
    else
      _ipv4(source)
  }

  private def _ipv4(p: String): Option[String] = {
    val parts = p.split("\\.", -1).toVector
    if (parts.length != 4 || parts.exists(_.isEmpty))
      None
    else {
      val octets = parts.map { part =>
        if (part.forall(_.isDigit) && (part == "0" || !part.startsWith("0")))
          part.toIntOption.filter(x => x >= 0 && x <= 255)
        else
          None
      }
      if (octets.forall(_.isDefined))
        Some(octets.flatten.mkString("."))
      else
        None
    }
  }

  private def _ipv6(p: String): Option[String] =
    if (p.contains("%") || !p.forall(c => c.isDigit || "abcdefABCDEF:.".contains(c)))
      None
    else
      scala.util.Try(InetAddress.getByName(p)).toOption.collect {
        case address: Inet6Address => address.getHostAddress.toLowerCase(java.util.Locale.ROOT)
      }
}
