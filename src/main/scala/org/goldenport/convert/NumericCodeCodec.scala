package org.goldenport.convert

import io.circe.{Codec, Decoder, DecodingFailure, Encoder, Json}
import org.goldenport.Consequence

/*
 * Typed correspondence between a value and its explicitly assigned integer code.
 *
 * @since   Oct. 19, 2026
 * @version Oct. 19, 2026
 */
trait NumericCodeCodec[A] {
  def encodeC(value: A): Consequence[Int]
  def decodeC(code: Int): Consequence[A]

  final def jsonCodec: Codec[A] = NumericCodeCodec.jsonCodec(this)
}

object NumericCodeCodec {
  def instance[A](
    codeOf: A => Option[Int],
    valueOf: Int => Option[A]
  ): NumericCodeCodec[A] = new NumericCodeCodec[A] {
    def encodeC(value: A): Consequence[Int] = Consequence.run {
      codeOf(value) match {
        case Some(code) => Consequence.success(code)
        case None => Consequence.valueInvalid("No numeric code is assigned to the value")
      }
    }

    def decodeC(code: Int): Consequence[A] = Consequence.run {
      valueOf(code) match {
        case Some(value) => Consequence.success(value)
        case None => Consequence.valueInvalid(s"Unknown numeric code: $code")
      }
    }
  }

  def jsonCodec[A](codec: NumericCodeCodec[A]): Codec[A] = Codec.from(
    Decoder.instance[A] { cursor =>
      val result = Consequence.run {
        cursor.value.asNumber.flatMap(_.toBigDecimal).filter(_.isWhole).filter(_.isValidInt) match {
          case Some(number) => codec.decodeC(number.toInt)
          case None => Consequence.valueInvalid("Expected an exact JSON integer in the Int range")
        }
      }
      result match {
        case Consequence.Success(value) => Right(value)
        case Consequence.Failure(conclusion) => Left(DecodingFailure(conclusion.print, cursor.history))
      }
    },
    Encoder.instance[A](value => Json.fromInt(Consequence.run(codec.encodeC(value)).TAKE))
  )
}
