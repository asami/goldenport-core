package org.goldenport.record.io

import scala.jdk.CollectionConverters._
import java.util.LinkedHashMap

import io.circe.Json

import org.goldenport.Consequence
import org.goldenport.record.Record
import org.goldenport.record.Field
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

/*
 * @since   Feb.  7, 2026
 *  version Feb.  7, 2026
 * @version Mar.  4, 2026
 * @author  ASAMI, Tomoharu
 */
class RecordEncoder(
  config: RecordEncoder.Config = RecordEncoder.Config.default
) {
  def jsonC(in: Record): Consequence[String] =
    Consequence {
      _to_json(in).noSpaces
    }

  def json(in: Record): String = jsonC(in).foldIdntity(c => s"""{"error": "${c.print}"}""")

  def yamlC(in: Record): Consequence[String] =
    Consequence {
      val json = _to_json(in)
      _yaml.dump(_to_java(json))
    }

  def yaml(in: Record): String = yamlC(in).foldIdntity(c => s"error: ${c.print}")

  private val _yaml = {
    val options = new DumperOptions()
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
    options.setPrettyFlow(true)
    new Yaml(options)
  }

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
      jsonNumber = num =>
        num.toBigInt match {
          case Some(bi) if bi.isValidInt => Int.box(bi.intValue)
          case Some(bi) if bi.isValidLong => Long.box(bi.longValue)
          case Some(bi) => bi.bigInteger
          case None =>
            num.toBigDecimal
              .map(_.bigDecimal)
              .getOrElse(java.math.BigDecimal.valueOf(num.toDouble))
        },
      jsonString = identity,
      jsonArray = arr => arr.map(_to_java).asJava,
      jsonObject = obj => {
        val m = new LinkedHashMap[String, Any]()
        obj.toIterable.foreach { case (k, v) =>
          m.put(k, _to_java(v))
        }
        m
      }
    )
}

object RecordEncoder {
  val encoder = RecordEncoder()

  case class Config()

  object Config {
    val default: Config = Config()
  }

  def jsonC(p: Record): Consequence[String] = encoder.jsonC(p)
  def json(p: Record): String = encoder.json(p)

  def yamlC(p: Record): Consequence[String] = encoder.yamlC(p)
  def yaml(p: Record): String = encoder.yaml(p)
}
