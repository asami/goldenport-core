package org.goldenport.record.io

import java.io.ByteArrayInputStream
import java.nio.file.{Files, Path}
import com.typesafe.config.{Config as HoconConfig, ConfigFactory, ConfigObject, ConfigValue}
import scala.jdk.CollectionConverters.*
import scala.util.Using
import org.apache.poi.ss.usermodel.{DataFormatter, WorkbookFactory}

import org.goldenport.Consequence
import org.goldenport.record.{Record, RecordFormat}
import org.goldenport.schema.*

/*
 * @since   May. 24, 2026
 * @version May. 24, 2026
 * @author  ASAMI, Tomoharu
 */
final class RecordImportDecoder(
  config: RecordImportDecoder.Config = RecordImportDecoder.Config.default
) {
  import RecordImportDecoder.*

  private val _decoder = new RecordDecoder()

  def decode(
    text: String,
    format: RecordImportFormat,
    options: RecordImportOptions = RecordImportOptions.default
  ): Consequence[RecordImportResult] =
    format match {
      case RecordImportFormat.Auto => decodeAuto(text, options)
      case RecordImportFormat.Csv => _decode_delimited(text, RecordImportFormat.Csv, ',', options)
      case RecordImportFormat.Tsv => _decode_delimited(text, RecordImportFormat.Tsv, '\t', options)
      case RecordImportFormat.Ltsv => _decode_ltsv(text, options)
      case RecordImportFormat.Lines => _decode_lines(text, options)
      case RecordImportFormat.Json => _decoder.jsonAutoRecords(text).map(_shape(_, RecordImportFormat.Json, options, Vector.empty, Record.empty))
      case RecordImportFormat.Yaml => _decoder.yamlAutoRecords(text).map(_shape(_, RecordImportFormat.Yaml, options, Vector.empty, Record.empty))
      case RecordImportFormat.Xml => _decoder.xmlAutoRecords(text).map(_shape(_, RecordImportFormat.Xml, options, Vector.empty, Record.empty))
      case RecordImportFormat.Hocon => _decode_hocon_records(text).map(_shape(_, RecordImportFormat.Hocon, options, Vector.empty, Record.empty))
      case RecordImportFormat.Tsl => _decoder.tslRecords(text).map(_shape(_, RecordImportFormat.Tsl, options, Vector.empty, Record.empty))
      case RecordImportFormat.Excel => Consequence.argumentInvalid("Excel import requires decodeBytes or decodePath.")
    }

  def decode(
    text: String,
    format: RecordFormat,
    options: RecordImportOptions
  ): Consequence[RecordImportResult] =
    decode(text, RecordImportFormat.fromRecordFormat(format), options)

  def decodeAuto(
    text: String,
    options: RecordImportOptions = RecordImportOptions.default
  ): Consequence[RecordImportResult] =
    decode(text, _detect_format(text), options)

  def decodeBytes(
    bytes: Array[Byte],
    format: RecordImportFormat,
    options: RecordImportOptions = RecordImportOptions.default
  ): Consequence[RecordImportResult] =
    format match {
      case RecordImportFormat.Auto | RecordImportFormat.Excel => _decode_excel(bytes, options)
      case _ => decode(new String(bytes, java.nio.charset.StandardCharsets.UTF_8), format, options)
    }

  def decodeBytes(
    bytes: Array[Byte],
    format: RecordFormat,
    options: RecordImportOptions
  ): Consequence[RecordImportResult] =
    decodeBytes(bytes, RecordImportFormat.fromRecordFormat(format), options)

  def decodePath(
    path: Path,
    format: RecordImportFormat,
    options: RecordImportOptions = RecordImportOptions.default
  ): Consequence[RecordImportResult] =
    Consequence(Files.readAllBytes(path)).flatMap(decodeBytes(_, format, options))

  def decodePath(
    path: Path,
    format: RecordFormat,
    options: RecordImportOptions
  ): Consequence[RecordImportResult] =
    decodePath(path, RecordImportFormat.fromRecordFormat(format), options)

  private def _decode_excel(
    bytes: Array[Byte],
    options: RecordImportOptions
  ): Consequence[RecordImportResult] =
    Consequence {
      Using.resource(WorkbookFactory.create(ByteArrayInputStream(bytes))) { workbook =>
        val sheet = options.sheetName match {
          case Some(name) =>
            Option(workbook.getSheet(name)).getOrElse {
              throw new IllegalArgumentException(s"Excel sheet is not found: ${name}")
            }
          case None =>
            workbook.getSheetAt(0)
        }
        val formatter = DataFormatter()
        val rows = sheet.iterator().asScala.toVector.map { row =>
          val last = math.max(row.getLastCellNum.toInt, 0)
          (0 until last).toVector.map { index =>
            Option(row.getCell(index)).map(formatter.formatCellValue).getOrElse("").trim
          }
        }.filter(_.exists(_.nonEmpty))
        if (rows.isEmpty)
          throw new IllegalArgumentException("Excel input is empty")
        val schema = options.schema
        val headerdecision = _header_decision(rows.headOption.getOrElse(Vector.empty), schema, options)
        val columns =
          if (headerdecision.hasHeader)
            _columns_from_header(rows.headOption.getOrElse(Vector.empty), schema)
          else
            _columns_from_schema(schema).getOrElse {
              throw new IllegalArgumentException("Excel requires a header row or schema column order")
            }
        val datarows = if (headerdecision.hasHeader) rows.drop(1) else rows
        val records = datarows.map { values =>
          Record.create(columns.zipAll(values, ImportColumn.empty, "").flatMap {
            case (column, value) if column.name.nonEmpty && value.trim.nonEmpty => Some(column.name -> value.trim)
            case _ => None
          })
        }
        val metadata = Record.create(Vector(
          "sheetName" -> sheet.getSheetName,
          "sheetIndex" -> workbook.getSheetIndex(sheet)
        ))
        _shape(records, RecordImportFormat.Excel, options, columns.flatMap(_.issue), metadata)
      }
    }

  private def _decode_delimited(
    text: String,
    format: RecordImportFormat,
    delimiter: Char,
    options: RecordImportOptions
  ): Consequence[RecordImportResult] =
    Consequence {
      val lines = _physical_lines(text, options)
      if (lines.isEmpty)
        throw new IllegalArgumentException(s"${format.label} input is empty")
      val preamble = _preamble(lines, options)
      val body = lines.drop(preamble.consumed)
      if (body.isEmpty)
        throw new IllegalArgumentException(s"${format.label} data rows are missing")
      val rows = body.map(_parse_delimited_line(_, delimiter))
      val schema = options.schema
      val headerdecision = _header_decision(rows.headOption.getOrElse(Vector.empty), schema, options)
      val columns =
        if (headerdecision.hasHeader)
          _columns_from_header(rows.headOption.getOrElse(Vector.empty), schema)
        else
          _columns_from_schema(schema).getOrElse {
            throw new IllegalArgumentException(s"${format.label} requires a header row or schema column order")
          }
      val datarows = if (headerdecision.hasHeader) rows.drop(1) else rows
      val records = datarows.map { values =>
        Record.create(columns.zipAll(values, ImportColumn.empty, "").flatMap {
          case (column, value) if column.name.nonEmpty && value.trim.nonEmpty => Some(column.name -> value.trim)
          case _ => None
        })
      }
      val issues = preamble.issues ++ columns.flatMap(_.issue)
      _shape(records, format, options, issues, preamble.metadata)
    }

  private def _decode_lines(
    text: String,
    options: RecordImportOptions
  ): Consequence[RecordImportResult] =
    Consequence {
      val fieldname = options.lineFieldName.getOrElse("value")
      val rows = _physical_lines(text, options).filterNot(_is_metadata_line(_, options))
      val records = rows.map(x => Record.create(Seq(fieldname -> x.trim)))
      _shape(records, RecordImportFormat.Lines, options, Vector.empty, Record.empty)
    }

  private def _decode_ltsv(
    text: String,
    options: RecordImportOptions
  ): Consequence[RecordImportResult] =
    Consequence {
      val lines = _physical_lines(text, options)
      if (lines.isEmpty)
        throw new IllegalArgumentException("LTSV input is empty")
      val preamble = _preamble(lines, options)
      val rows = lines.drop(preamble.consumed).filterNot(_is_metadata_line(_, options))
      val records = rows.zipWithIndex.map { case (line, index) =>
        val fields = line.split('\t').toVector.flatMap(_ltsv_field(_, index + 1))
        Record.create(fields)
      }
      _shape(records, RecordImportFormat.Ltsv, options, preamble.issues, preamble.metadata)
    }

  private def _ltsv_field(
    value: String,
    row: Int
  ): Option[(String, Any)] = {
    val idx = value.indexOf(':')
    if (idx <= 0)
      throw new IllegalArgumentException(s"Malformed LTSV field at row $row: $value")
    else {
      val key = value.substring(0, idx).trim
      val body = value.substring(idx + 1).trim
      Option.when(key.nonEmpty)(key -> body)
    }
  }

  private def _decode_hocon_records(
    text: String
  ): Consequence[Vector[Record]] =
    Consequence {
      _config_records(ConfigFactory.parseString(text).resolve())
    }

  private def _config_records(config: HoconConfig): Vector[Record] = {
    val root = config.root()
    val containerrecords = Vector("records", "items", "data").flatMap { key =>
      if (root.containsKey(key))
        Some(_config_records_from_value(root.get(key)))
      else
        None
    }
    containerrecords.find(_.nonEmpty).getOrElse(Vector(_config_object(root)))
  }

  private def _config_records_from_value(value: ConfigValue): Vector[Record] =
    value match {
      case obj: ConfigObject => Vector(_config_object(obj))
      case _ =>
        value.unwrapped() match {
          case xs: java.util.Collection[?] => xs.asScala.toVector.map(_plain_value_to_record)
          case other => Vector(Record.create(Vector("value" -> _plain_config_value(other))))
        }
    }

  private def _plain_value_to_record(value: Any): Record =
    value match {
      case m: java.util.Map[?, ?] =>
        Record.create(m.asScala.toVector.map {
          case (k, v) => k.toString -> _plain_config_value(v)
        })
      case other => Record.create(Vector("value" -> _plain_config_value(other)))
    }

  private def _plain_config_value(value: Any): Any =
    value match {
      case null => null
      case _: java.lang.Boolean => value
      case _: java.lang.Number => BigDecimal(value.toString)
      case s: String => s
      case m: java.util.Map[?, ?] =>
        Record.create(m.asScala.toVector.map {
          case (k, v) => k.toString -> _plain_config_value(v)
        })
      case xs: java.util.Collection[?] => xs.asScala.toVector.map(_plain_config_value)
      case other => other.toString
    }

  private def _shape(
    records: Vector[Record],
    format: RecordImportFormat,
    options: RecordImportOptions,
    issues: Vector[RecordImportIssue],
    metadata: Record
  ): RecordImportResult = {
    val shaped =
      options.schema.map(schema => records.map(_shape_record(_, schema, options))).getOrElse(records.map(x => x -> Vector.empty))
    RecordImportResult(
      records = shaped.map(_._1),
      metadata = metadata,
      issues = issues ++ shaped.flatMap(_._2),
      detectedFormat = format
    )
  }

  private def _shape_record(
    record: Record,
    schema: Schema,
    options: RecordImportOptions
  ): (Record, Vector[RecordImportIssue]) = {
    val columns = schema.columns
    val bynormalized = columns.map(x => _normalize(x.name.value) -> x).toMap
    val fields = scala.collection.mutable.ArrayBuffer.empty[(String, Any)]
    val issues = scala.collection.mutable.ArrayBuffer.empty[RecordImportIssue]
    val seen = scala.collection.mutable.Set.empty[String]
    record.fields.foreach { field =>
      bynormalized.get(_normalize(field.key)) match {
        case Some(column) =>
          seen += column.name.value
          val value = field.value.single
          if (options.coerceBySchema) {
            _coerce(value, column.domain.datatype) match {
              case Right(v) => fields += column.name.value -> v
              case Left(message) =>
                issues += RecordImportIssue("type-mismatch", Some(field.key), None, message)
                fields += column.name.value -> value
            }
          } else {
            fields += column.name.value -> value
          }
        case None =>
          options.unknownFieldPolicy match {
            case UnknownFieldPolicy.Keep => fields += field.key -> field.value.single
            case UnknownFieldPolicy.Drop => ()
            case UnknownFieldPolicy.Issue =>
              issues += RecordImportIssue("unknown-field", Some(field.key), None, s"Unknown field: ${field.key}")
              fields += field.key -> field.value.single
          }
      }
    }
    val ordered = columns.flatMap(column => fields.find(_._1 == column.name.value)) ++ fields.filterNot(x => seen.contains(x._1))
    Record.create(ordered) -> issues.toVector
  }

  private def _coerce(
    value: Any,
    datatype: DataType
  ): Either[String, Any] =
    datatype match {
      case XString => Right(Option(value).map(_.toString).orNull)
      case XBoolean => _string(value).flatMap {
        case "true" | "t" | "yes" | "y" | "1" => Right(true)
        case "false" | "f" | "no" | "n" | "0" => Right(false)
        case x => Left(s"Invalid boolean value: $x")
      }
      case XInt => _string(value).flatMap(x => x.toIntOption.toRight(s"Invalid int value: $x"))
      case XLong => _string(value).flatMap(x => x.toLongOption.toRight(s"Invalid long value: $x"))
      case XFloat => _string(value).flatMap(x => x.toFloatOption.toRight(s"Invalid float value: $x"))
      case XDouble => _string(value).flatMap(x => x.toDoubleOption.toRight(s"Invalid double value: $x"))
      case XInteger => _string(value).flatMap(x => scala.util.Try(BigInt(x)).toOption.toRight(s"Invalid integer value: $x"))
      case XDecimal => _string(value).flatMap(x => scala.util.Try(BigDecimal(x)).toOption.toRight(s"Invalid decimal value: $x"))
      case _ => Right(value)
    }

  private def _string(value: Any): Either[String, String] =
    Option(value).map(_.toString.trim.toLowerCase(java.util.Locale.ROOT)).filter(_.nonEmpty).toRight("Value is empty")

  private def _header_decision(
    row: Vector[String],
    schema: Option[Schema],
    options: RecordImportOptions
  ): HeaderDecision =
    options.headerMode match {
      case HeaderMode.Present => HeaderDecision(true)
      case HeaderMode.Absent => HeaderDecision(false)
      case HeaderMode.Auto =>
        if (schema.isEmpty)
          HeaderDecision(true)
        else
          HeaderDecision(row.exists(_header_cell_resolves(_, schema.get)))
    }

  private def _header_cell_resolves(
    cell: String,
    schema: Schema
  ): Boolean = {
    val parsed = ImportColumn.parse(cell, None)
    val candidates = Vector(parsed.name, parsed.sourceLabel.getOrElse("")).filter(_.nonEmpty)
    candidates.exists(name => schema.columns.exists(column => _same_name(column.name.value, name) || column.label.exists(x => _same_name(x.value.displayMessage, name))))
  }

  private def _columns_from_header(
    header: Vector[String],
    schema: Option[Schema]
  ): Vector[ImportColumn] =
    header.map { cell =>
      val parsed = ImportColumn.parse(cell, schema)
      schema.flatMap(_resolve_column(_, parsed.name)) match {
        case Some(column) =>
          val issue =
            parsed.datatype.filterNot(_same_type(_, column.domain.datatype.name)).map { datatype =>
              RecordImportIssue("header-type-conflict", Some(parsed.name), None, s"Header type '$datatype' conflicts with schema type '${column.domain.datatype.name}'.")
            }
          parsed.copy(name = column.name.value, issue = issue)
        case None => parsed
      }
    }

  private def _columns_from_schema(
    schema: Option[Schema]
  ): Option[Vector[ImportColumn]] =
    schema.map(_.columns.map(column => ImportColumn(column.name.value, None, None, None)))

  private def _resolve_column(
    schema: Schema,
    name: String
  ): Option[Column] =
    schema.columns.find { column =>
      _same_name(column.name.value, name) || column.label.exists(x => _same_name(x.value.displayMessage, name))
    }

  private def _same_name(
    lhs: String,
    rhs: String
  ): Boolean =
    _normalize(lhs) == _normalize(rhs)

  private def _same_type(
    lhs: String,
    rhs: String
  ): Boolean =
    _normalize(lhs) == _normalize(rhs)

  private def _normalize(value: String): String =
    value.trim.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "")

  private def _preamble(
    lines: Vector[String],
    options: RecordImportOptions
  ): Preamble = {
    val metadata = scala.collection.mutable.ArrayBuffer.empty[(String, Any)]
    val issues = scala.collection.mutable.ArrayBuffer.empty[RecordImportIssue]
    var consumed = 0
    var active = true
    lines.foreach { line =>
      if (active && _is_metadata_line(line, options)) {
        consumed = consumed + 1
        _metadata_pair(line, options) match {
          case Some(pair) => metadata += pair
          case None => issues += RecordImportIssue("metadata", None, None, s"Ignored metadata line: $line")
        }
      } else if (active && line.trim.isEmpty) {
        consumed = consumed + 1
      } else {
        active = false
      }
    }
    Preamble(consumed, Record.create(metadata.toVector), issues.toVector)
  }

  private def _is_metadata_line(
    line: String,
    options: RecordImportOptions
  ): Boolean =
    line.trim.startsWith(options.commentPrefix)

  private def _metadata_pair(
    line: String,
    options: RecordImportOptions
  ): Option[(String, Any)] = {
    val body = line.trim.stripPrefix(options.commentPrefix).trim.stripPrefix("@").trim
    val idx = body.indexOf(':')
    if (idx <= 0)
      None
    else
      Some(body.substring(0, idx).trim -> body.substring(idx + 1).trim)
  }

  private def _physical_lines(
    text: String,
    options: RecordImportOptions
  ): Vector[String] =
    text.linesIterator.map(_.trim).filter(_.nonEmpty).toVector

  private def _parse_delimited_line(
    line: String,
    delimiter: Char
  ): Vector[String] = {
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
        case c if c == delimiter && !inquotes =>
          values += current.toString.trim
          current.clear()
        case c =>
          current += c
      }
      i += 1
    }
    values += current.toString.trim
    values.toVector
  }

  private def _detect_format(
    text: String
  ): RecordImportFormat = {
    val trimmed = text.trim
    if (trimmed.startsWith("{") || trimmed.startsWith("["))
      RecordImportFormat.Json
    else if (trimmed.startsWith("<"))
      RecordImportFormat.Xml
    else {
      val lines = trimmed.linesIterator.map(_.trim).filter(_.nonEmpty).filterNot(_.startsWith("#")).toVector
      val first = lines.headOption.getOrElse("")
      if (_looks_like_ltsv(first))
        RecordImportFormat.Ltsv
      else if (first.contains("\t"))
        RecordImportFormat.Tsv
      else if (first.contains(","))
        RecordImportFormat.Csv
      else if (first.contains("="))
        RecordImportFormat.Hocon
      else if (first.startsWith("- ") || first.contains(": "))
        RecordImportFormat.Yaml
      else
        RecordImportFormat.Lines
    }
  }

  private def _config_value(value: ConfigValue): Any =
    value match {
      case obj: ConfigObject => _config_object(obj)
      case _ => _plain_config_value(value.unwrapped())
    }

  private def _config_object(obj: ConfigObject): Record =
    Record.create(obj.entrySet().asScala.toVector.map(entry => entry.getKey -> _config_value(entry.getValue)))
}

