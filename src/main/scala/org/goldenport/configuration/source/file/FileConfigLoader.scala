package org.goldenport.configuration.source.file

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import scala.util.Try

import com.typesafe.config.ConfigFactory
import io.circe.Json
import io.circe.parser.parse
import org.yaml.snakeyaml.Yaml

import org.goldenport.Conclusion
import org.goldenport.Consequence
import org.goldenport.configuration.Configuration
import org.goldenport.configuration.ConfigurationValue

trait FileConfigLoader {
  def load(
    path: Path
  ): Consequence[Configuration]
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
}

object ConfigTextDecoder {
  private val _yaml_parser = new Yaml()

  def decode(path: Path, content: String): Consequence[Configuration] =
    _decode(path.getFileName.toString, content)

  def decode(fileName: String, content: String): Consequence[Configuration] =
    _decode(fileName, content)

  private def _decode(fileName: String, content: String): Consequence[Configuration] =
    Try {
      val values =
        _format(fileName) match {
          case Format.Hocon => _decode_hocon_(content)
          case Format.Json => _decode_json_(content)
          case Format.Yaml => _decode_yaml_(content)
        }
      Configuration(values)
    } match {
      case scala.util.Success(cfg) => Consequence.Success(cfg)
      case scala.util.Failure(exception) =>
        Consequence.Failure(Conclusion.from(exception))
    }

  private def _format(fileName: String): Format = {
    val ext = _extension(fileName)
    ext match {
      case "conf" | "props" | "properties" => Format.Hocon
      case "json" => Format.Json
      case "yaml" => Format.Yaml
      case _ => Format.Hocon
    }
  }

  private def _extension(fileName: String): String = {
    val i = fileName.lastIndexOf('.')
    if (i >= 0 && i < fileName.length - 1)
      fileName.substring(i + 1).toLowerCase
    else
      ""
  }

  private def _decode_hocon_(content: String): Map[String, ConfigurationValue] = {
    val root = ConfigFactory.parseString(content).root()
    _as_map_(root.unwrapped())
  }

  private def _decode_json_(content: String): Map[String, ConfigurationValue] =
    parse(content) match {
      case Left(error) => throw error
      case Right(json) =>
        json.asObject.map(_.toMap.map { case (k, v) => k -> _from_json_(v) }).getOrElse(Map.empty)
    }

  private def _decode_yaml_(content: String): Map[String, ConfigurationValue] = {
    val root: Any = _yaml_parser.load(content)
    _as_map_(root)
  }

  private def _as_map_(value: Any): Map[String, ConfigurationValue] =
    value match {
      case null => Map.empty
      case m: java.util.Map[_, _] =>
        _java_map_entries(m).collect {
          case (k, v) if k != null => k.toString -> _from_any_(v)
        }.toMap
      case m: scala.collection.Map[?, ?] =>
        m.iterator.collect {
          case (k, v) if k != null => k.toString -> _from_any_(v)
        }.toMap
      case _ =>
        Map.empty
    }

  private def _from_any_(value: Any): ConfigurationValue =
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
      case m: java.util.Map[_, _] =>
        ConfigurationValue.ObjectValue(
          _java_map_entries(m).collect {
            case (k, x) if k != null => k.toString -> _from_any_(x)
          }.toMap
        )
      case m: scala.collection.Map[?, ?] =>
        ConfigurationValue.ObjectValue(
          m.iterator.collect {
            case (k, x) if k != null => k.toString -> _from_any_(x)
          }.toMap
        )
      case xs: java.util.List[?] =>
        ConfigurationValue.ListValue(xs.asScala.toList.map(_from_any_))
      case xs: Seq[?] =>
        ConfigurationValue.ListValue(xs.toList.map(_from_any_))
      case other =>
        ConfigurationValue.StringValue(other.toString)
    }

  private def _from_json_(json: Json): ConfigurationValue =
    json.fold(
      ConfigurationValue.NullValue,
      bool => ConfigurationValue.BooleanValue(bool),
      num =>
        num.toBigDecimal
          .map(ConfigurationValue.NumberValue(_))
          .getOrElse(ConfigurationValue.StringValue(num.toString)),
      str => ConfigurationValue.StringValue(str),
      arr => ConfigurationValue.ListValue(arr.toList.map(_from_json_)),
      obj => ConfigurationValue.ObjectValue(obj.toMap.map { case (k, v) => k -> _from_json_(v) })
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
    case Json
    case Yaml
  }
}
