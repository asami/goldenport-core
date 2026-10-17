package org.goldenport.schema

import java.math.MathContext
import io.circe.{Json, JsonObject}
import io.circe.parser
import org.goldenport.Consequence
import org.goldenport.record.Record

/*
 * Canonical JSON conversion for structured schema values.
 *
 * @since   Oct.  5, 2026
 * @version Oct.  5, 2026
 * @author  ASAMI, Tomoharu
 */
private[schema] object StructuredDataJson {
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
    if (value == null) {
      Consequence.valueInvalid(value, XSemiStructuredData)
    } else {
      _to_object(value)
    }
  }

  private def _from_object(value: JsonObject): Consequence[Record] = {
    val fields = value.toVector.map { case (key, json) =>
      _from_value(json).map(key -> _)
    }
    Consequence.zipN(fields).map(Record.createFull)
  }

  private def _from_value(value: Json): Consequence[Any] = {
    value.fold[Consequence[Any]](
      jsonNull = Consequence.success(null),
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
    val keys = value.fields.map(_.key)
    val validkeys: Consequence[Unit] = {
      if (keys.distinct.size == keys.size) {
        Consequence.success(())
      } else {
        Consequence.valueInvalid(value, XSemiStructuredData)
      }
    }
    val fields = Consequence.zipN(value.fields.map { field =>
      _to_value(field.value.single).map(field.key -> _)
    })
    validkeys.zip(fields).map { case (_, members) =>
      Json.fromJsonObject(JsonObject.fromIterable(members))
    }
  }

  private def _to_value(value: Any): Consequence[Json] = {
    value match {
      case null => Consequence.success(Json.Null)
      case record: Record => _to_object(record)
      case values: Seq[?] => _to_array(values)
      case values: Array[?] => _to_array(values.toVector)
      case string: String => Consequence.success(Json.fromString(string))
      case boolean: Boolean => Consequence.success(Json.fromBoolean(boolean))
      case number: BigInt => Consequence.success(Json.fromBigInt(number))
      case number: BigDecimal => Consequence.success(Json.fromBigDecimal(number))
      case number: java.math.BigInteger => Consequence.success(Json.fromBigInt(BigInt(number)))
      case number: java.math.BigDecimal =>
        Consequence.success(Json.fromBigDecimal(new BigDecimal(number, MathContext.UNLIMITED)))
      case number: Byte => Consequence.success(Json.fromInt(number.toInt))
      case number: Short => Consequence.success(Json.fromInt(number.toInt))
      case number: Int => Consequence.success(Json.fromInt(number))
      case number: Long => Consequence.success(Json.fromLong(number))
      case number: Float if java.lang.Float.isFinite(number) =>
        _from_decimal_text(java.lang.Float.toString(number))
      case number: Double if java.lang.Double.isFinite(number) =>
        _from_decimal_text(java.lang.Double.toString(number))
      case _ => Consequence.valueInvalid(value, XSemiStructuredData)
    }
  }

  private def _to_array(values: Seq[Any]): Consequence[Json] = {
    Consequence.zipN(values.map(_to_value)).map(Json.fromValues)
  }

  private def _from_decimal_text(text: String): Consequence[Json] = {
    val decimal = new java.math.BigDecimal(text, MathContext.UNLIMITED)
    Consequence.success(Json.fromBigDecimal(new BigDecimal(decimal, MathContext.UNLIMITED)))
  }
}
