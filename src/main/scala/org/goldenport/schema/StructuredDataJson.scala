package org.goldenport.schema

import java.math.MathContext
import cats.data.NonEmptyVector
import io.circe.{Json, JsonObject}
import io.circe.parser
import org.goldenport.Consequence
import org.goldenport.record.Record

/*
 * Canonical JSON conversion for structured schema values.
 *
 * @since   Oct.  5, 2026
 * @version Oct. 18, 2026
 * @author  ASAMI, Tomoharu
 */
private[goldenport] object StructuredDataJson {
  def fromJsonText(text: String): Consequence[Json] = {
    if (text == null) {
      Consequence.valueFormatError(text, XJson)
    } else {
      parser.parse(text) match {
        case Right(json) => Consequence.success(json)
        case Left(_) => Consequence.valueFormatError(text, XJson)
      }
    }
  }

  def fromJson(value: Json): Consequence[Record] = {
    if (value == null) {
      Consequence.valueInvalid(value, XSemiStructuredData)
    } else {
      value.asObject match {
        case Some(jsonobject) => _from_object(jsonobject)
        case None => Consequence.valueInvalid(value, XSemiStructuredData)
      }
    }
  }

  def toJson(value: Record): Consequence[Json] = {
    normalize(value).flatMap(_to_object)
  }

  def valueToJson(value: Any): Consequence[Json] =
    _normalize_value(value).flatMap(_to_value)

  def normalize(value: Record): Consequence[Record] = {
    if (value == null) {
      Consequence.valueInvalid(value, XSemiStructuredData)
    } else {
      val keys = value.fields.map(_.key)
      val validkeys: Consequence[Unit] = {
        if (keys.distinct.size == keys.size) {
          Consequence.success(())
        } else {
          Consequence.valueInvalid(value, XSemiStructuredData)
        }
      }
      val fields = Consequence.zipN(value.fields.map { field =>
        _normalize_value(field.value.single).map(field.key -> _)
      })
      validkeys.zip(fields).map { case (_, members) => Record.createFull(members) }
    }
  }

  private def _from_object(value: JsonObject): Consequence[Record] = {
    val fields = value.toVector.filterNot(_._2.isNull).map { case (key, json) =>
      _from_value(json).map(key -> _)
    }
    Consequence.zipN(fields).map(Record.createFull)
  }

  private def _from_value(value: Json): Consequence[Any] = {
    value.fold[Consequence[Any]](
      jsonNull = Consequence.valueInvalid(value, XSemiStructuredData),
      jsonBoolean = boolean => Consequence.success(boolean),
      jsonNumber = number => {
        number.toBigDecimal match {
          case Some(decimal) =>
            Consequence.success(new BigDecimal(decimal.bigDecimal, MathContext.UNLIMITED))
          case None => Consequence.valueInvalid(value, XSemiStructuredData)
        }
      },
      jsonString = string => Consequence.success(string),
      jsonArray = array => Consequence.zipN(array.map(_from_value)).map(_.toVector),
      jsonObject = _from_object
    )
  }

  private def _to_object(value: Record): Consequence[Json] = {
    val fields = Consequence.zipN(value.fields.map { field =>
      _to_value(field.value.single).map(field.key -> _)
    })
    fields.map { members =>
      Json.fromJsonObject(JsonObject.fromIterable(members))
    }
  }

  private def _to_value(value: Any): Consequence[Json] = {
    value match {
      case record: Record => _to_object(record)
      case values: Vector[?] => Consequence.zipN(values.map(_to_value)).map(Json.fromValues)
      case string: String => Consequence.success(Json.fromString(string))
      case boolean: Boolean => Consequence.success(Json.fromBoolean(boolean))
      case number: BigDecimal => Consequence.success(Json.fromBigDecimal(number))
      case _ => Consequence.valueInvalid(value, XSemiStructuredData)
    }
  }

  private def _normalize_value(value: Any): Consequence[Any] = {
    value match {
      case record: Record => normalize(record)
      case values: NonEmptyVector[?] => _normalize_array(values.toVector)
      case values: Seq[?] => _normalize_array(values)
      case values: Array[?] => _normalize_array(values.toVector)
      case string: String => Consequence.success(string)
      case boolean: Boolean => Consequence.success(boolean)
      case number: BigInt => _from_decimal_text(number.toString)
      case number: BigDecimal =>
        Consequence.success(new BigDecimal(number.bigDecimal, MathContext.UNLIMITED))
      case number: java.math.BigInteger =>
        Consequence.success(new BigDecimal(new java.math.BigDecimal(number), MathContext.UNLIMITED))
      case number: java.math.BigDecimal =>
        Consequence.success(new BigDecimal(number, MathContext.UNLIMITED))
      case number: Byte => _from_decimal_text(number.toString)
      case number: Short => _from_decimal_text(number.toString)
      case number: Int => _from_decimal_text(number.toString)
      case number: Long => _from_decimal_text(number.toString)
      case number: Float if java.lang.Float.isFinite(number) =>
        _from_decimal_text(java.lang.Float.toString(number))
      case number: Double if java.lang.Double.isFinite(number) =>
        _from_decimal_text(java.lang.Double.toString(number))
      case _ => Consequence.valueInvalid(value, XSemiStructuredData)
    }
  }

  private def _normalize_array(values: Seq[Any]): Consequence[Vector[Any]] = {
    Consequence.zipN(values.map(_normalize_value)).map(_.toVector)
  }

  private def _from_decimal_text(text: String): Consequence[BigDecimal] = {
    val decimal = new java.math.BigDecimal(text, MathContext.UNLIMITED)
    Consequence.success(new BigDecimal(decimal, MathContext.UNLIMITED))
  }
}
