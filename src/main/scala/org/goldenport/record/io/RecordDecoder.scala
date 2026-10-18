package org.goldenport.record.io

import java.io.InputStream
import java.io.StringReader
import java.nio.charset.StandardCharsets

import javax.xml.parsers.DocumentBuilderFactory

import org.goldenport.Consequence
import org.goldenport.bag.Bag
import org.goldenport.record.Record
import org.goldenport.schema.{StructuredDataFormat, StructuredDataText}

import org.xml.sax.InputSource

/*
 * @since   Feb.  7, 2026
 *  version Apr. 11, 2026
 *  version Jul. 21, 2026
 * @version Oct. 18, 2026
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

  private val _xml_factory = {
    val f = DocumentBuilderFactory.newInstance()
    f.setNamespaceAware(true)
    f.setExpandEntityReferences(false)
    f
  }

  private def _decode_json(text: String): Consequence[Record] =
    StructuredDataText.fromText(text, StructuredDataFormat.Json)

  private def _decode_json_records(text: String): Consequence[Vector[Record]] =
    StructuredDataText.fromJsonRecords(text, allowSingle = false)

  private def _decode_json_auto_records(text: String): Consequence[Vector[Record]] =
    StructuredDataText.fromJsonRecords(text, allowSingle = true)

  private def _decode_yaml(text: String): Consequence[Record] =
    StructuredDataText.fromText(text, StructuredDataFormat.Yaml)

  private def _decode_yaml_records(text: String): Consequence[Vector[Record]] =
    StructuredDataText.fromYamlRecords(text, allowSingle = false)

  private def _decode_yaml_auto_records(text: String): Consequence[Vector[Record]] =
    StructuredDataText.fromYamlRecords(text, allowSingle = true)

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
      val namesInOrder = childElements.map(_xml_local_name).foldLeft(Vector.empty[String]) { (acc, name) =>
        if (acc.contains(name)) acc else acc :+ name
      }
      namesInOrder.foreach { name =>
        val elems = childElements.filter(e => _xml_local_name(e) == name)
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
      case c: org.w3c.dom.CDATASection => c.getData
      case t: org.w3c.dom.Text => t.getWholeText
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
