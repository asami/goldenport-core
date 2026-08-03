package org.goldenport.configuration.source.file

import java.io.StringReader
import java.nio.file.{Files, Path}
import java.util.Base64
import javax.xml.XMLConstants
import javax.xml.parsers.{DocumentBuilderFactory, ParserConfigurationException}
import org.xml.sax.{EntityResolver, InputSource}
import scala.jdk.CollectionConverters.*
import scala.util.Try

import com.typesafe.config.ConfigFactory
import io.circe.Json
import io.circe.parser.parse
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.nodes.{MappingNode, Node, ScalarNode, SequenceNode}

import org.goldenport.Conclusion
import org.goldenport.Consequence
import org.goldenport.configuration.Configuration
import org.goldenport.configuration.ConfigurationValue
import org.goldenport.configuration.ConfigurationDocument
import org.goldenport.configuration.ConfigurationSourceLoad

/*
 * @since   Mar. 13, 2026
 *  version Jul.  1, 2026
 * @version Aug.  3, 2026
 * @author  ASAMI, Tomoharu
 */
trait FileConfigLoader {
  def load(
    path: Path
  ): Consequence[Configuration]

  def loadSnapshot(
    path: Path
  ): Consequence[ConfigurationSourceLoad] =
    load(path).map(ConfigurationSourceLoad(_))
}

final class SimpleFileConfigLoader
  extends FileConfigLoader {

  override def load(
    path: Path
  ): Consequence[Configuration] = {
    if (!Files.exists(path))
      Consequence.Success(Configuration.empty)
    else
      ConfigTextDecoder.decode(path, Files.readString(path))
  }

  override def loadSnapshot(
    path: Path
  ): Consequence[ConfigurationSourceLoad] =
    if (!Files.exists(path))
      Consequence.Success(ConfigurationSourceLoad(Configuration.empty))
    else
      ConfigTextDecoder.decodeSnapshot(path, Files.readString(path))
}

object ConfigTextDecoder {
  private val _yaml_parser = new Yaml()

  def decode(path: Path, content: String): Consequence[Configuration] =
    _decode(path.getFileName.toString, content)

  def decode(filename: String, content: String): Consequence[Configuration] =
    _decode(filename, content)

  def decodeSnapshot(path: Path, content: String): Consequence[ConfigurationSourceLoad] =
    decodeSnapshot(path.getFileName.toString, content)

  def decodeSnapshot(filename: String, content: String): Consequence[ConfigurationSourceLoad] =
    _format(filename) match {
      case Format.Yaml =>
        for {
          configuration <- _decode(filename, content)
          document <- _decode_yaml_document(content)
        } yield ConfigurationSourceLoad(configuration, Some(document))
      case _ =>
        _decode(filename, content).map(ConfigurationSourceLoad(_))
    }

  private def _decode(filename: String, content: String): Consequence[Configuration] =
    Try {
      val values =
        _format(filename) match {
          case Format.Hocon => _decode_hocon(content)
          case Format.Properties => _decode_properties(content)
          case Format.Json => _decode_json(content)
          case Format.Yaml => _decode_yaml(content)
          case Format.Xml => _decode_xml(content)
        }
      Configuration(values)
    } match {
      case scala.util.Success(cfg) => Consequence.Success(cfg)
      case scala.util.Failure(exception) =>
        Consequence.Failure(Conclusion.from(exception))
    }

  private def _format(filename: String): Format = {
    val ext = _extension(filename)
    ext match {
      case "conf" => Format.Hocon
      case "props" | "properties" => Format.Properties
      case "json" => Format.Json
      case "yaml" | "yml" => Format.Yaml
      case "xml" => Format.Xml
      case _ => Format.Hocon
    }
  }

  private def _extension(filename: String): String = {
    val i = filename.lastIndexOf('.')
    if (i >= 0 && i < filename.length - 1)
      filename.substring(i + 1).toLowerCase
    else
      ""
  }

  private val _xml_factory = _new_xml_factory()

  private def _decode_hocon(content: String): Map[String, ConfigurationValue] = {
    val root = ConfigFactory.parseString(content).root()
    _as_map(root.unwrapped())
  }

  private def _decode_properties(content: String): Map[String, ConfigurationValue] = {
    val props = java.util.Properties()
    props.load(StringReader(content))
    props.stringPropertyNames().asScala.map { key =>
      key -> ConfigurationValue.StringValue(props.getProperty(key))
    }.toMap
  }

  private def _decode_json(content: String): Map[String, ConfigurationValue] =
    parse(content) match {
      case Left(error) => throw error
      case Right(json) =>
        json.asObject.map(_.toMap.map { case (k, v) => k -> _from_json(v) })
          .getOrElse(throw new IllegalArgumentException("configuration root must be an object"))
    }

