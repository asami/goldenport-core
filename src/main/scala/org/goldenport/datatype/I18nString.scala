package org.goldenport.datatype

import cats.data.NonEmptyVector
import io.circe.{Codec, Decoder, Encoder, Json}
import io.circe.syntax.*
import io.circe.parser.parse
import org.goldenport.Consequence
import org.goldenport.context.ExecutionContext
import org.goldenport.convert.{StringCodex, StringCodexable, StringEncoder, ValueReader}
import org.goldenport.record.Record
import org.goldenport.schema.XString
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
 *  version Apr. 17, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
case class I18nString(
  entries: NonEmptyVector[(java.util.Locale, String)]
) extends StringCodexable {
  I18nString._require_valid(entries)

  def encode(using ctx: ExecutionContext): String =
    summon[StringCodex[I18nString]].encode(this)

  def displayMessage: String = {
    val prioritized = Vector(java.util.Locale.ROOT, java.util.Locale.ENGLISH, java.util.Locale.JAPANESE)
    val bylocale = entries.toVector.toMap
    prioritized.iterator
      .map(bylocale.get)
      .collectFirst { case Some(value) => value }
      .getOrElse(entries.head._2)
  }

  def displayMessage(locale: java.util.Locale): String =
    displayMessage(Vector(locale))

  def displayMessage(locales: Iterable[java.util.Locale]): String = {
    val requested = locales.iterator.filter(_ != null).toVector
    val priorities =
      (requested ++ requested.map(_root_locale) ++ Vector(java.util.Locale.ROOT, java.util.Locale.ENGLISH, java.util.Locale.JAPANESE))
        .distinct
    val bylocale = entries.toVector.toMap
    priorities.iterator
      .map(bylocale.get)
      .collectFirst { case Some(value) => value }
      .getOrElse(entries.head._2)
  }

  def toRecord: Record =
    Record.create(entries.toVector.map { case (locale, value) =>
      locale.toLanguageTag -> value
    })

  def validateAllowedLocales(allowedlocales: Set[java.util.Locale]): Consequence[I18nString] =
    I18nString.validateAllowedLocales(this, allowedlocales)

  private def _root_locale(locale: java.util.Locale): java.util.Locale =
    if (locale == null || locale.getLanguage.isEmpty)
      java.util.Locale.ROOT
    else
      java.util.Locale.forLanguageTag(locale.getLanguage)
}