object RecordImportDecoder {
  final case class Config()

  object Config {
    val default: Config = Config()
  }

  private def _looks_like_ltsv(line: String): Boolean =
    line.contains('\t') && line.split('\t').toVector.exists(_.contains(":"))

  enum RecordImportFormat {
    case Auto, Csv, Tsv, Ltsv, Lines, Json, Yaml, Xml, Hocon, Tsl, Excel

    def label: String =
      productPrefix.toUpperCase(java.util.Locale.ROOT)
  }

  object RecordImportFormat {
    def fromRecordFormat(format: RecordFormat): RecordImportFormat =
      format match {
        case RecordFormat.Json => Json
        case RecordFormat.Yaml => Yaml
        case RecordFormat.Xml => Xml
        case RecordFormat.Hocon => Hocon
        case RecordFormat.Csv => Csv
        case RecordFormat.Tsv => Tsv
        case RecordFormat.Ltsv => Ltsv
        case RecordFormat.Lines => Lines
        case RecordFormat.Tsl => Tsl
        case RecordFormat.Excel => Excel
      }

    def parse(value: String): Option[RecordImportFormat] =
      value.trim.toLowerCase(java.util.Locale.ROOT) match {
        case "auto" => Some(Auto)
        case "csv" => Some(Csv)
        case "tsv" => Some(Tsv)
        case "ltsv" => Some(Ltsv)
        case "lines" | "line" | "line-delimited" | "isbn-lines" | "isbn" | "text" => Some(Lines)
        case "json" => Some(Json)
        case "yaml" | "yml" => Some(Yaml)
        case "xml" => Some(Xml)
        case "hocon" | "conf" => Some(Hocon)
        case "tsl" => Some(Tsl)
        case "excel" | "xlsx" | "xls" => Some(Excel)
        case _ => None
      }
  }

