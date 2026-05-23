package org.goldenport.record.io

import java.io.ByteArrayOutputStream
import scala.jdk.CollectionConverters.*
import io.circe.Json
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import org.goldenport.Consequence
import org.goldenport.record.{Field, Record, RecordFormat}
import org.goldenport.schema.Schema

/*
 * @since   May. 24, 2026
 * @version May. 24, 2026
 * @author  ASAMI, Tomoharu
 */
final class RecordExportEncoder(
  config: RecordExportEncoder.Config = RecordExportEncoder.Config.default
) {
  import RecordExportEncoder.*

  def encode(
    records: Vector[Record],
    format: RecordFormat,
    options: RecordExportOptions = RecordExportOptions.default
  ): Consequence[RecordExportResult] =
    format match {
      case RecordFormat.Excel => encodeBytes(records, format, options)
      case _ => encodeText(records, format, options).map { text =>
        RecordExportResult(
          bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8),
          text = Some(text),
          contentType = _text_content_type(format),
          extension = _extension(format),
          format = format
        )
      }
    }

  def encodeText(
    records: Vector[Record],
    format: RecordFormat,
    options: RecordExportOptions = RecordExportOptions.default
  ): Consequence[String] =
    Consequence {
      format match {
        case RecordFormat.Json => _json(records).spaces2
        case RecordFormat.Yaml => _yaml(records)
        case RecordFormat.Xml => _xml(records)
        case RecordFormat.Hocon => _hocon(records)
        case RecordFormat.Csv => _delimited(records, ",", options)
        case RecordFormat.Tsv => _delimited(records, "\t", options)
        case RecordFormat.Ltsv => _ltsv(records)
        case RecordFormat.Lines => _lines(records)
        case RecordFormat.Tsl => _json(records).spaces2
        case RecordFormat.Excel => throw new IllegalArgumentException("Excel export is binary; use encodeBytes.")
      }
    }

  def encodeBytes(
    records: Vector[Record],
    format: RecordFormat,
    options: RecordExportOptions = RecordExportOptions.default
  ): Consequence[RecordExportResult] =
    format match {
      case RecordFormat.Excel => _excel(records, options)
      case _ => encode(records, format, options)
    }

  private def _excel(
    records: Vector[Record],
    options: RecordExportOptions
  ): Consequence[RecordExportResult] =
    Consequence {
      val workbook = XSSFWorkbook()
      try {
        val sheet = workbook.createSheet(options.sheetName.getOrElse("records"))
        val columns = _columns(records, options)
        val header = sheet.createRow(0)
        columns.zipWithIndex.foreach { case (column, index) =>
          header.createCell(index).setCellValue(column)
        }
        records.zipWithIndex.foreach { case (record, rowindex) =>
          val row = sheet.createRow(rowindex + 1)
          columns.zipWithIndex.foreach { case (column, colindex) =>
            row.createCell(colindex).setCellValue(_cell_text(record.getAny(column).orNull))
          }
        }
        val out = ByteArrayOutputStream()
        workbook.write(out)
        RecordExportResult(
          bytes = out.toByteArray,
          text = None,
          contentType = ExcelContentType,
          extension = "xlsx",
          format = RecordFormat.Excel
        )
      } finally {
        workbook.close()
      }
    }

  private def _columns(
    records: Vector[Record],
    options: RecordExportOptions
  ): Vector[String] =
    options.schema.map(_.columns.map(_.name.value))
      .getOrElse(records.flatMap(_.fields.map(_.key)).distinct)

  private def _delimited(
    records: Vector[Record],
    separator: String,
    options: RecordExportOptions
  ): String = {
    val columns = _columns(records, options)
    val header = columns.map(_delimited_cell(_, separator)).mkString(separator)
    val rows = records.map { record =>
      columns.map(column => _delimited_cell(_cell_text(record.getAny(column).orNull), separator)).mkString(separator)
    }
    (header +: rows).mkString("\n") + "\n"
  }

  private def _delimited_cell(
    value: String,
    separator: String
  ): String =
    if (separator == ",") {
      val escaped = value.replace("\"", "\"\"")
      if (escaped.exists(ch => ch == ',' || ch == '"' || ch == '\n' || ch == '\r'))
        s""""${escaped}""""
      else
        escaped
    } else {
      value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')
    }

  private def _ltsv(records: Vector[Record]): String =
    records.map { record =>
      record.fields.map { field =>
        s"${_ltsv_label(field.key)}:${_ltsv_value(_cell_text(field.value.single))}"
      }.mkString("\t")
    }.mkString("\n") + "\n"

  private def _lines(records: Vector[Record]): String =
    records.map { record =>
      Vector("value", "isbn", "title", "name", "label")
        .flatMap(key => record.getAny(key).map(_cell_text))
        .headOption
        .getOrElse(_cell_text(record))
    }.mkString("\n") + "\n"

  private def _yaml(records: Vector[Record]): String =
    records.map { record =>
      val fields = record.fields.map { field =>
        s"  ${field.key}: ${_yaml_value(field.value.single)}"
      }.mkString("\n")
      s"-\n${fields}"
    }.mkString("\n") + "\n"

  private def _xml(records: Vector[Record]): String = {
    val body = records.map { record =>
      val fields = record.fields.map { field =>
        s"<${_xml_name(field.key)}>${_xml_escape(_cell_text(field.value.single))}</${_xml_name(field.key)}>"
      }.mkString
      s"<record>${fields}</record>"
    }.mkString
    s"""<?xml version="1.0" encoding="UTF-8"?><records>${body}</records>"""
  }

  private def _hocon(records: Vector[Record]): String = {
    val rows = records.map { record =>
      val fields = record.fields.map { field =>
        s"${field.key} = ${_hocon_value(field.value.single)}"
      }.mkString(", ")
      s"{ ${fields} }"
    }.mkString(",\n  ")
    s"records = [\n  ${rows}\n]\n"
  }

  private def _json(records: Vector[Record]): Json =
    Json.fromValues(records.map(_json_record))

  private def _json_record(record: Record): Json =
    Json.fromJsonObject(io.circe.JsonObject.fromIterable(record.fields.collect {
      case Field(key, Field.Value.Single(value)) => key -> _json_value(value)
    }))

  private def _json_value(value: Any): Json =
    value match {
      case null => Json.Null
      case record: Record => _json_record(record)
      case xs: Iterable[?] => Json.fromValues(xs.map(_json_value))
      case arr: Array[?] => Json.fromValues(arr.toSeq.map(_json_value))
      case b: java.lang.Boolean => Json.fromBoolean(b)
      case n: java.lang.Number => Json.fromBigDecimal(BigDecimal(n.toString))
      case s: String => Json.fromString(s)
      case other => Json.fromString(other.toString)
    }

  private def _cell_text(value: Any): String =
    value match {
      case null => ""
      case record: Record => _json_record(record).noSpaces
      case xs: Iterable[?] => Json.fromValues(xs.map(_json_value)).noSpaces
      case arr: Array[?] => Json.fromValues(arr.toSeq.map(_json_value)).noSpaces
      case other => other.toString
    }

  private def _yaml_value(value: Any): String =
    value match {
      case null => "null"
      case n: java.lang.Number => n.toString
      case b: java.lang.Boolean => b.toString
      case other => "\"" + _cell_text(other).replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }

  private def _hocon_value(value: Any): String =
    _yaml_value(value)

  private def _xml_name(value: String): String = {
    val text = value.map {
      case ch if ch.isLetterOrDigit || ch == '_' || ch == '-' || ch == '.' => ch
      case _ => '_'
    }
    if (text.headOption.exists(ch => ch.isLetter || ch == '_')) text else s"_${text}"
  }

  private def _xml_escape(value: String): String =
    value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

  private def _ltsv_label(value: String): String =
    value.map {
      case ch if ch.isLetterOrDigit || ch == '_' || ch == '-' || ch == '.' => ch
      case _ => '_'
    }

  private def _ltsv_value(value: String): String =
    value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')
}

