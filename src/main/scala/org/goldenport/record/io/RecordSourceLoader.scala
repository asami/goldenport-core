package org.goldenport.record.io

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import com.typesafe.config.{Config, ConfigFactory, ConfigObject, ConfigValue}
import org.goldenport.Consequence
import org.goldenport.record.{Field, Record, RecordDecoder as TypedRecordDecoder, RecordFormat}

/*
 * @since   Apr.  8, 2026
 * @version Apr.  8, 2026
 * @author  ASAMI, Tomoharu
 */
object RecordSourceLoader:
  private val _decoder = new RecordDecoder()

  def load(path: Path): Consequence[Record] =
    formatFrom(path)
      .map(load(path, _))
      .getOrElse(Consequence.failure(s"unsupported record format: ${path}"))

  def load(path: Path, format: RecordFormat): Consequence[Record] =
    _read(path).flatMap(load(_, format))

  def load(content: String, format: RecordFormat): Consequence[Record] =
    format match
      case RecordFormat.Json => _decoder.json(content)
      case RecordFormat.Yaml => _decoder.yaml(content)
      case RecordFormat.Xml => _decoder.xml(content)
      case RecordFormat.Hocon => _decode_hocon(content)

  def loadRecords(path: Path): Consequence[Vector[Record]] =
    formatFrom(path)
      .map(loadRecords(path, _))
      .getOrElse(Consequence.failure(s"unsupported record format: ${path}"))

  def loadRecords(path: Path, format: RecordFormat): Consequence[Vector[Record]] =
    _read(path).flatMap(loadRecords(_, format))

  def loadRecords(content: String, format: RecordFormat): Consequence[Vector[Record]] =
    format match
      case RecordFormat.Json => _decoder.jsonAutoRecords(content)
      case RecordFormat.Yaml => _decoder.yamlAutoRecords(content)
      case RecordFormat.Xml => _decoder.xmlAutoRecords(content)
      case RecordFormat.Hocon => _decode_hocon_records(content)

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
    Consequence {
      _config_to_record(ConfigFactory.parseString(content).resolve())
    }

  private def _decode_hocon_records(content: String): Consequence[Vector[Record]] =
    _decode_hocon(content).map(Vector(_))

  private def _config_to_record(config: Config): Record =
    _record_from_pairs(
      config.entrySet().asScala.toVector.map { entry =>
        entry.getKey -> _config_value(entry.getValue)
      }
    )

  private def _config_value(value: ConfigValue): Any =
    value.unwrapped() match
      case null => null
      case _: java.lang.Boolean => value.unwrapped()
      case _: java.lang.Number => BigDecimal(value.unwrapped().toString)
      case s: String => s
      case obj: java.util.Map[?, ?] =>
        val cobj = value.asInstanceOf[ConfigObject]
        _config_object(cobj)
      case xs: java.util.Collection[?] =>
        xs.asScala.toVector.map {
          case cv: ConfigValue => _config_value(cv)
          case other => other
        }
      case other => other.toString

  private def _config_object(obj: ConfigObject): Map[String, Any] =
    obj.entrySet().asScala.toVector.map { entry =>
      entry.getKey -> _config_value(entry.getValue)
    }.toMap

  private def _record_from_pairs(
    pairs: Seq[(String, Any)]
  ): Record =
    def merge(lhs: Map[String, Any], rhs: Map[String, Any]): Map[String, Any] =
      rhs.foldLeft(lhs) { case (z, (key, value)) =>
        (z.get(key), value) match
          case (Some(lm: Map[?, ?]), rm: Map[?, ?]) =>
            z.updated(
              key,
              merge(
                lm.asInstanceOf[Map[String, Any]],
                rm.asInstanceOf[Map[String, Any]]
              )
            )
          case _ =>
            z.updated(key, value)
      }

    def nest(path: String, value: Any): Map[String, Any] =
      path.split("\\.").toList.filter(_.nonEmpty) match
        case Nil => Map.empty
        case head :: Nil => Map(head -> value)
        case head :: tail => Map(head -> nest(tail.mkString("."), value))

    def to_record_value(value: Any): Any = value match
      case m: Map[?, ?] =>
        Record.create(m.iterator.map { case (k, v) => k.toString -> to_record_value(v) }.toSeq)
      case other => other

    val tree = pairs.foldLeft(Map.empty[String, Any]) { case (z, (key, value)) =>
      merge(z, nest(key, value))
    }
    Record.create(tree.iterator.map { case (k, v) => k -> to_record_value(v) }.toSeq)