  private def _decode_yaml(content: String): Map[String, ConfigurationValue] = {
    val root: Any = _yaml_parser.load(content)
    _as_map(root)
  }

  /** Parses YAML's node graph so repeated mapping members are not collapsed
   *  before a downstream binding layer can validate them. The compatibility
   *  configuration continues to use SnakeYAML's established last-wins map.
   */
  private def _decode_yaml_document(content: String): Consequence[ConfigurationDocument.Object] =
    Try {
      Option(new Yaml().compose(new StringReader(content))) match {
        case None => ConfigurationDocument.Object(Vector.empty)
        case Some(root: MappingNode) => _yaml_object(root)
        case Some(_) =>
          throw new IllegalArgumentException("configuration root must be an object")
      }
    } match {
      case scala.util.Success(document) => Consequence.Success(document)
      case scala.util.Failure(exception) => Consequence.Failure(Conclusion.from(exception))
    }

  private def _yaml_object(node: MappingNode): ConfigurationDocument.Object =
    ConfigurationDocument.Object(
      node.getValue.asScala.toVector.map { tuple =>
        tuple.getKeyNode match {
          case key: ScalarNode =>
            ConfigurationDocument.Field(key.getValue, _yaml_document(tuple.getValueNode))
          case _ =>
            throw new IllegalArgumentException("configuration object key must be a scalar")
        }
      }
    )

  private def _yaml_document(node: Node): ConfigurationDocument =
    node match {
      case mapping: MappingNode => _yaml_object(mapping)
      case sequence: SequenceNode =>
        ConfigurationDocument.Sequence(sequence.getValue.asScala.toVector.map(_yaml_document))
      case scalar: ScalarNode => ConfigurationDocument.Scalar(_from_yaml_scalar(scalar))
      case _ =>
        throw new IllegalArgumentException("unsupported YAML configuration node")
    }

  private def _from_yaml_scalar(node: ScalarNode): ConfigurationValue = {
    val value = node.getValue
    val tag = node.getTag.getValue
    if (tag == "tag:yaml.org,2002:str")
      ConfigurationValue.StringValue(value)
    else
      // Reuse the established SnakeYAML scalar conversion with the composed
      // tag intact. Raw-document capture therefore cannot reinterpret a
      // compatibility-accepted null, bool, number, timestamp, or other native
      // scalar as plain text.
      _from_any(new Yaml().load(_yaml_tagged_scalar(tag, value)))
  }

  private def _yaml_tagged_scalar(tag: String, value: String): String =
    s"!<$tag> ${_yaml_double_quoted(value)}"

  private def _yaml_double_quoted(value: String): String =
    value.foldLeft(new StringBuilder("\"")) { (z, c) =>
      c match {
        case '\\' => z.append("\\\\")
        case '"' => z.append("\\\"")
        case '\n' => z.append("\\n")
        case '\r' => z.append("\\r")
        case '\t' => z.append("\\t")
        case '\u0000' => z.append("\\0")
        case '\b' => z.append("\\b")
        case '\f' => z.append("\\f")
        case '\u0007' => z.append("\\a")
        case '\u000b' => z.append("\\v")
        case '\u001b' => z.append("\\e")
        case x if Character.isISOControl(x) => z.append(f"\\u${x.toInt}%04X")
        case x => z.append(x)
      }
    }.append('"').toString

  private def _decode_xml(content: String): Map[String, ConfigurationValue] = {
    val builder = _xml_factory.newDocumentBuilder()
    builder.setEntityResolver(new EntityResolver {
      override def resolveEntity(publicid: String, systemid: String): InputSource =
        new InputSource(new StringReader(""))
    })
    val doc = builder.parse(new InputSource(new StringReader(content)))
    _xml_children(doc.getDocumentElement)
  }

  private def _xml_children(node: org.w3c.dom.Node): Map[String, ConfigurationValue] =
    _xml_child_elements(node).groupBy(_xml_name).map {
      case (label, children) => label -> _xml_value(children)
    }

  private def _xml_value(children: Vector[org.w3c.dom.Element]): ConfigurationValue =
    children.toList match {
      case one :: Nil => _xml_value(one)
      case many => ConfigurationValue.ListValue(many.map(_xml_value))
    }

  private def _xml_value(node: org.w3c.dom.Element): ConfigurationValue = {
    val elements = _xml_child_elements(node)
    if (elements.nonEmpty)
      ConfigurationValue.ObjectValue(_xml_children(node))
    else
      ConfigurationValue.StringValue(node.getTextContent.trim)
  }