object RecordExportEncoder {
  val ExcelContentType: String = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

  final case class Config()

  object Config {
    val default: Config = Config()
  }

  final case class RecordExportOptions(
    schema: Option[Schema] = None,
    sheetName: Option[String] = None
  )

  object RecordExportOptions {
    val default: RecordExportOptions = RecordExportOptions()
  }

  final case class RecordExportResult(
    bytes: Array[Byte],
    text: Option[String],
    contentType: String,
    extension: String,
    format: RecordFormat
  )

  private def _text_content_type(format: RecordFormat): String =
    format match {
      case RecordFormat.Csv => "text/csv"
      case RecordFormat.Tsv => "text/tab-separated-values"
      case RecordFormat.Ltsv | RecordFormat.Lines => "text/plain"
      case RecordFormat.Yaml => "application/yaml"
      case RecordFormat.Xml => "application/xml"
      case RecordFormat.Hocon => "application/hocon"
      case _ => "application/json"
    }

  private def _extension(format: RecordFormat): String =
    format match {
      case RecordFormat.Yaml => "yaml"
      case RecordFormat.Xml => "xml"
      case RecordFormat.Hocon => "conf"
      case RecordFormat.Csv => "csv"
      case RecordFormat.Tsv => "tsv"
      case RecordFormat.Ltsv => "ltsv"
      case RecordFormat.Lines => "txt"
      case RecordFormat.Excel => "xlsx"
      case _ => "json"
    }
}