  final case class RecordImportOptions(
    schema: Option[Schema] = None,
    lineFieldName: Option[String] = None,
    commentPrefix: String = "#",
    headerMode: HeaderMode = HeaderMode.Auto,
    coerceBySchema: Boolean = false,
    unknownFieldPolicy: UnknownFieldPolicy = UnknownFieldPolicy.Keep,
    sheetName: Option[String] = None
  )

  object RecordImportOptions {
    val default: RecordImportOptions = RecordImportOptions()
  }

  final case class RecordImportResult(
    records: Vector[Record],
    metadata: Record,
    issues: Vector[RecordImportIssue],
    detectedFormat: RecordImportFormat
  )

  final case class RecordImportIssue(
    code: String,
    field: Option[String],
    row: Option[Int],
    message: String
  )

  enum HeaderMode {
    case Auto, Present, Absent
  }

  enum UnknownFieldPolicy {
    case Keep, Drop, Issue
  }

  private final case class HeaderDecision(hasHeader: Boolean)

  private final case class Preamble(
    consumed: Int,
    metadata: Record,
    issues: Vector[RecordImportIssue]
  )

  private final case class ImportColumn(
    name: String,
    sourceLabel: Option[String],
    datatype: Option[String],
    issue: Option[RecordImportIssue]
  )

  private object ImportColumn {
    val empty: ImportColumn = ImportColumn("", None, None, None)

    def parse(
      value: String,
      schema: Option[Schema]
    ): ImportColumn = {
      val parts = value.split(":").toVector.map(_.trim).filter(_.nonEmpty)
      parts match {
        case Vector() => empty
        case Vector(one) => ImportColumn(one, None, None, None)
        case Vector(first, second) if _is_type(second) => ImportColumn(first, None, Some(second), None)
        case Vector(first, second) => ImportColumn(second, Some(first), None, None)
        case Vector(first, second, third) => ImportColumn(second, Some(first), Some(third), None)
        case xs => ImportColumn(xs.last, Some(xs.head), None, Some(RecordImportIssue("header", Some(value), None, s"Unsupported header annotation: $value")))
      }
    }

    private def _is_type(value: String): Boolean =
      Set("string", "boolean", "int", "integer", "long", "float", "double", "decimal", "datetime", "date").contains(
        value.trim.toLowerCase(java.util.Locale.ROOT)
      )
  }
}
