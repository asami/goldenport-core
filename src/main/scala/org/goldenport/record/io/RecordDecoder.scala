package org.goldenport.record.io

import java.io.InputStream
import java.nio.charset.StandardCharsets

import scala.jdk.CollectionConverters._

import io.circe.Json
import io.circe.parser.parse

import org.goldenport.Consequence
import org.goldenport.bag.Bag
import org.goldenport.record.Record
import org.goldenport.record.Field

import org.yaml.snakeyaml.Yaml

/*
 * @since   Feb.  7, 2026
 * @version Feb.  7, 2026
 * @author  ASAMI, Tomoharu
 */
class RecordDecoder(
  config: RecordDecoder.Config = RecordDecoder.Config.default
) {
  def json(in: String): Consequence[Record] =
    _decode_json(in)

  def json(in: Bag): Consequence[Record] =
    in.asString().flatMap(_decode_json)

  def json(in: InputStream): Consequence[Record] =
    _read_all(in).flatMap(_decode_json)

  def yaml(in: String): Consequence[Record] =
    _decode_yaml(in)

  def yaml(in: Bag): Consequence[Record] =
    in.asString().flatMap(_decode_yaml)

  def yaml(in: InputStream): Consequence[Record] =
    _read_all(in).flatMap(_decode_yaml)

  // --- private helpers ---

  private val _yaml_parser = new Yaml()

  private def _decode_json(text: String): Consequence[Record] =
    Consequence {
      parse(text) match {
        case Right(json) => _to_record(json)
        case Left(e)     => throw e
      }
    }

  private def _decode_yaml(text: String): Consequence[Record] =
    Consequence {
      val yamlValue = _yaml_parser.load(text)
      val jsonString = _to_json(yamlValue).noSpaces
      parse(jsonString) match {
        case Right(json) => _to_record(json)
        case Left(e)     => throw e
      }
    }

  private def _to_json(value: Any): Json =
    value match {
      case null => Json.Null
      case b: java.lang.Boolean => Json.fromBoolean(b)
      case n: java.lang.Number => Json.fromBigDecimal(BigDecimal(n.toString))
      case s: String => Json.fromString(s)
      case m: java.util.Map[?, ?] =>
        Json.fromJsonObject(
          io.circe.JsonObject.fromIterable(
            m.asScala.toIterable.collect { case (k: String, v) => k -> _to_json(v) }
          )
        )
      case c: java.util.Collection[?] =>
        Json.fromValues(c.asScala.map(_to_json).toSeq)
      case arr: Array[?] =>
        Json.fromValues(arr.toSeq.map(_to_json))
      case other => Json.fromString(other.toString)
    }

  private def _to_value(json: Json): Any =
    json.fold(
      jsonNull = null,
      jsonBoolean = identity,
      jsonNumber = num => num.toBigDecimal.getOrElse(BigDecimal(num.toDouble)),
      jsonString = identity,
      jsonArray = arr => arr.map(_to_value).toVector,
      jsonObject = obj =>
        Record.create(
          obj.toIterable.map { case (k, v) => k -> _to_value(v) }.toVector
        )
    )

  private def _to_record(json: Json): Record =
    json.asObject
      .map(obj =>
        Record.create(
          obj.toIterable.map { case (k, v) => k -> _to_value(v) }.toVector
        )
      )
      .getOrElse(Record.empty)

  private def _read_all(in: InputStream): Consequence[String] =
    Consequence {
      new String(in.readAllBytes(), StandardCharsets.UTF_8)
    }
}

object RecordDecoder {
  case class Config()

  object Config {
    val default: Config = Config()
  }
}
