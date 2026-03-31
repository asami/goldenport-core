package org.goldenport.protocol

import java.nio.charset.Charset
import org.goldenport.protocol.scalar.ScalarValue
import org.goldenport.text.Presentable
import org.goldenport.datatype.{MimeType, ContentType}
import org.goldenport.bag.{Bag, TextBag, BinaryBag}
import org.goldenport.record.Recordable
import org.goldenport.record.io.RecordEncoder

/**
 * Response represents protocol-level output semantics.
 * It is independent of transport (CLI / REST / MCP).
 */
/*
 * @since   Jan.  1, 2026
 *  version Jan.  3, 2026
 *  version Jan. 21, 2026
 * @version Mar. 31, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class Response extends Presentable {
  def mimeType: MimeType
  def charset: Option[Charset] = None
  def contentType: ContentType = ContentType(mimeType, charset)
  def bag: Bag
}

abstract class TextResponse extends Response {
  def bag: Bag = textBag
  def textBag: TextBag = Bag.text(print)
}

abstract class BinaryResponse extends Response {
  def bag: Bag = binaryBag
  def binaryBag: BinaryBag
}

object Response {
  final case class Void() extends TextResponse {
    def mimeType = MimeType.TEXT_PLAIN
    def print = ""
    override def display: String = "Void"
    override def show: String = "Void"
  }

  /** JSON representation (protocol-level, transport-agnostic). */
  final case class Json(value: String) extends TextResponse {
    def mimeType = MimeType.APPLICATION_JSON
    def print = value
    override def display: String = value
    override def show: String = value
  }

  final case class Yaml(value: String) extends TextResponse {
    def mimeType = MimeType.APPLICATION_YAML
    def print = value
    override def display: String = value
    override def show: String = value
  }

  final case class Xml(value: String) extends TextResponse {
    def mimeType = MimeType.APPLICATION_XML
    def print = value
    override def display: String = value
    override def show: String = value
  }

  /**
   * Scalar response value.
   *
   * Intended only for Scala primitive-like / Predef types
   * such as Int, Long, Double, Boolean, String.
   *
   * Structured or semantic values MUST NOT use Scalar.
   */
  /**
   * Scalar wraps a value permitted by ScalarValue.
   *
   * It MUST be used only for atomic Scala values.
   * Structured, semantic, or document-like values are out of scope.
   */
  final case class Scalar[T](value: T)(using val scalarValue: ScalarValue[T]) extends TextResponse {
    def mimeType = MimeType.TEXT_PLAIN
    def print = Presentable.print(value)
    override def display: String = Presentable.display(value)
    override def show: String = Presentable.show(value)
  }

  final case class Opaque(value: Any) extends TextResponse {
    private def _json = value match {
      case m: Recordable => Some(RecordEncoder().json(m.toRecord()))
      case _ => None
    }

    def mimeType = if (_json.isDefined) MimeType.APPLICATION_JSON else MimeType.APPLICATION_OCTET_STREAM
    def print = _json.getOrElse(Presentable.print(value))
    override def display: String = _json.getOrElse(Presentable.display(value))
    override def show: String = _json.getOrElse(Presentable.show(value))
  }
}
