package org.goldenport.http

import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/*
 * @since   Dec. 25, 2025
 * @version Jan. 21, 2026
 * @author  ASAMI, Tomoharu
 */
trait NamedValueInstance {
  def name: String
}

trait EnumerationClass[A] {
  def elements: Vector[A]
}

// case class MimeType(value: String) {
//   def isHtml: Boolean = value.toLowerCase.contains("html")
//   def isXml: Boolean = value.toLowerCase.contains("xml")
//   def isText: Boolean =
//     value.toLowerCase.startsWith("text/") || isHtml || isXml || value.toLowerCase.contains("json")
// }

// case class ContentType(
//   mime: MimeType,
//   charset: Option[Charset]
// ) {
// }

// object ContentType {
//   val octetstream: ContentType = ContentType(MimeType("application/octet-stream"), None)
//   val html: ContentType = ContentType(MimeType("text/html"), Some(StandardCharsets.UTF_8))

//   def parse(s: String): ContentType = {
//     val parts = s.split(";").toVector.map(_.trim)
//     val mime = parts.headOption.getOrElse("application/octet-stream")
//     val charset = parts
//       .drop(1)
//       .find(_.toLowerCase.startsWith("charset="))
//       .map(_.substring("charset=".length))
//       .flatMap(name => Option(Charset.forName(name)))
//     ContentType(MimeType(mime), charset)
//   }
// }

trait ChunkBag {
  def size: Long
}

case class BufferFileBag(bytes: Array[Byte]) extends ChunkBag {
  def size: Long = bytes.length.toLong

  def toText: String = new String(bytes, StandardCharsets.UTF_8)

  def toText(charset: Charset): String = new String(bytes, charset)

  def toTextFirstPage: String = {
    val max = math.min(bytes.length, 2048)
    new String(bytes.slice(0, max), StandardCharsets.UTF_8)
  }
}

object BufferFileBag {
  def fromInputStream(in: InputStream): BufferFileBag = {
    BufferFileBag(in.readAllBytes())
  }
}

object IoUtils {
  def toText(in: InputStream): String = {
    new String(in.readAllBytes(), StandardCharsets.UTF_8)
  }

  def toText(in: InputStream, charset: Charset): String = {
    new String(in.readAllBytes(), charset)
  }
}

sealed trait JsValue

case class StatusCode(code: Int)

case class Conclusion(statusCode: StatusCode)

object RAISE {
  def unsupportedOperationFault[A]: A =
    throw new UnsupportedOperationException("unsupported operation")
}
