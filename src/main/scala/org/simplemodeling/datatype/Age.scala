package org.simplemodeling.datatype

import scala.language.strictEquality
import cats.*
import cats.implicits.*
import cats.syntax.all.*
import io.circe.{Codec, Decoder, Encoder, HCursor, Json}
import org.simplemodeling.Consequence
import org.simplemodeling.convert.ValueReader

/*
 * @since   Sep. 17, 2025
 * @version Nov. 19, 2025
 * @author  ASAMI, Tomoharu
 */
abstract class Age() extends NonNegativeInt() {
  import Age._

  override protected def value_Max = Some(MAX_DEFAULT)
}

object Age {
  final val MAX_DEFAULT = 150

  given ValueReader[Age] with
      def read(v: Any): Option[Age] = ???

  // Age is serialized as an integer.
  // Decoder uses emap so validation errors can be returned later.
  given Codec[Age] = Codec.from(
    Decoder.decodeInt.emap { n =>
      // Add validation logic if needed:
      // if (n < MIN || n > MAX) then Left("Invalid age value")
      Right(Age(n))
    },
    Encoder.encodeInt.contramap(_.value)
  )

  case class Instance(value: Int) extends Age() {
  }

  def apply(p: Int): Age = Instance(p)

  def createC(p: Int): Consequence[Age] = Consequence(Instance(p))

  def parse(p: String): Consequence[Age] = Consequence.run(createC(p.toInt))
}