  private def _xml_child_elements(node: org.w3c.dom.Node): Vector[org.w3c.dom.Element] =
    _xml_node_list_to_vector(node.getChildNodes).collect {
      case e: org.w3c.dom.Element => e
    }

  private def _xml_node_list_to_vector(nodes: org.w3c.dom.NodeList): Vector[org.w3c.dom.Node] =
    (0 until nodes.getLength).toVector.map(nodes.item)

  private def _xml_name(node: org.w3c.dom.Node): String =
    Option(node.getLocalName).getOrElse(node.getNodeName)

  private def _as_map(value: Any): Map[String, ConfigurationValue] =
    value match {
      case null => Map.empty
      case m: java.util.Map[_, _] =>
        _java_map_entries(m).collect {
          case (k, v) if k != null => k.toString -> _from_any(v)
        }.toMap
      case m: scala.collection.Map[?, ?] =>
        m.iterator.collect {
          case (k, v) if k != null => k.toString -> _from_any(v)
        }.toMap
      case _ =>
        throw new IllegalArgumentException("configuration root must be an object")
    }

  private def _new_xml_factory(): DocumentBuilderFactory = {
    val factory = DocumentBuilderFactory.newInstance()
    factory.setNamespaceAware(true)
    factory.setXIncludeAware(false)
    factory.setExpandEntityReferences(false)
    _set_xml_feature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true)
    _set_xml_feature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true)
    _set_xml_feature(factory, "http://xml.org/sax/features/external-general-entities", false)
    _set_xml_feature(factory, "http://xml.org/sax/features/external-parameter-entities", false)
    _set_xml_feature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    factory
  }

  private def _set_xml_feature(
    factory: DocumentBuilderFactory,
    feature: String,
    enabled: Boolean
  ): Unit =
    try {
      factory.setFeature(feature, enabled)
    } catch {
      case e: ParserConfigurationException =>
        throw new IllegalStateException(s"XML parser feature is not supported: $feature", e)
    }

  private def _from_any(value: Any): ConfigurationValue =
    value match {
      case null => ConfigurationValue.NullValue
      case v: String => ConfigurationValue.StringValue(v)
      case v: java.lang.Integer => ConfigurationValue.NumberValue(BigDecimal(v.intValue))
      case v: java.lang.Long => ConfigurationValue.NumberValue(BigDecimal(v.longValue))
      case v: java.lang.Double => ConfigurationValue.NumberValue(BigDecimal(v.doubleValue))
      case v: java.lang.Float => ConfigurationValue.NumberValue(BigDecimal(v.doubleValue))
      case v: java.lang.Boolean => ConfigurationValue.BooleanValue(v.booleanValue)
      case v: java.math.BigDecimal => ConfigurationValue.NumberValue(BigDecimal(v))
      case v: java.math.BigInteger => ConfigurationValue.NumberValue(BigDecimal(v))
      case v: Array[Byte] => ConfigurationValue.StringValue(Base64.getEncoder.encodeToString(v))
      case m: java.util.Map[_, _] =>
        ConfigurationValue.ObjectValue(
          _java_map_entries(m).collect {
            case (k, x) if k != null => k.toString -> _from_any(x)
          }.toMap
        )
      case m: scala.collection.Map[?, ?] =>
        ConfigurationValue.ObjectValue(
          m.iterator.collect {
            case (k, x) if k != null => k.toString -> _from_any(x)
          }.toMap
        )
      case xs: java.util.List[?] =>
        ConfigurationValue.ListValue(xs.asScala.toList.map(_from_any))
      case xs: Seq[?] =>
        ConfigurationValue.ListValue(xs.toList.map(_from_any))
      case other =>
        ConfigurationValue.StringValue(other.toString)
    }

  private def _from_json(json: Json): ConfigurationValue =
    json.fold(
      ConfigurationValue.NullValue,
      bool => ConfigurationValue.BooleanValue(bool),
      num =>
        num.toBigDecimal
          .map(ConfigurationValue.NumberValue(_))
          .getOrElse(ConfigurationValue.StringValue(num.toString)),
      str => ConfigurationValue.StringValue(str),
      arr => ConfigurationValue.ListValue(arr.toList.map(_from_json)),
      obj => ConfigurationValue.ObjectValue(obj.toMap.map { case (k, v) => k -> _from_json(v) })
    )

  private def _java_map_entries(
    value: java.util.Map[_, _]
  ): Iterator[(Any, Any)] =
    value
      .asInstanceOf[java.util.Map[Any, Any]]
      .entrySet()
      .asScala
      .iterator
      .map(entry => entry.getKey -> entry.getValue)

  private enum Format {
    case Hocon
    case Properties
    case Json
    case Yaml
    case Xml
  }
}
