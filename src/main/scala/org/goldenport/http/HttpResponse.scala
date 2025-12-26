package org.goldenport.http

import java.io.InputStream
import java.nio.charset.Charset
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
 * @version Dec. 25, 2025
 * @author  ASAMI, Tomoharu
 */
sealed trait HttpResponse {
  def code: Int
  def contentType: ContentType
  def mime: MimeType = contentType.mime
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
  def json: JsValue
  def show: String
  def isSuccess = code == 200
  def isNotFound = code == 404
  def toStatusCode: StatusCode = StatusCode(code)
  def toConclusion: Conclusion = Conclusion(toStatusCode)
}

object HttpResponse {
  def parser(code: Int, header: Map[String, IndexedSeq[String]], in: InputStream): HttpResponse = {
    val contenttype = header.get("Content-Type").flatMap(_.headOption.map(ContentType.parse)).getOrElse(ContentType.octetstream)
    def text = {
      contenttype.charset.
        map(IoUtils.toText(in, _)).
        getOrElse(
          if (contenttype.mime.isHtml)
            parseHtml(contenttype, in)
          else if (contenttype.mime.isXml)
            parseXml(contenttype, in)
          else
            IoUtils.toText(in)
        )
    }
    def binary = in.readAllBytes()
    if (contenttype.mime.isText)
      StringResponse(code, contenttype, Bag.text(text))
    else
      BinaryResponse(code, contenttype, Bag.binary(binary))
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

  def html(p: String): StringResponse = StringResponse(
    200,
    ContentType.html,
    Bag.text(p)
  )
}

case class StringResponse(
  code: Int,
  contentType: ContentType,
  bag: Bag
) extends HttpResponse {
  lazy val show = s"Response(${getString.map(Strings.cutstring(_, 32)).getOrElse("")})"
  def json = RAISE.unsupportedOperationFault
}

case class BinaryResponse(
  code: Int,
  contentType: ContentType,
  bag: Bag
) extends HttpResponse {
  lazy val show = s"Response(Binary[${bag.metadata.size.getOrElse(0)}])"
  def json = RAISE.unsupportedOperationFault
}
