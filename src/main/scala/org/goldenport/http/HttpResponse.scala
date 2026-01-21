package org.goldenport.http

import java.io.InputStream
import java.nio.charset.{Charset, StandardCharsets}
import org.goldenport.datatype.{MimeType, ContentType}
import org.goldenport.text.Presentable
import org.goldenport.bag.{Bag, BinaryBag, TextBag}
import org.goldenport.record.Record
import org.goldenport.util.Strings

/*
 * Unify arcadia
 * 
 * @since   Oct.  8, 2017
 *  version Aug. 29, 2018
 *  version Sep. 18, 2018
 *  version Oct.  8, 2018
 *  version Apr. 21, 2019
 *  version Feb. 21, 2021
 *  version Dec. 25, 2025
 * @version Jan. 21, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait HttpResponse extends Presentable {
  def status: HttpStatus
  final def code: Int = status.code
  def contentType: ContentType
  def mime: MimeType = contentType.mimeType
  def charset: Option[Charset] = contentType.charset
  def bag: Bag
  final def getString: Option[String] = bag match {
    case t: TextBag => Some(t.toText)
    case _ => None
  }
  final def getBinary: Option[BinaryBag] = bag match {
    case b: BinaryBag => Some(b)
    case _ => None
  }
  final def getRecord: Option[Record] = None
  final def getRecords: Option[List[Record]] = None
  def show: String
  def isSuccess = status == HttpStatus.Ok
  def isNotFound = status == HttpStatus.NotFound
  def toStatusCode: StatusCode = StatusCode(code)
  def toConclusion: Conclusion = Conclusion(toStatusCode)
}

object HttpResponse {
  case class Text(
    status: HttpStatus,
    contentType: ContentType,
    bag: TextBag
  ) extends HttpResponse {
    def print = show
    override lazy val show = s"Response(${getString.map(Strings.cutstring(_, 32)).getOrElse("")})"
    def json = RAISE.unsupportedOperationFault
  }

  case class Binary(
    status: HttpStatus,
    contentType: ContentType,
    bag: BinaryBag
  ) extends HttpResponse {
    def print = show
    override lazy val show = s"Response(Binary[${bag.metadata.size.getOrElse(0)}])"
  }

  def parser(code: Int, header: Map[String, IndexedSeq[String]], in: InputStream): HttpResponse = {
    val contenttype = header.get("Content-Type").flatMap(_.headOption.map(ContentType.parse)).getOrElse(ContentType.APPLICATION_OCTET_STREAM)
    val status = HttpStatus.fromInt(code).getOrElse(HttpStatus.InternalServerError)
    def text = {
      contenttype.charset.
        map(IoUtils.toText(in, _)).
        getOrElse(
          if (contenttype.mimeType.isHtml)
            parseHtml(contenttype, in)
          else if (contenttype.mimeType.isXml)
            parseXml(contenttype, in)
          else
            IoUtils.toText(in)
        )
    }
    def binary = in.readAllBytes()
    if (contenttype.mimeType.isText)
      HttpResponse.Text(status, contenttype, Bag.text(text))
    else
      HttpResponse.Binary(status, contenttype, Bag.binary(binary))
  }

  private val _regex_xml = """(?i)[<][?]xml[ ][^?]+(encoding[ ]*[=][ ]*["]([^"]+)["])""".r
  private val _regex_html4 = """(?i)<meta[^>]+content[ ]*=[ ]*["]([^"]+)["]""".r
  private val _regex_html5 = """(?i)<meta[^>]+charset[ ]*=[ ]*["]([^"]+)["]""".r

  def parseHtml(contenttype: ContentType, in: InputStream) = {
    val bag = BufferFileBag.fromInputStream(in)
    val firstpage = bag.toTextFirstPage
    val charset = _regex_html4.findFirstMatchIn(firstpage).
      flatMap(x =>
        ContentType.parse(x.group(1)).charset.map(_.name())
      ).orElse(
        _regex_html5.findFirstMatchIn(firstpage).
          map(_.group(1))
      ).orElse(
        _regex_xml.findFirstMatchIn(firstpage).
          map(_.group(2))
      )
    charset
      .flatMap(name => Option(Charset.forName(name)))
      .map(bag.toText)
      .getOrElse(bag.toText)
  }

  def parseXml(contenttype: ContentType, in: InputStream) = {
    val bag = BufferFileBag.fromInputStream(in)
    val firstpage = bag.toTextFirstPage
    val charset = _regex_xml.findFirstMatchIn(firstpage).map(_.group(2))
    charset
      .flatMap(name => Option(Charset.forName(name)))
      .map(bag.toText)
      .getOrElse(bag.toText)
  }

  def html(p: String): HttpResponse.Text = HttpResponse.Text(
    HttpStatus.Ok,
    ContentType.TEXT_HTML,
    Bag.text(p)
  )

  def text(
    status: HttpStatus,
    body: String,
    charset: Charset = StandardCharsets.UTF_8
  ): HttpResponse =
    HttpResponse.Text(
      status,
      ContentType(MimeType("text/plain"), Some(charset)),
      Bag.text(body)
    )

  def notFound(body: String = "not found"): HttpResponse =
    text(HttpStatus.NotFound, body)

  def internalServerError(body: String = "internal server error"): HttpResponse =
    text(HttpStatus.InternalServerError, body)
}

// ---------------------------------------------------------------------
// HTTP Status abstraction (protocol-level, minimal)
// ---------------------------------------------------------------------
sealed abstract class HttpStatus(val code: Int)

object HttpStatus {
  case object Ok extends HttpStatus(200)
  case object BadRequest extends HttpStatus(400)
  case object NotFound extends HttpStatus(404)
  case object InternalServerError extends HttpStatus(500)

  def fromInt(code: Int): Option[HttpStatus] =
    code match {
      case 200 => Some(Ok)
      case 400 => Some(BadRequest)
      case 404 => Some(NotFound)
      case 500 => Some(InternalServerError)
      case _   => None
    }
}
