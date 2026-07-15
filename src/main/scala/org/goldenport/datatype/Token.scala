package org.goldenport.datatype

import scala.util.Try
import io.circe.{Codec, Decoder, Encoder}
import org.goldenport.Consequence

/*
 * @since   Jul. 23, 2025
 *  version Jul. 23, 2025
 *  version Apr.  9, 2026
 *  version Jun.  4, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class Token() extends StringDataType() {
  import Token._

  protected final def length_min: Int = LENGTH_MIN
  protected final def length_max: Int = LENGTH_MAX
  protected final def is_valid(s: String): Boolean = validate_token(s)
}

object Token {
import org.goldenport.convert.ValueReader

  val LENGTH_MIN = 1
  val LENGTH_MAX = 8192

  case class Instance(value: String) extends Token() {
  }

  given ValueReader[Token] with
    def readC(v: Any): Consequence[Token] = Option(v) match
      case None => Consequence.valueInvalid("Invalid Token value: null")
      case Some(value) => value match
        case t: Token => Consequence.success(t)
        case s: String => Consequence.success(Instance(s))
        case other => Consequence.success(Instance(other.toString))

  given Codec[Token] = Codec.from(
    Decoder.decodeString.emap(text => Try(Token(text)).toEither.left.map(_.getMessage)),
    Encoder.encodeString.contramap(_.value)
  )

  def apply(s: String): Token = Instance(s)

  def create(s: String): Consequence[Token] = Consequence(Instance(s))

  def parse(s: String): Consequence[Token] = create(s)
}
