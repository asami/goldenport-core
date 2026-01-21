package org.goldenport.datatype

/*
 * @since   Dec. 25, 2025
 * @version Jan. 21, 2026
 * @author  ASAMI, Tomoharu
 */
final case class MimeType(value: String) extends DataType() {
  private def normalized: String = value.trim.toLowerCase

  def isHtml: Boolean = normalized.contains("html")
  def isXml: Boolean = normalized.contains("xml")
  def isText: Boolean =
    normalized.startsWith("text/") || isHtml || isXml || normalized.contains("json")

  override def print: String = value

  override def show: String = s"${getClass.getSimpleName}(${value})"
}

object MimeType {
  val TEXT_PLAIN = MimeType("text/plain")
  val TEXT_HTML = MimeType("text/html")
  val TEXT_XML = MimeType("text/xml")
  val TEXT_CSV = MimeType("text/csv")
  val TEXT_MARKDOWN = MimeType("text/markdown")

  val APPLICATION_JSON = MimeType("application/json")
  val APPLICATION_XML = MimeType("application/xml")
  val APPLICATION_YAML = MimeType("application/x-yaml")
  val APPLICATION_PDF = MimeType("application/pdf")
  val APPLICATION_ZIP = MimeType("application/zip")
  val APPLICATION_GZIP = MimeType("application/gzip")
  val APPLICATION_OCTET_STREAM = MimeType("application/octet-stream")
  val APPLICATION_FORM_URLENCODED = MimeType("application/x-www-form-urlencoded")
  val MULTIPART_FORM_DATA = MimeType("multipart/form-data")

  val IMAGE_PNG = MimeType("image/png")
  val IMAGE_JPEG = MimeType("image/jpeg")
  val IMAGE_GIF = MimeType("image/gif")
  val IMAGE_SVG = MimeType("image/svg+xml")

  private val suffixMap = Map(
    "json" -> APPLICATION_JSON,
    "jsonl" -> APPLICATION_JSON,
    "xml" -> APPLICATION_XML,
    "yaml" -> APPLICATION_YAML,
    "yml" -> APPLICATION_YAML,
    "txt" -> TEXT_PLAIN,
    "csv" -> TEXT_CSV,
    "html" -> TEXT_HTML,
    "htm" -> TEXT_HTML,
    "md" -> TEXT_MARKDOWN,
    "bin" -> APPLICATION_OCTET_STREAM
  )

  def fromSuffix(suffix: String): Option[MimeType] = {
    val normalized = suffix.trim.toLowerCase.stripPrefix(".")
    if normalized.isEmpty then None
    else suffixMap.get(normalized)
  }
}
