package org.goldenport.record.io

import scala.jdk.CollectionConverters._

import io.circe.Json

import org.goldenport.Consequence
import org.goldenport.record.Record
import org.goldenport.record.Field
import org.yaml.snakeyaml.Yaml

/*
 * @since   Feb.  7, 2026
 * @version Feb.  7, 2026
 * @author  ASAMI, Tomoharu
 */
class RecordEncoder(
  config: RecordEncoder.Config = RecordEncoder.Config.default
) {
  def json(in: Record): Consequence[String] =
    Consequence {
      _to_json(in).noSpaces
    }

  def yaml(in: Record): Consequence[String] =
    Consequence {
      val json = _to_json(in)
      _yaml.dump(_to_java(json))
    }

  private val _yaml = new Yaml()

  // --- private helpers ---

  private def _to_json(record: Record): Json =
    Json.fromJsonObject(
      io.circe.JsonObject.fromIterable(
        record.fields.collect {
          case Field(key, Field.Value.Single(value)) => key -> _to_json_value(value)
        }
      )
    )

  private def _to_json_value(value: Any): Json =
    value match {
      case null => Json.Null
      case r: Record => _to_json(r)
      case seq: Iterable[?] => Json.fromValues(seq.map(_to_json_value))
      case arr: Array[?] => Json.fromValues(arr.toSeq.map(_to_json_value))
      case b: java.lang.Boolean => Json.fromBoolean(b)
      case n: java.lang.Number => Json.fromBigDecimal(BigDecimal(n.toString))
      case s: String => Json.fromString(s)
      case other => Json.fromString(other.toString)
    }

  private def _to_java(json: Json): Any =
    json.fold(
      jsonNull = null,
      jsonBoolean = identity,
      jsonNumber = num => num.toBigDecimal.getOrElse(BigDecimal(num.toDouble)),
      jsonString = identity,
      jsonArray = arr => arr.map(_to_java).asJava,
      jsonObject = obj =>
        obj.toMap.map { case (k, v) => k -> _to_java(v) }.asJava
    )
}

object RecordEncoder {
  case class Config()

  object Config {
    val default: Config = Config()
  }
}
