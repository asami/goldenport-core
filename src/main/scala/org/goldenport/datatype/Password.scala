package org.goldenport.datatype

import scala.util.Try
import io.circe.{Codec, Decoder, Encoder}
import org.goldenport.Consequence
import org.goldenport.convert.ValueReader

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class Password() extends StringDataType() {
  import Password._

  protected final def length_min: Int = LENGTH_MIN
  protected final def length_max: Int = LENGTH_MAX
  protected final def is_valid(value: String): Boolean = validate_printable(value)
}

object Password {
  val LENGTH_MIN = 1
  val LENGTH_MAX = 1024

  final case class Instance(value: String) extends Password()

  given ValueReader[Password] with
    def readC(value: Any): Consequence[Password] = Option(value) match
      case None => Consequence.valueInvalid("Invalid Password value: null")
      case Some(password: Password) => Consequence.success(password)
      case Some(text: String) => create(text)
      case Some(other) => create(other.toString)

  given Codec[Password] = Codec.from(
    Decoder.decodeString.emap(text => Try(Password(text)).toEither.left.map(_.getMessage)),
    Encoder.encodeString.contramap(_.value)
  )

  def apply(value: String): Password = Instance(value)

  def create(value: String): Consequence[Password] = Consequence(Instance(value))

  def parse(value: String): Consequence[Password] = create(value)
}
