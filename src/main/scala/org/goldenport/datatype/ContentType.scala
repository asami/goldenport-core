package org.goldenport.datatype

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/*
 * @since   Dec. 25, 2025
 * @version Jan. 21, 2026
 * @author  ASAMI, Tomoharu
 */
final case class ContentType(
  mimeType: MimeType,
  charset: Option[Charset],
  parameters: Map[String, String] = Map.empty
) extends DataType() {
  def parameter(name: String): Option[String] =
    parameters.get(name.toLowerCase)

  def header: String = {
    val builder = new StringBuilder(mimeType.print)
    charset.foreach(cs => builder.append(s"; charset=${cs.name()}"))
    parameters
      .iterator
      .filterNot { case (name, _) => name.equalsIgnoreCase("charset") }
      .foreach { case (name, value) => builder.append(s"; ${name}=${value}") }
    builder.toString()
  }

  override def print: String = header

  override def show: String =
    s"ContentType(mimeType=${mimeType.show}, charset=${charset.map(_.name()).getOrElse("none")}, params=${parameters})"
}

object ContentType {
  val APPLICATION_OCTET_STREAM: ContentType =
    ContentType(MimeType.APPLICATION_OCTET_STREAM, None)

  val TEXT_PLAIN: ContentType =
    ContentType(MimeType.TEXT_PLAIN, None)
  val TEXT_PLAIN_UTF8: ContentType =
    ContentType(MimeType.TEXT_PLAIN, Some(StandardCharsets.UTF_8))
  val TEXT_HTML: ContentType =
    ContentType(MimeType.TEXT_HTML, None)
  val TEXT_HTML_UTF8: ContentType =
    ContentType(MimeType.TEXT_HTML, Some(StandardCharsets.UTF_8))
  val TEXT_XML: ContentType =
    ContentType(MimeType.TEXT_XML, None)
  val TEXT_CSV: ContentType =
    ContentType(MimeType.TEXT_CSV, None)
  val TEXT_MARKDOWN: ContentType =
    ContentType(MimeType.TEXT_MARKDOWN, None)

  val APPLICATION_JSON: ContentType =
    ContentType(MimeType.APPLICATION_JSON, None)
  val APPLICATION_JSON_UTF8: ContentType =
    ContentType(MimeType.APPLICATION_JSON, Some(StandardCharsets.UTF_8))
  val APPLICATION_XML: ContentType =
    ContentType(MimeType.APPLICATION_XML, None)
  val APPLICATION_YAML: ContentType =
    ContentType(MimeType.APPLICATION_YAML, None)
  val APPLICATION_PDF: ContentType =
    ContentType(MimeType.APPLICATION_PDF, None)
  val APPLICATION_ZIP: ContentType =
    ContentType(MimeType.APPLICATION_ZIP, None)
  val APPLICATION_GZIP: ContentType =
    ContentType(MimeType.APPLICATION_GZIP, None)
  val APPLICATION_FORM_URLENCODED: ContentType =
    ContentType(MimeType.APPLICATION_FORM_URLENCODED, None)
  val APPLICATION_FORM_URLENCODED_UTF8: ContentType =
    ContentType(MimeType.APPLICATION_FORM_URLENCODED, Some(StandardCharsets.UTF_8))
  val MULTIPART_FORM_DATA: ContentType =
    ContentType(MimeType.MULTIPART_FORM_DATA, None)

  val IMAGE_PNG: ContentType =
    ContentType(MimeType.IMAGE_PNG, None)
  val IMAGE_JPEG: ContentType =
    ContentType(MimeType.IMAGE_JPEG, None)
  val IMAGE_GIF: ContentType =
    ContentType(MimeType.IMAGE_GIF, None)
  val IMAGE_SVG: ContentType =
    ContentType(MimeType.IMAGE_SVG, None)

  def parse(value: String): ContentType = {
    val parts = value.split(";").toVector.map(_.trim).filter(_.nonEmpty)
    val mime = parts.headOption.getOrElse("application/octet-stream")
    val params = parts.drop(1).flatMap { part =>
      val idx = part.indexOf("=")
      if (idx > 0) {
        val key = part.substring(0, idx).trim
        val data = part.substring(idx + 1).trim
        Some(key -> data)
      } else None
    }
    val charset = params.find { case (name, _) => name.equalsIgnoreCase("charset") }
      .flatMap { case (_, name) => Option(Charset.forName(name)) }
    val parameterMap = params.foldLeft(Map.empty[String, String]) {
      case (acc, (name, data)) if name.equalsIgnoreCase("charset") => acc
      case (acc, (name, data)) => acc + (name.toLowerCase -> data)
    }
    ContentType(
      mimeType = MimeType(mime),
      charset = charset,
      parameters = parameterMap
    )
  }
}
