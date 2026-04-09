package org.goldenport.datatype

import java.net.URI
import scala.language.strictEquality
import cats.*
import cats.implicits.*
import io.circe.{Codec, Decoder, Encoder}
import org.goldenport.Consequence
import org.goldenport.convert.ValueReader

/*
 * @since   Apr.  9, 2026
 * @version Apr.  9, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class Urn() extends StringDataType() {
  import Urn.*

  def uri: URI = URI.create(value)

  protected final def length_min: Int = LENGTH_MIN
  protected final def length_max: Int = LENGTH_MAX
  protected final def is_valid(s: String): Boolean = validate_urn(s)
}

object Urn {
  val LENGTH_MIN = 1
  val LENGTH_MAX = 2048

  given CanEqual[Urn, Urn] = CanEqual.derived
  given Eq[Urn] = Eq.by(_.value)

  given Codec[Urn] = Codec.from(
    Decoder.decodeString.emap { s =>
      parse(s).toOption.toRight(s"Invalid URN: $s")
    },
    Encoder.encodeString.contramap(_.value)
  )

  case class Instance(value: String) extends Urn()

  given ValueReader[Urn] with
    def readC(v: Any): Consequence[Urn] = Option(v) match
      case None => Consequence.failure("Invalid Urn value: null")
      case Some(value) => value match
        case u: Urn => Consequence.success(u)
        case uri: URI => parse(uri.toString)
        case s: String => parse(s)
        case other => parse(other.toString)

  def apply(s: String): Urn = Instance(s)

  def parse(s: String): Consequence[Urn] = {
    val normalized = Option(s).map(_.trim).getOrElse("")
    if (validate_urn(normalized))
      Consequence.success(Instance(normalized))
    else
      Consequence.failure(s"Invalid URN: $s")
  }

  def validate_urn(s: String): Boolean =
    try {
      val uri = URI.create(s)
      s.nonEmpty && Option(uri.getScheme).exists(_.equalsIgnoreCase("urn")) && Option(uri.getSchemeSpecificPart).exists(_.nonEmpty)
    } catch {
      case _: Throwable => false
    }
}
