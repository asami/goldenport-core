package org.goldenport.schema

import java.io.StringReader
import java.math.MathContext
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal
import io.circe.Json
import io.circe.parser
import org.goldenport.Consequence
import org.goldenport.record.Record
import org.yaml.snakeyaml.{LoaderOptions, Yaml}
import org.yaml.snakeyaml.nodes.{MappingNode, Node, ScalarNode, SequenceNode, Tag}

/*
 * Explicit external text conversion into canonical structured values.
 *
 * @since   Oct.  5, 2026
 * @version Oct. 18, 2026
 * @author  ASAMI, Tomoharu
 */
private[goldenport] object StructuredDataText {
  def fromText(text: String, format: StructuredDataFormat): Consequence[Record] = {
    if (format == null) {
      _invalid("Absent structured data format")
    } else if (text == null) {
      Consequence.valueFormatError(text, XSemiStructuredData)
    } else {
      val decoded = format match {
        case StructuredDataFormat.Json => _parse_json(text).flatMap(XSemiStructuredData.fromJson)
        case StructuredDataFormat.Yaml => _compose_yaml(text).flatMap(_from_yaml)
        case StructuredDataFormat.Hocon => StructuredDataHocon.fromText(text)
      }
      decoded.flatMap(XSemiStructuredData.normalizer.normalize)
    }
  }

  def fromJsonRecords(text: String, allowSingle: Boolean): Consequence[Vector[Record]] =
    _parse_json(text).flatMap { json =>
      json.asArray match {
        case Some(values) => Consequence.zipN(values.map(XSemiStructuredData.fromJson)).map(_.toVector)
        case None if allowSingle => XSemiStructuredData.fromJson(json).map(Vector(_))
        case None => _invalid("JSON records require an array of objects")
      }
    }

  def fromYamlRecords(text: String, allowSingle: Boolean): Consequence[Vector[Record]] =
    _compose_yaml(text).flatMap {
      case Some(sequence: SequenceNode) if sequence.getTag == Tag.SEQ =>
        Consequence.zipN(sequence.getValue.asScala.toVector.map { node =>
          _from_yaml(Some(node)).flatMap(XSemiStructuredData.normalizer.normalize)
        }).map(_.toVector)
      case node @ Some(_) if allowSingle =>
        _from_yaml(node).flatMap(XSemiStructuredData.normalizer.normalize).map(Vector(_))
      case _ => _invalid("YAML records require a list of mappings")
    }

  private def _parse_json(text: String): Consequence[Json] = {
    try {
      parser.parse(text) match {
        case Right(json) => Consequence.success(json)
        case Left(_) => Consequence.valueFormatError("Malformed JSON text", XSemiStructuredData)
      }
    } catch {
      case NonFatal(_) => Consequence.valueFormatError("Malformed JSON text", XSemiStructuredData)
    }
  }

  private def _compose_yaml(text: String): Consequence[Option[Node]] = {
    try {
      val options = new LoaderOptions()
      options.setMaxAliasesForCollections(Int.MaxValue)
      Consequence.success(Option(new Yaml(options).compose(new StringReader(text))))
    } catch {
      case NonFatal(_) => Consequence.valueFormatError("Malformed YAML document", XSemiStructuredData)
    }
  }

  private def _from_yaml(node: Option[Node]): Consequence[Record] = {
    node match {
      case Some(mapping: MappingNode) if mapping.getTag == Tag.MAP =>
        _yaml_mapping(mapping, List(mapping))
      case _ => _invalid("YAML root must be a mapping")
    }
  }

  private def _decode_yaml(node: Node, ancestors: List[Node]): Consequence[Any] = {
    if (ancestors.exists(_ eq node)) {
      _invalid("Cyclic YAML alias")
    } else {
      val path = node :: ancestors
      node match {
        case mapping: MappingNode if mapping.getTag == Tag.MAP => _yaml_mapping(mapping, path)
        case sequence: SequenceNode if sequence.getTag == Tag.SEQ =>
          Consequence.zipN(sequence.getValue.asScala.toVector.map(_decode_yaml(_, path)))
            .map(_.toVector)
        case scalar: ScalarNode if scalar.getTag == Tag.STR => Consequence.success(scalar.getValue)
        case scalar: ScalarNode if scalar.getTag == Tag.BOOL => _yaml_boolean(scalar.getValue)
        case scalar: ScalarNode if scalar.getTag == Tag.NULL => _invalid("Null YAML sequence element")
        case scalar: ScalarNode if scalar.getTag == Tag.INT || scalar.getTag == Tag.FLOAT =>
          _yaml_number(scalar)
        case _ => _invalid("Unsupported YAML node tag")
      }
    }
  }

  private def _yaml_mapping(node: MappingNode, ancestors: List[Node]): Consequence[Record] = {
    val members = node.getValue.asScala.toVector
    val keys = members.flatMap { member =>
      member.getKeyNode match {
        case key: ScalarNode if key.getTag == Tag.STR => Some(key.getValue)
        case _ => None
      }
    }
    val validkeys: Consequence[Unit] = {
      if (keys.distinct.size == keys.size) {
        Consequence.success(())
      } else {
        _invalid("Duplicate YAML mapping key")
      }
    }
    val fields = Consequence.zipN(members.map { member =>
      val key: Consequence[String] = member.getKeyNode match {
        case scalar: ScalarNode if scalar.getTag == Tag.STR => Consequence.success(scalar.getValue)
        case _ => _invalid("YAML mapping key must be a string without merge semantics")
      }
      if (member.getValueNode.getTag == Tag.NULL) {
        key.map(_ => Option.empty[(String, Any)])
      } else {
        key.zip(_decode_yaml(member.getValueNode, ancestors)).map(Some.apply)
      }
    })
    validkeys.zip(fields).map { case (_, values) => Record.createFull(values.flatten) }
  }

  private def _yaml_boolean(text: String): Consequence[Boolean] = {
    if (Vector("true", "yes", "on").exists(_.equalsIgnoreCase(text))) {
      Consequence.success(true)
    } else if (Vector("false", "no", "off").exists(_.equalsIgnoreCase(text))) {
      Consequence.success(false)
    } else {
      _invalid("Unsupported YAML boolean")
    }
  }

  private def _yaml_number(node: ScalarNode): Consequence[BigDecimal] = {
    try {
      val text = node.getValue.replace("_", "")
      val negative = text.startsWith("-")
      val magnitude = if (negative || text.startsWith("+")) { text.substring(1) } else { text }
      val unsigned = {
        if (magnitude.contains(":")) {
          magnitude.split(":", -1).foldLeft(java.math.BigDecimal.ZERO) { (total, component) =>
            total.multiply(java.math.BigDecimal.valueOf(60))
              .add(new java.math.BigDecimal(component, MathContext.UNLIMITED))
          }
        } else if (node.getTag == Tag.INT) {
          val (radix, digits) = {
            if (magnitude.startsWith("0b")) {
              (2, magnitude.substring(2))
            } else if (magnitude.startsWith("0x")) {
              (16, magnitude.substring(2))
            } else if (magnitude.startsWith("0") && magnitude.length > 1) {
              (8, magnitude.substring(1))
            } else {
              (10, magnitude)
            }
          }
          new java.math.BigDecimal(new java.math.BigInteger(digits, radix), MathContext.UNLIMITED)
        } else {
          new java.math.BigDecimal(magnitude, MathContext.UNLIMITED)
        }
      }
      val decimal = if (negative) { unsigned.negate() } else { unsigned }
      Consequence.success(new BigDecimal(decimal, MathContext.UNLIMITED))
    } catch {
      case NonFatal(_) => _invalid("YAML number has no exact BigDecimal representation")
    }
  }

  private def _invalid[A](description: String): Consequence[A] = {
    Consequence.valueInvalid(description, XSemiStructuredData)
  }
}
