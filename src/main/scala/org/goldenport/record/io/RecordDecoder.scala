package org.goldenport.record.io

import java.io.InputStream
import java.io.StringReader
import java.nio.charset.StandardCharsets

import javax.xml.parsers.DocumentBuilderFactory

import scala.jdk.CollectionConverters._

import io.circe.Json
import io.circe.parser.parse

import org.goldenport.Consequence
import org.goldenport.bag.Bag
import org.goldenport.record.Record
import org.goldenport.record.Field

import org.xml.sax.InputSource
import org.yaml.snakeyaml.Yaml

/*
 * @since   Feb.  7, 2026
 * @version Mar. 27, 2026
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

  def jsonRecords(in: String): Consequence[Vector[Record]] =
    _decode_json_records(in)

  def jsonRecords(in: Bag): Consequence[Vector[Record]] =
    in.asString().flatMap(_decode_json_records)

  def jsonRecords(in: InputStream): Consequence[Vector[Record]] =
    _read_all(in).flatMap(_decode_json_records)

  def jsonAutoRecords(in: String): Consequence[Vector[Record]] =
    _decode_json_auto_records(in)

  def jsonAutoRecords(in: Bag): Consequence[Vector[Record]] =
    in.asString().flatMap(_decode_json_auto_records)

  def jsonAutoRecords(in: InputStream): Consequence[Vector[Record]] =
    _read_all(in).flatMap(_decode_json_auto_records)

  def yaml(in: String): Consequence[Record] =
    _decode_yaml(in)

  def yaml(in: Bag): Consequence[Record] =
    in.asString().flatMap(_decode_yaml)

  def yaml(in: InputStream): Consequence[Record] =
    _read_all(in).flatMap(_decode_yaml)

  def xml(in: String): Consequence[Record] =
    _decode_xml(in)

  def xml(in: Bag): Consequence[Record] =
    in.asString().flatMap(_decode_xml)

  def xml(in: InputStream): Consequence[Record] =
    _read_all(in).flatMap(_decode_xml)

  def csvRecords(in: String): Consequence[Vector[Record]] =
    _decode_csv_records(in)

  def csvRecords(in: Bag): Consequence[Vector[Record]] =
    in.asString().flatMap(_decode_csv_records)

  def csvRecords(in: InputStream): Consequence[Vector[Record]] =
    _read_all(in).flatMap(_decode_csv_records)

  def tslRecords(in: String): Consequence[Vector[Record]] =
    _decode_tsl_records(in)

  def tslRecords(in: Bag): Consequence[Vector[Record]] =
    in.asString().flatMap(_decode_tsl_records)

  def tslRecords(in: InputStream): Consequence[Vector[Record]] =
    _read_all(in).flatMap(_decode_tsl_records)

  def yamlRecords(in: String): Consequence[Vector[Record]] =
    _decode_yaml_records(in)

  def yamlRecords(in: Bag): Consequence[Vector[Record]] =
    in.asString().flatMap(_decode_yaml_records)

  def yamlRecords(in: InputStream): Consequence[Vector[Record]] =
    _read_all(in).flatMap(_decode_yaml_records)

  def yamlAutoRecords(in: String): Consequence[Vector[Record]] =
    _decode_yaml_auto_records(in)

  def yamlAutoRecords(in: Bag): Consequence[Vector[Record]] =
    in.asString().flatMap(_decode_yaml_auto_records)

  def yamlAutoRecords(in: InputStream): Consequence[Vector[Record]] =
    _read_all(in).flatMap(_decode_yaml_auto_records)

  def xmlRecords(in: String): Consequence[Vector[Record]] =
    _decode_xml_records(in)

  def xmlRecords(in: Bag): Consequence[Vector[Record]] =
    in.asString().flatMap(_decode_xml_records)

  def xmlRecords(in: InputStream): Consequence[Vector[Record]] =
    _read_all(in).flatMap(_decode_xml_records)

  def xmlAutoRecords(in: String): Consequence[Vector[Record]] =
    _decode_xml_auto_records(in)

  def xmlAutoRecords(in: Bag): Consequence[Vector[Record]] =
    in.asString().flatMap(_decode_xml_auto_records)

  def xmlAutoRecords(in: InputStream): Consequence[Vector[Record]] =
    _read_all(in).flatMap(_decode_xml_auto_records)

  // --- private helpers ---

  private val _yaml_parser = new Yaml()
  private val _xml_factory = {
    val f = DocumentBuilderFactory.newInstance()
    f.setNamespaceAware(true)
    f.setExpandEntityReferences(false)
    f
  }

  private def _decode_json(text: String): Consequence[Record] =
    Consequence {
      parse(text) match {
        case Right(json) => _to_record(json)
        case Left(e)     => throw e
      }
    }

  private def _decode_json_records(text: String): Consequence[Vector[Record]] =
    Consequence {
      parse(text) match {
        case Right(json) =>
          json.asArray match {
            case Some(values) =>
              values.toVector.map { value =>
                value.asObject match {
                  case Some(_) => _to_record(value)
                  case None => throw new IllegalArgumentException("JSON records require an array of objects")
                }
              }
            case None =>
              throw new IllegalArgumentException("JSON records require a top-level array")
          }
        case Left(e) => throw e
      }
    }

  private def _decode_json_auto_records(text: String): Consequence[Vector[Record]] =
    Consequence {
      parse(text) match {
        case Right(json) =>
          json.asObject match {
            case Some(_) => Vector(_to_record(json))
            case None =>
              json.asArray match {
                case Some(values) =>
                  values.toVector.map { value =>
                    value.asObject match {
                      case Some(_) => _to_record(value)
                      case None => throw new IllegalArgumentException("JSON auto records require an object or an array of objects")
                    }
                  }
                case None => throw new IllegalArgumentException("JSON auto records require an object or an array of objects")
              }
          }
        case Left(e) => throw e
      }
    }

  private def _decode_yaml(text: String): Consequence[Record] =
    Consequence {
      val yamlValue: AnyRef = _yaml_parser.load(text)
      val jsonString = _to_json(yamlValue).noSpaces
      parse(jsonString) match {
        case Right(json) => _to_record(json)
        case Left(e)     => throw e
      }
    }

  private def _decode_yaml_records(text: String): Consequence[Vector[Record]] =
    Consequence {
      val yamlValue: AnyRef = _yaml_parser.load(text)
      if (yamlValue == null)
        Vector.empty
      else if (yamlValue.isInstanceOf[java.util.Collection[?]]) {
        val xs = yamlValue.asInstanceOf[java.util.Collection[AnyRef]]
        xs.asScala.toVector.map(_yaml_record_from_list_item)
      } else {
        throw new IllegalArgumentException("YAML plural decode requires a top-level list")
      }
    }

  private def _decode_yaml_auto_records(text: String): Consequence[Vector[Record]] =
    Consequence {
      val yamlValue: AnyRef = _yaml_parser.load(text)
      if (yamlValue == null) {
        throw new IllegalArgumentException("YAML auto records require a mapping or a list of mappings")
      } else if (yamlValue.isInstanceOf[java.util.Map[?, ?]]) {
        Vector(_yaml_record_from_list_item(yamlValue))
      } else if (yamlValue.isInstanceOf[java.util.Collection[?]]) {
        val xs = yamlValue.asInstanceOf[java.util.Collection[AnyRef]]
        xs.asScala.toVector.map(_yaml_record_from_list_item)
      } else {
        throw new IllegalArgumentException("YAML auto records require a mapping or a list of mappings")
      }
    }

  private def _decode_xml(text: String): Consequence[Record] =
    Consequence {
      val doc = _xml_factory.newDocumentBuilder().parse(new InputSource(new StringReader(text)))
      _xml_element_to_record(doc.getDocumentElement)
    }

  private def _decode_xml_records(text: String): Consequence[Vector[Record]] =
    Consequence {
      val doc = _xml_factory.newDocumentBuilder().parse(new InputSource(new StringReader(text)))
      _xml_container_to_records(doc.getDocumentElement)
    }

  private def _decode_xml_auto_records(text: String): Consequence[Vector[Record]] =
    Consequence {
      val doc = _xml_factory.newDocumentBuilder().parse(new InputSource(new StringReader(text)))
      _xml_container_to_auto_records(doc.getDocumentElement)
    }

  private def _yaml_record_from_list_item(value: Any): Record =
    if (value.isInstanceOf[java.util.Map[?, ?]]) {
      val m = value.asInstanceOf[java.util.Map[String, AnyRef]]
      Record.create(
        m.asScala.toVector.map { case (k, v) => k -> v }
      )
    } else {
      throw new IllegalArgumentException("YAML list items must be mappings")
    }

  private def _decode_csv_records(text: String): Consequence[Vector[Record]] =
    Consequence {
      val lines = _split_non_empty_lines(text)
      if (lines.isEmpty)
        throw new IllegalArgumentException("CSV input is empty")
      val header = _parse_csv_line(lines.head)
      if (header.isEmpty)
        throw new IllegalArgumentException("CSV header is missing")
      val rows = lines.drop(1)
      rows.map { line =>
        val values = _parse_csv_line(line)
        Record.create(header.zipAll(values, "", "").collect {
          case (key, value) if key.nonEmpty => key -> value
        })
      }.toVector
    }

  private def _decode_tsl_records(text: String): Consequence[Vector[Record]] =
    Consequence {
      val blocks = _split_tsl_blocks(text)
      if (blocks.isEmpty)
        throw new IllegalArgumentException("TSL input is empty")
      blocks.map(_parse_tsl_block).toVector
    }

  private def _to_json(value: Any): Json =
    if (value == null) {
      Json.Null
    } else if (value.isInstanceOf[java.lang.Boolean]) {
      Json.fromBoolean(value.asInstanceOf[java.lang.Boolean])
    } else if (value.isInstanceOf[java.lang.Number]) {
      Json.fromBigDecimal(BigDecimal(value.asInstanceOf[java.lang.Number].toString))
    } else if (value.isInstanceOf[String]) {
      Json.fromString(value.asInstanceOf[String])
    } else if (value.isInstanceOf[java.util.Map[?, ?]]) {
      val m = value.asInstanceOf[java.util.Map[String, AnyRef]]
      Json.fromJsonObject(
        io.circe.JsonObject.fromIterable(
          m.asScala.map { case (k, v) => k -> _to_json(v) }
        )
      )
    } else if (value.isInstanceOf[java.util.Collection[?]]) {
      val c = value.asInstanceOf[java.util.Collection[AnyRef]]
      Json.fromValues(c.asScala.iterator.map(_to_json).toVector)
    } else if (value.isInstanceOf[Array[?]]) {
      Json.fromValues(value.asInstanceOf[Array[AnyRef]].toVector.map(_to_json))
    } else {
      Json.fromString(value.toString)
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

  private def _xml_container_to_records(element: org.w3c.dom.Element): Vector[Record] = {
    val children = _xml_child_elements(element)
    if (children.isEmpty)
      Vector.empty
    else {
      val names = children.map(_xml_local_name).distinct
      if (names.size != 1)
        throw new IllegalArgumentException("XML records require homogeneous direct child elements")
      children.toVector.map { child =>
        _xml_element_to_record(child)
      }
    }
  }

  private def _xml_container_to_auto_records(element: org.w3c.dom.Element): Vector[Record] = {
    val children = _xml_child_elements(element)
    if (children.isEmpty) {
      Vector(_xml_element_to_record(element))
    } else {
      val names = children.map(_xml_local_name).distinct
      if (_xml_local_name(element) == "list" && names.size == 1) {
        children.toVector.map(_xml_element_to_record)
      } else {
        Vector(_xml_element_to_record(element))
      }
    }
  }

  private def _xml_element_to_record(element: org.w3c.dom.Element): Record = {
    val fields = scala.collection.mutable.ArrayBuffer.empty[(String, Any)]
    _xml_attributes(element).foreach { case (k, v) =>
      fields += s"@$k" -> v
    }
    val childElements = _xml_child_elements(element)
    val text = _xml_text_content(element)
    if (childElements.nonEmpty && text.nonEmpty)
      throw new IllegalArgumentException("XML mixed content is not supported")
    if (childElements.isEmpty) {
      if (text.nonEmpty || fields.nonEmpty) {
        if (text.nonEmpty)
          fields += "#text" -> text
        Record.create(fields.toVector)
      } else {
        Record.empty
      }
    } else {
      val grouped = childElements.groupBy(_xml_local_name)
      grouped.foreach { case (name, elems) =>
        val value =
          if (elems.size == 1) _xml_element_value(elems.head)
          else elems.toVector.map(_xml_element_value)
        fields += name -> value
      }
      if (text.nonEmpty)
        fields += "#text" -> text
      Record.create(fields.toVector)
    }
  }

  private def _xml_element_value(element: org.w3c.dom.Element): Any = {
    val childElements = _xml_child_elements(element)
    val text = _xml_text_content(element)
    val attrs = _xml_attributes(element)
    if (childElements.isEmpty && attrs.isEmpty)
      text
    else
      _xml_element_to_record(element)
  }

  private def _xml_child_elements(element: org.w3c.dom.Element): Vector[org.w3c.dom.Element] =
    _xml_node_list_to_vector(element.getChildNodes).collect {
      case e: org.w3c.dom.Element => e
    }

  private def _xml_text_content(element: org.w3c.dom.Element): String = {
    val text = _xml_node_list_to_vector(element.getChildNodes).collect {
      case t: org.w3c.dom.Text => t.getWholeText
      case c: org.w3c.dom.CDATASection => c.getData
    }.mkString.trim
    text
  }

  private def _xml_attributes(element: org.w3c.dom.Element): Vector[(String, String)] = {
    val attrs = element.getAttributes
    (0 until attrs.getLength).toVector.map { i =>
      val node = attrs.item(i)
      val name = _xml_local_name(node)
      name -> node.getNodeValue
    }
  }

  private def _xml_local_name(node: org.w3c.dom.Node): String =
    Option(node.getLocalName).getOrElse(node.getNodeName)

  private def _xml_node_list_to_vector(nodes: org.w3c.dom.NodeList): Vector[org.w3c.dom.Node] =
    (0 until nodes.getLength).toVector.map(i => nodes.item(i))

  private def _split_non_empty_lines(text: String): Vector[String] =
    text.linesIterator.map(_.trim).filter(_.nonEmpty).toVector

  private def _parse_csv_line(line: String): Vector[String] = {
    val values = scala.collection.mutable.ArrayBuffer.empty[String]
    val current = new StringBuilder
    var inquotes = false
    var i = 0
    while (i < line.length) {
      line.charAt(i) match {
        case '"' =>
          if (inquotes && i + 1 < line.length && line.charAt(i + 1) == '"') {
            current += '"'
            i += 1
          } else {
            inquotes = !inquotes
          }
        case ',' if !inquotes =>
          values += current.toString
          current.clear()
        case c =>
          current += c
      }
      i += 1
    }
    values += current.toString
    values.toVector.map(_.trim)
  }

  private def _split_tsl_blocks(text: String): Vector[String] = {
    val blocks = scala.collection.mutable.ArrayBuffer.empty[String]
    val current = new StringBuilder
    text.linesIterator.foreach { line =>
      if (line.trim.isEmpty) {
        if (current.nonEmpty) {
          blocks += current.toString
          current.clear()
        }
      } else {
        if (current.nonEmpty) current.append('\n')
        current.append(line)
      }
    }
    if (current.nonEmpty) blocks += current.toString
    blocks.toVector
  }

  private def _parse_tsl_block(block: String): Record = {
    val fields = scala.collection.mutable.ArrayBuffer.empty[(String, Any)]
    val seen = scala.collection.mutable.Set.empty[String]
    block.linesIterator.foreach { line =>
      val trimmed = line.trim
      if (trimmed.isEmpty) ()
      else {
        val idx = trimmed.indexOf(':')
        if (idx < 0)
          throw new IllegalArgumentException(s"Malformed TSL line: $line")
        val key = trimmed.substring(0, idx).trim
        val value = trimmed.substring(idx + 1).trim
        if (key.isEmpty)
          throw new IllegalArgumentException(s"Malformed TSL line: $line")
        if (seen.contains(key))
          throw new IllegalArgumentException(s"Duplicate TSL key: $key")
        seen += key
        fields += key -> value
      }
    }
    Record.create(fields.toVector)
  }

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
