package org.goldenport.datatype

import cats.data.NonEmptyVector
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.syntax.*
import io.circe.parser.parse
import org.goldenport.Consequence
import org.goldenport.context.ExecutionContext
import org.goldenport.convert.{StringCodex, StringCodexable}
/*
 * @since   Apr. 17, 2020
 *  version Jun.  1, 2020
 *  version Mar. 27, 2021
 *  version Jun. 20, 2021
 *  version Feb.  9, 2022
 *  version Jun. 13, 2022
 *  version Dec. 28, 2022
 *  version May. 11, 2025
 *  version Jul. 23, 2025
 *  version Dec. 25, 2025
 *  version Apr. 11, 2026
 * @version Apr. 14, 2026
 * @author  ASAMI, Tomoharu
 */
case class I18nString(
  entries: NonEmptyVector[(java.util.Locale, String)]
) extends StringCodexable {
  def encode(using ctx: ExecutionContext): String =
    summon[StringCodex[I18nString]].encode(this)

  def displayMessage: String = {
    val prioritized = Vector(java.util.Locale.ROOT, java.util.Locale.ENGLISH, java.util.Locale.JAPANESE)
    val byLocale = entries.toVector.toMap
    prioritized.iterator
      .map(byLocale.get)
      .collectFirst { case Some(value) => value }
      .getOrElse(entries.head._2)
  }
}

object I18nString {
  given Encoder[java.util.Locale] = Encoder.encodeString.contramap(_.toLanguageTag)
  given Decoder[java.util.Locale] = Decoder.decodeString.map(java.util.Locale.forLanguageTag)

  given Encoder[I18nString] = Encoder.instance { p =>
    Json.obj(
      "entries" -> p.entries.toVector.map { case (locale, value) =>
        Json.arr(locale.asJson, value.asJson)
      }.asJson
    )
  }

  given Decoder[I18nString] = Decoder.instance { c =>
    c.downField("entries").as[Vector[(java.util.Locale, String)]].flatMap { xs =>
      xs.headOption match {
        case Some(x) => Right(I18nString(NonEmptyVector(x, xs.tail)))
        case None => Left(io.circe.DecodingFailure("entries must be non-empty", c.history))
      }
    }
  }

  def apply(p: String): I18nString =
    I18nString(NonEmptyVector.one(java.util.Locale.ROOT -> p))

  given StringCodex[I18nString] with
    def encode(p: I18nString)(using ctx: ExecutionContext): String =
      p.entries.toVector match {
        case Vector((locale, value)) if _is_plain_locale(locale, ctx.locale) =>
          _escape_plain(value)
        case _ =>
          p.asJson.noSpaces
      }

    def decode(p: String)(using ctx: ExecutionContext): Consequence[I18nString] =
      if (p.startsWith("""\{"""))
        Consequence.success(I18nString(NonEmptyVector.one(ctx.locale -> p.drop(1))))
      else if (p.startsWith("{"))
        parse(p).flatMap(_.as[I18nString]) match {
          case Right(s) => Consequence.success(s)
          case Left(e) => Consequence.valueFormatError(e.getMessage)
        }
      else
        Consequence.success(I18nString(NonEmptyVector.one(ctx.locale -> p)))

  def decode(p: String)(using ctx: ExecutionContext): Consequence[I18nString] =
    summon[StringCodex[I18nString]].decode(p)

  private def _escape_plain(p: String): String =
    if (p.startsWith("{")) s"""\\$p""" else p

  private def _is_plain_locale(
    valueLocale: java.util.Locale,
    contextLocale: java.util.Locale
  ): Boolean =
    valueLocale == java.util.Locale.ROOT || valueLocale == contextLocale
}
