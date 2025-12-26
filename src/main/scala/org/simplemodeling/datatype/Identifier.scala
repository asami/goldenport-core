package org.goldenport.datatype

import scala.language.strictEquality
import cats.*
import cats.implicits.*
import cats.syntax.all.*
import io.circe.{Codec, Decoder, Encoder, HCursor, Json}
import org.goldenport.Consequence
import org.goldenport.convert.ValueReader

/*
 * @since   Jul. 20, 2025
 *  version Jul. 23, 2025
 * @version Nov. 19, 2025
 * @author  ASAMI, Tomoharu
 */
abstract class Identifier() extends StringDataType() {
  import Identifier._

  def value: String

  protected def length_min: Int = LENGTH_MIN
  protected def length_max: Int = LENGTH_MAX
  protected final def is_valid(s: String): Boolean = validate_identifier(s)
}

object Identifier {
  val LENGTH_MIN = 1
  val LENGTH_MAX = 1000

  given CanEqual[Identifier, Identifier] = CanEqual.derived

  given Eq[Identifier] = Eq.by(_.value)

  given ValueReader[Identifier] with
      def read(v: Any): Option[Identifier] = ???

  // Identifier is serialized as a plain string.
  // Decoder uses emap so validation errors can be returned later.
  given Codec[Identifier] = Codec.from(
    Decoder.decodeString.emap { s =>
      // NOTE:
      // If you want to apply validation, call validate_identifier(s)
      // or wrap the validation rules here.
      Right(Identifier(s))
    },
    Encoder.encodeString.contramap(_.value)
  )

  case class Instance(value: String) extends Identifier()

  def apply(id: String): Identifier = Instance(id)

  def parse(id: String): Consequence[Identifier] = Consequence(apply(id))
}
