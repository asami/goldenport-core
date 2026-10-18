package org.goldenport.record.io

import java.io.StringReader
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import com.typesafe.config.ConfigFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import org.goldenport.Consequence
import org.goldenport.record.{Record, RecordDecoder as TypedRecordDecoder, RecordFormat}
import org.goldenport.schema.StructuredDataHocon

/*
 * @since   Apr.  8, 2026
 *  version May. 24, 2026
 *  version Jul. 21, 2026
 * @version Oct. 18, 2026
 * @author  ASAMI, Tomoharu
 */
object RecordSourceLoader:
  private val _decoder = new RecordDecoder()
  private val _import_decoder = new RecordImportDecoder()
  private val _toml_mapper = new ObjectMapper(new TomlFactory())

  def load(path: Path): Consequence[Record] =
    formatFrom(path)
      .map(load(path, _))
      .getOrElse(Consequence.resourceUnsupported(s"unsupported record format: ${path}"))

  def load(path: Path, format: RecordFormat): Consequence[Record] =
    _read(path).flatMap(load(_, format))

  def load(content: String, format: RecordFormat): Consequence[Record] =
    format match
      case RecordFormat.Json => _decoder.json(content)
      case RecordFormat.Yaml => _decoder.yaml(content)
      case RecordFormat.Xml => _decoder.xml(content)
      case RecordFormat.Hocon => _decode_hocon(content)
      case RecordFormat.Toml => _decode_toml(content)
      case RecordFormat.Properties => _decode_properties(content)
      case RecordFormat.Csv | RecordFormat.Tsv | RecordFormat.Ltsv | RecordFormat.Lines | RecordFormat.Tsl | RecordFormat.Excel =>
        loadRecords(content, format).map(_.headOption.getOrElse(Record.empty))

  def loadRecords(path: Path): Consequence[Vector[Record]] =
    formatFrom(path)
      .map(loadRecords(path, _))
      .getOrElse(Consequence.resourceUnsupported(s"unsupported record format: ${path}"))

  def loadRecords(path: Path, format: RecordFormat): Consequence[Vector[Record]] =
    format match
      case RecordFormat.Excel =>
        _import_decoder.decodePath(path, format, RecordImportDecoder.RecordImportOptions.default).map(_.records)
      case _ =>
        _read(path).flatMap(loadRecords(_, format))

  def loadRecords(content: String, format: RecordFormat): Consequence[Vector[Record]] =
    format match
      case RecordFormat.Json => _decoder.jsonAutoRecords(content)
      case RecordFormat.Yaml => _decoder.yamlAutoRecords(content)
      case RecordFormat.Xml => _decoder.xmlAutoRecords(content)
      case RecordFormat.Hocon => _decode_hocon_records(content)
      case RecordFormat.Toml => _decode_toml(content).map(Vector(_))
      case RecordFormat.Properties => _decode_properties_records(content)
      case RecordFormat.Csv | RecordFormat.Tsv | RecordFormat.Ltsv | RecordFormat.Lines | RecordFormat.Tsl =>
        _import_decoder.decode(content, format, RecordImportDecoder.RecordImportOptions.default).map(_.records)
      case RecordFormat.Excel =>
        Consequence.argumentInvalid("Excel records must be loaded from bytes or a path.")

  def decode[T](path: Path)(using dec: TypedRecordDecoder[T]): Consequence[T] =
    load(path).flatMap(dec.fromRecord)

  def decode[T](content: String, format: RecordFormat)(using dec: TypedRecordDecoder[T]): Consequence[T] =
    load(content, format).flatMap(dec.fromRecord)

  def formatFrom(path: Path): Option[RecordFormat] =
    RecordFormat.fromPath(path)

  private def _read(path: Path): Consequence[String] =
    Consequence {
      Files.readString(path)
    }

  private def _decode_hocon(content: String): Consequence[Record] =
    Consequence { ConfigFactory.parseString(content).resolve() }
      .flatMap(StructuredDataHocon.fromConfig)

  private def _decode_hocon_records(content: String): Consequence[Vector[Record]] =
    _decode_hocon(content).map(Vector(_))

  private def _decode_toml(content: String): Consequence[Record] =
    Consequence {
      _toml_mapper.readValue(content, classOf[java.util.Map[String, Object]])
    }.flatMap(_toml_object)

  private def _toml_object(value: java.util.Map[?, ?]): Consequence[Record] =
    Consequence.zipN(value.asScala.toVector.flatMap { case (key, entry) =>
      Option(entry).map(present => _toml_value(present).map(key.toString -> _))
    }).flatMap(Record.createC)

  private def _toml_value(value: Any): Consequence[Any] =
    value match
      case null => Consequence.valueInvalid("Null TOML sequence element")
      case map: java.util.Map[?, ?] =>
        _toml_object(map)
      case values: java.util.Collection[?] =>
        Consequence.zipN(values.asScala.toVector.map(_toml_value)).map(_.toVector)
      case number: java.lang.Number => Consequence { BigDecimal(number.toString) }
      case other => Consequence.success(other)

  private def _decode_properties(content: String): Consequence[Record] =
    Consequence {
      val props = java.util.Properties()
      props.load(StringReader(content))
      Record.create(
        props.stringPropertyNames().asScala.toVector.sorted.map { key =>
          key -> props.getProperty(key)
        }
      )
    }

  private def _decode_properties_records(content: String): Consequence[Vector[Record]] =
    _decode_properties(content).map(Vector(_))