object I18nString {
  given Encoder[java.util.Locale] = Encoder.encodeString.contramap(_.toLanguageTag)
  given Decoder[java.util.Locale] = Decoder.decodeString.emap(_parse_locale_tag)

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
        case Some(x) =>
          val entries = NonEmptyVector(x, xs.tail)
          _validate_entries(entries, None)
            .left.map(message => io.circe.DecodingFailure(message, c.history))
            .map(_ => I18nString(entries))
        case None => Left(io.circe.DecodingFailure("entries must be non-empty", c.history))
      }
    }
  }

  def apply(p: String): I18nString =
    I18nString(NonEmptyVector.one(java.util.Locale.ROOT -> p))

  given ValueReader[I18nString] with
    def readC(value: Any): Consequence[I18nString] = value match {
      case p: I18nString => Consequence.success(p)
      case p: String => _decode_for_storage(p)
      case p: Record => _decode_record(p)
      case _ => Consequence.valueInvalid(value, XString)
    }
    override def readContextC(value: Any)(using ctx: ExecutionContext): Consequence[I18nString] =
      value match {
        case p: I18nString => _create(p.entries, ctx.i18n.allowedLocales)
        case p: String => decode(p)
        case p: Record => _decode_record(p, ctx.i18n.allowedLocales)
        case _ => Consequence.valueInvalid(value, XString)
      }

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
        _create(NonEmptyVector.one(ctx.locale -> p.drop(1)), ctx.i18n.allowedLocales)
      else if (p.startsWith("{"))
        parse(p).flatMap(_.as[I18nString]) match {
          case Right(s) => _create(s.entries, ctx.i18n.allowedLocales)
          case Left(e) => Consequence.valueFormatError(e.getMessage)
        }
      else
        _create(NonEmptyVector.one(ctx.locale -> p), ctx.i18n.allowedLocales)

  def decode(p: String)(using ctx: ExecutionContext): Consequence[I18nString] =
    summon[StringCodex[I18nString]].decode(p)

  def readC(p: Any): Consequence[I18nString] =
    summon[ValueReader[I18nString]].readC(p)

  def readContextC(p: Any)(using ExecutionContext): Consequence[I18nString] =
    summon[ValueReader[I18nString]].readContextC(p)

  def create(entries: NonEmptyVector[(java.util.Locale, String)]): Consequence[I18nString] =
    _create(entries, None)

  def create(
    entries: NonEmptyVector[(java.util.Locale, String)],
    allowedlocales: Set[java.util.Locale]
  ): Consequence[I18nString] =
    _create(entries, Some(allowedlocales))

  def validateAllowedLocales(
    value: I18nString,
    allowedlocales: Set[java.util.Locale]
  ): Consequence[I18nString] =
    _create(value.entries, Some(allowedlocales))

  private[datatype] def semanticCodec[A](
    create: I18nString => A,
    extract: A => I18nString
  ): Codec[A] =
    Codec.from(
      summon[Decoder[I18nString]].map(create),
      summon[Encoder[I18nString]].contramap(extract)
    )

  private def _decode_for_storage(p: String): Consequence[I18nString] = {
    given ExecutionContext = StringEncoder.storageExecutionContext
    decode(p)
  }

  private def _decode_record(
    p: Record,
    allowedlocales: Option[Set[java.util.Locale]] = None
  ): Consequence[I18nString] =
    if (p.fields.exists(_.key == "entries"))
      p.toJsonStringC.flatMap { encoded =>
        parse(encoded).flatMap(_.as[I18nString]) match {
          case Right(value) => _create(value.entries, allowedlocales)
          case Left(e) => Consequence.valueFormatError(e.getMessage)
        }
      }
    else {
      val entries = p.fields.map { field =>
        field.value.single match {
          case value: String =>
            _parse_locale_tag(field.key) match {
              case Right(locale) => Consequence.success(locale -> value)
              case Left(message) => Consequence.valueFormatError(message)
            }
          case value =>
            Consequence.valueInvalid(value, XString)
        }
      }
      entries.foldLeft(Consequence.success(Vector.empty[(java.util.Locale, String)])) { (z, x) =>
        z.zip(x).map { case (xs, entry) => xs :+ entry }
      }.flatMap {
        case head +: tail => _create(NonEmptyVector(head, tail), allowedlocales)
        case _ => Consequence.valueInvalid(p, XString)
      }
    }

  private def _create(
    entries: NonEmptyVector[(java.util.Locale, String)],
    allowedlocales: Option[Set[java.util.Locale]]
  ): Consequence[I18nString] =
    _validate_entries(entries, allowedlocales) match {
      case Right(_) => Consequence.success(I18nString(entries))
      case Left(message) => Consequence.valueInvalid(message)
    }

  private def _validate_entries(
    entries: NonEmptyVector[(java.util.Locale, String)],
    allowedlocales: Option[Set[java.util.Locale]]
  ): Either[String, Unit] = {
    val locales = entries.toVector.map(_._1)
    val nullindex = locales.indexWhere(_ == null)
    if (nullindex >= 0)
      Left(s"locale must not be null at entry $nullindex")
    else {
      val duplicatelocales = locales.groupBy(identity).collect {
        case (locale, xs) if xs.sizeIs > 1 => locale.toLanguageTag
      }.toVector.sorted
      if (duplicatelocales.nonEmpty)
        Left(s"duplicate locale entries: ${duplicatelocales.mkString(", ")}")
      else
        allowedlocales match {
          case Some(allowed) =>
            val unsupported = locales.filterNot(allowed).map(_.toLanguageTag).distinct.sorted
            if (unsupported.nonEmpty)
              Left(s"locale entries are not allowed: ${unsupported.mkString(", ")}")
            else
              Right(())
          case None => Right(())
        }
    }
  }

  private def _parse_locale_tag(tag: String): Either[String, java.util.Locale] =
    try {
      if (tag == null || tag.isEmpty)
        Left("locale tag must not be empty")
      else
        Right(new java.util.Locale.Builder().setLanguageTag(tag).build())
    } catch {
      case _: java.util.IllformedLocaleException => Left(s"invalid BCP 47 locale tag: $tag")
    }

  private def _require_valid(entries: NonEmptyVector[(java.util.Locale, String)]): Unit =
    _validate_entries(entries, None).fold(message => throw new IllegalArgumentException(message), identity)

  private def _escape_plain(p: String): String =
    if (p.startsWith("{")) s"""\\$p""" else p

  private def _is_plain_locale(
    valuelocale: java.util.Locale,
    contextlocale: java.util.Locale
  ): Boolean =
    valuelocale == java.util.Locale.ROOT || valuelocale == contextlocale
}
