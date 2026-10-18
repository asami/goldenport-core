package org.goldenport.record.io

import java.io.ByteArrayOutputStream
import cats.data.NonEmptyVector
import io.circe.Json
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import org.goldenport.Consequence
import org.goldenport.record.{Record, RecordFormat}
import org.goldenport.schema.{Schema, StructuredDataJson, XSemiStructuredData}

/*
 * @since   May. 24, 2026
 *  version Jul. 21, 2026
 * @version Oct. 18, 2026
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
          extension = _extension(format, options.extensionPurpose),
          format = format
        )
      }
    }

  def encodeText(
    records: Vector[Record],
    format: RecordFormat,
    options: RecordExportOptions = RecordExportOptions.default
  ): Consequence[String] =
    format match {
      case RecordFormat.Json | RecordFormat.Tsl => _json(records).map(_.spaces2)
      case RecordFormat.Yaml => _yaml(records)
      case RecordFormat.Hocon => _hocon(records)
      case _ => Consequence {
        format match {
        case RecordFormat.Xml => _xml(records)
        case RecordFormat.Toml => throw new IllegalArgumentException("TOML export is not supported.")
        case RecordFormat.Properties => _properties(records)
        case RecordFormat.Csv => _delimited(records, ",", options)
        case RecordFormat.Tsv => _delimited(records, "\t", options)
        case RecordFormat.Ltsv => _ltsv(records)
        case RecordFormat.Lines => _lines(records)
        case RecordFormat.Excel => throw new IllegalArgumentException("Excel export is binary; use encodeBytes.")
        case _ => throw new IllegalArgumentException("Unsupported text export format.")
        }
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
            row.createCell(colindex).setCellValue(record.getAny(column).map(_cell_text).getOrElse(""))
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
      columns.map(column => _delimited_cell(record.getAny(column).map(_cell_text).getOrElse(""), separator)).mkString(separator)
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

  private def _yaml(records: Vector[Record]): Consequence[String] =
    Consequence.zipN(records.map(RecordEncoder.yamlC)).map { rows =>
      if (rows.isEmpty) {
        "[]\n"
      } else {
        rows.map { row =>
          val lines = row.stripSuffix("\n").linesIterator.toVector
          "- " + lines.headOption.getOrElse("{}") +
            lines.drop(1).map(line => "\n  " + line).mkString
        }.mkString("\n") + "\n"
      }
    }

  private def _xml(records: Vector[Record]): String = {
    val body = records.map { record =>
      val fields = record.fields.map { field =>
        s"<${_xml_name(field.key)}>${_xml_escape(_cell_text(field.value.single))}</${_xml_name(field.key)}>"
      }.mkString
      s"<record>${fields}</record>"
    }.mkString
    s"""<?xml version="1.0" encoding="UTF-8"?><records>${body}</records>"""
  }

  private def _hocon(records: Vector[Record]): Consequence[String] =
    _json(records).map(json => s"records = ${_hocon_number_tokens(json).spaces2}\n")

  private def _hocon_number_tokens(json: Json): Json =
    json.fold(
      json,
      _ => json,
      number => number.toBigDecimal match {
        case Some(decimal) if decimal.isWhole && !decimal.isValidLong =>
          Json.fromBigDecimal(new BigDecimal(decimal.bigDecimal.setScale(1), java.math.MathContext.UNLIMITED))
        case _ => json
      },
      _ => json,
      values => Json.fromValues(values.map(_hocon_number_tokens)),
      fields => Json.fromJsonObject(fields.mapValues(_hocon_number_tokens))
    )

  private def _properties(records: Vector[Record]): String = {
    val pairs =
      if (records.size <= 1)
        records.headOption.toVector.flatMap(_.fields.map(field => field.key -> _cell_text(field.value.single)))
      else
        records.zipWithIndex.flatMap { case (record, rowindex) =>
          record.fields.map(field => s"records.${rowindex}.${field.key}" -> _cell_text(field.value.single))
        }
    pairs.map { case (key, value) =>
      s"${_properties_escape(key, keypart = true)}=${_properties_escape(value, keypart = false)}"
    }.mkString("\n") + "\n"
  }

  private def _json(records: Vector[Record]): Consequence[Json] =
    Consequence.zipN(records.map(XSemiStructuredData.toJson)).map(Json.fromValues)

  private def _cell_text(value: Any): String =
    value match {
      case record: Record => StructuredDataJson.valueToJson(record).TAKE.noSpaces
      case values: NonEmptyVector[?] => StructuredDataJson.valueToJson(values).TAKE.noSpaces
      case values: Seq[?] => StructuredDataJson.valueToJson(values).TAKE.noSpaces
      case values: Array[?] => StructuredDataJson.valueToJson(values).TAKE.noSpaces
      case other => other.toString
    }

  private def _properties_escape(
    value: String,
    keypart: Boolean
  ): String = {
    val escaped = value.flatMap {
      case '\\' => "\\\\"
      case '\n' => "\\n"
      case '\r' => "\\r"
      case '\t' => "\\t"
      case '=' => "\\="
      case ':' => "\\:"
      case '#' => "\\#"
      case '!' => "\\!"
      case ' ' if keypart => "\\ "
      case ch => ch.toString
    }
    escaped
  }

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

  enum RecordExportExtensionPurpose {
    case Configuration
    case Definition
  }

  final case class RecordExportOptions(
    schema: Option[Schema] = None,
    sheetName: Option[String] = None,
    extensionPurpose: RecordExportExtensionPurpose = RecordExportExtensionPurpose.Configuration
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
      case RecordFormat.Properties => "text/x-java-properties"
      case _ => "application/json"
    }

  private def _extension(
    format: RecordFormat,
    purpose: RecordExportExtensionPurpose
  ): String =
    format match {
      case RecordFormat.Yaml => "yaml"
      case RecordFormat.Xml => "xml"
      case RecordFormat.Hocon => _hocon_extension(purpose)
      case RecordFormat.Properties => "properties"
      case RecordFormat.Csv => "csv"
      case RecordFormat.Tsv => "tsv"
      case RecordFormat.Ltsv => "ltsv"
      case RecordFormat.Lines => "txt"
      case RecordFormat.Excel => "xlsx"
      case _ => "json"
    }

  private def _hocon_extension(purpose: RecordExportExtensionPurpose): String =
    purpose match {
      case RecordExportExtensionPurpose.Configuration => "conf"
      case RecordExportExtensionPurpose.Definition => "hocon"
    }
}
