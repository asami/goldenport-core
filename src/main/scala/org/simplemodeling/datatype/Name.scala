package org.simplemodeling.datatype

import scala.language.strictEquality
import cats.*
import cats.implicits.*
import cats.syntax.all.*
import io.circe.{Codec, Decoder, Encoder, HCursor, Json}
import org.simplemodeling.Consequence
import org.simplemodeling.convert.ValueReader

/*
 * @since   Jul. 20, 2025
 *  version Jul. 23, 2025
 *  version Sep. 17, 2025
 * @version Nov. 19, 2025
 * @author  ASAMI, Tomoharu
 */
abstract class Name() extends StringDataType() {
  import Name._

  protected final def length_min: Int = LENGTH_MIN
  protected final def length_max: Int = LENGTH_MAX
  protected final def is_valid(s: String): Boolean = validate_printable(s)

//  def titleName: String = StringUtils.makeTitle(name)
}

object Name {
  val LENGTH_MIN = 1
  val LENGTH_MAX = 64

  given Codec[Name] = Codec.from(
    Decoder.decodeString.emap { s =>
      Right(Name(s))
    },
    Encoder.encodeString.contramap(_.value)
  )

  case class Instance(value: String) extends Name() {
  }

  given ValueReader[Name] with
      def read(v: Any): Option[Name] = ???

  def apply(s: String): Name = Instance(s)

  def parse(s: String): Consequence[Name] = Consequence(Instance(s))
}
