package org.goldenport.record.io

import scala.jdk.CollectionConverters._
import java.util.LinkedHashMap
import cats.data.NonEmptyVector

import io.circe.Json

import org.goldenport.Consequence
import org.goldenport.record.Record
import org.goldenport.schema.XSemiStructuredData
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import scala.xml.{Elem, Text}

/*
 * @since   Feb.  7, 2026
 *  version Mar.  4, 2026
 *  version Jul.  1, 2026
 * @version Oct. 18, 2026
 * @author  ASAMI, Tomoharu
 */
class RecordEncoder(
  config: RecordEncoder.Config = RecordEncoder.Config.default
) {
  def jsonC(in: Record): Consequence[String] =
    XSemiStructuredData.toJson(in).map(_.noSpaces)

  def json(in: Record): String = jsonC(in).foldIdntity(c => s"""{"error": "${c.print}"}""")

  def yamlC(in: Record): Consequence[String] =
    XSemiStructuredData.toJson(in).flatMap { json =>
      Consequence { _yaml.dump(_to_java(json)) }
    }

  def yaml(in: Record): String = yamlC(in).foldIdntity(c => s"error: ${c.print}")

  def xmlC(in: Record): Consequence[String] =
    Consequence {
      _to_xml(in)
    }

  def xml(in: Record): String = xmlC(in).foldIdntity(c => s"<error>${c.print}</error>")

  private val _yaml = {
    val options = new DumperOptions()
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
    options.setPrettyFlow(true)
    options.setAllowUnicode(true)
    new Yaml(options)
  }

  // --- private helpers ---

  private def _to_xml(record: Record): String = {
    val nodes = record.fields.map { field =>
      _field_to_node(field.key, field.value.single)
    }
    _elem("record", nodes).toString
  }

  private def _field_to_node(
    name: String,
    value: Any
  ): scala.xml.Node =
    value match {
      case r: Record =>
        _elem(_xml_name(name), r.fields.map(f => _field_to_node(f.key, f.value.single)))
      case xs: NonEmptyVector[?] =>
        _elem(_xml_name(name), xs.toVector.map(v => _field_to_node("item", v)))
      case xs: Iterable[?] =>
        _elem(_xml_name(name), xs.toVector.map(v => _field_to_node("item", v)))
      case arr: Array[?] =>
        _elem(_xml_name(name), arr.toVector.map(v => _field_to_node("item", v)))
      case other =>
        _elem(_xml_name(name), Text(other.toString))
    }

  private def _elem(
    name: String,
    children: Seq[scala.xml.Node]
  ): Elem =
    Elem(null, name, scala.xml.Null, scala.xml.TopScope, minimizeEmpty = children.isEmpty, children*)

  private def _xml_name(value: String): String = {
    val text = value.map {
      case ch if ch.isLetterOrDigit || ch == '_' || ch == '-' || ch == '.' => ch
      case _ => '_'
    }
    if (text.headOption.exists(ch => ch.isLetter || ch == '_')) text else s"_$text"
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

  def xmlC(p: Record): Consequence[String] = encoder.xmlC(p)
  def xml(p: Record): String = encoder.xml(p)
}
