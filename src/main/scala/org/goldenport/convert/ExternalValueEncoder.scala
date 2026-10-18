package org.goldenport.convert

import java.nio.charset.Charset
import java.util.{Locale, TimeZone}
import cats.data.NonEmptyVector
import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.datatype.*
import org.goldenport.record.{Record, RecordPresentable}
import org.goldenport.text.Presentable
import org.goldenport.value.*

/*
 * External Record-construction projection, distinct from canonical datatype validation.
 *
 * @since   Oct. 18, 2026
 * @version Oct. 18, 2026
 */
object ExternalValueEncoder {
  type Adapter = PartialFunction[Any, Consequence[Any]]

  def encodeC(value: Any): Consequence[Any] = encodeC(value, PartialFunction.empty)

  def encodeC(value: Any, adapter: Adapter): Consequence[Any] =
    Consequence.run(_encode(value, adapter))

  private def _encode(value: Any, adapter: Adapter): Consequence[Any] = value match {
    case null => Consequence.valueInvalid("Null external value")
    case json: Json => Consequence.success(json)
    case record: Record => Consequence.success(record)
    case None => Consequence.success(None)
    case Some(child) => encodeC(child, adapter).map(Some(_))
    case _: String | _: Boolean | _: Byte | _: Short | _: Int | _: Long |
         _: BigInt | _: BigDecimal | _: java.math.BigInteger | _: java.math.BigDecimal =>
      Consequence.success(value)
    case number: Float if !number.isNaN && !number.isInfinity => Consequence.success(number)
    case _: Float => Consequence.valueInvalid("Nonfinite external number")
    case number: Double if !number.isNaN && !number.isInfinity => Consequence.success(number)
    case _: Double => Consequence.valueInvalid("Nonfinite external number")
    case character: Char => Consequence.success(character.toString)
    case values: NonEmptyVector[?] => _encode_elements(values.toVector, adapter)
    case values: scala.collection.Seq[?] => _encode_elements(values.toVector, adapter)
    case values: Array[?] => _encode_elements(values.toVector, adapter)
    case values: scala.collection.Set[?] => _encode_elements(values.toVector, adapter)
    case values: scala.collection.Map[?, ?] =>
      Consequence.zipN(values.toVector.map {
        case (key: String, child) => encodeC(child, adapter).map(key -> _)
        case _ => Consequence.valueInvalid("External Map requires String keys")
      }).map(_.toMap)
    case text: I18nLabel => Consequence(text.toI18nString.toRecord)
    case text: I18nTitle => Consequence(text.toI18nString.toRecord)
    case text: I18nBrief => Consequence(text.toI18nString.toRecord)
    case text: I18nSummary => Consequence(text.toI18nString.toRecord)
    case text: I18nDescription => Consequence(text.toI18nString.toRecord)
    case text: I18nText => Consequence(text.toI18nString.toRecord)
    case mime: MimeType => Consequence(mime.print)
    case charset: Charset => Consequence(charset.name())
    case locale: Locale => Consequence(locale.toLanguageTag)
    case timezone: TimeZone => Consequence(timezone.getID)
    case body: ContentBody => encodeC(body.value, adapter)
    case markup: ContentMarkup => encodeC(markup.value, adapter)
    case id: ObjectId => encodeC(id.id, adapter)
    case names: NameAttributes => _attributes(Vector(
      "name" -> names.name, "label" -> names.label, "title" -> names.title,
      "code" -> names.code, "alias" -> names.alias, "slug" -> names.slug), adapter)
    case description: DescriptiveAttributes => _attributes(Vector(
      "headline" -> description.headline, "brief" -> description.brief,
      "summary" -> description.summary, "description" -> description.description,
      "lead" -> description.lead, "abstract" -> description.`abstract`,
      "remarks" -> description.remarks, "tooltip" -> description.tooltip), adapter)
    case content: ContentReferenceOccurrence => _record(content)
    case content: ContentAttributes => _record(content)
    case other => adapter.lift(other) match {
      case Some(result) => result
      case None => other match {
        case presentable: RecordPresentable => _record(presentable)
        case presentable: Presentable => Consequence(presentable.print).flatMap { text =>
          if (java.util.Objects.isNull(text)) Consequence.valueInvalid("Null external presentation")
          else Consequence.success(text)
        }
        case _ => Consequence.valueInvalid("Unsupported external value")
      }
    }
  }

  private def _encode_elements(values: Vector[Any], adapter: Adapter): Consequence[Any] =
    Consequence.zipN(values.map { value =>
      encodeC(value, adapter).flatMap {
        case None => Consequence.valueInvalid("Absent external sequence element")
        case child if java.util.Objects.isNull(child) => Consequence.valueInvalid("Null external sequence element")
        case child => Consequence.success(child)
      }
    }).map(_.toVector)

  private def _attributes(fields: Vector[(String, Any)], adapter: Adapter): Consequence[Any] =
    Consequence.zipN(fields.map { case (key, value) =>
      encodeC(value, adapter).map(key -> _)
    }).flatMap(fields => Consequence(Record.dataAuto(fields*)))

  private def _record(value: RecordPresentable): Consequence[Any] =
    Consequence(value.toRecord()).flatMap { record =>
      if (java.util.Objects.isNull(record)) Consequence.valueInvalid("Null external Record presentation")
      else Consequence.success(record)
    }
}
