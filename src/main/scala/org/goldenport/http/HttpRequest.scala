package org.goldenport.http

import java.net.URL
import java.net.URLDecoder
import org.goldenport.record.Record
import org.goldenport.util.StringUtils
import org.goldenport.util.Strings
import org.goldenport.bag.Bag

/*
 * @since   Dec. 19, 2017
 *  version Aug. 19, 2018
 *  version Sep. 17, 2018
 *  version Oct. 30, 2018
 *  version Nov.  7, 2018
 *  version Mar. 20, 2021
 *  version Dec. 25, 2025
 * @version Jan. 17, 2026
 * @author  ASAMI, Tomoharu
 */
case class HttpRequest(
  path: HttpPath,
  method: HttpRequest.Method,
  query: Record,
  form: Record,
  header: Record,
  body: Option[org.goldenport.bag.Bag] = None,
  context: HttpContext = HttpContext.empty,
  @deprecated("Use path/context", "2026-01-08")
  url: Option[URL] = None
) {
  import HttpRequest._

  def isGet = method == GET
  def isMutation = !isGet
  def urlStringWithQuery: String = {
    val base = context.originalUri
      .orElse(url.map(_.toExternalForm))
      .getOrElse(path.asString)
    StringUtils.addUrlParams(base, query.asNameStringVector)
  }
  def pathParts: Vector[String] = path.segments
  // Router must depend only on path, not on context.
  // lazy val pathName = PathName(pathname)
  def show = s"Request(${path.asString})"
}

case class HttpPath(
  segments: Vector[String]
) {
  def asString: String =
    if (segments.isEmpty) "/" else segments.mkString("/", "/", "")
}

object HttpPath {
  def parse(path: String): HttpPath =
    HttpPath(path.split("/").toVector.filter(_.nonEmpty))
}

case class HttpContext(
  scheme: Option[String],
  authority: Option[String],
  originalUri: Option[String]
)

object HttpContext {
  val empty: HttpContext = HttpContext(None, None, None)
}

object HttpRequest {
  import org.goldenport.Consequence
  import org.goldenport.model.value.BaseContent
  import org.goldenport.protocol.operation.OperationRequest
  import org.goldenport.protocol.spec.{OperationDefinition, RequestDefinition, ResponseDefinition}
  import org.goldenport.protocol.{Argument, Property, Request}

  sealed trait Method extends NamedValueInstance {
  }
  object Method extends EnumerationClass[Method] {
    val elements = Vector(GET, POST, PUT, DELETE)
  }
  case object GET extends Method {
    val name = "GET"
  }
  case object POST extends Method {
    val name = "POST"
  }
  case object PUT extends Method {
    val name = "PUT"
  }
  case object DELETE extends Method {
    val name = "DELETE"
  }

  @deprecated("Use fromUrl or fromPath", "2026-01-08")
  def apply(url: URL): HttpRequest = fromUrl(GET, url)

  def fromUrl(
    method: Method,
    url: URL,
    query: Record = Record.empty,
    form: Record = Record.empty,
    header: Record = Record.empty,
    body: Option[Bag] = None
  ): HttpRequest =
    HttpRequest(
      path = HttpPath.parse(url.getPath),
      method = method,
      query = query,
      form = form,
      header = header,
      body = body,
      context = HttpContext(
        scheme = Option(url.getProtocol),
        authority = Option(url.getAuthority),
        originalUri = Option(url.toExternalForm)
      ),
      url = Some(url)
    )

  def fromPath(
    method: Method,
    path: String,
    query: Record = Record.empty,
    header: Record = Record.empty,
    body: Option[Bag] = None,
    context: HttpContext = HttpContext.empty,
    form: Record = Record.empty
  ): HttpRequest =
    HttpRequest(
      path = HttpPath.parse(path),
      method = method,
      query = query,
      form = form,
      header = header,
      body = body,
      context = context,
      url = None
    )

  @deprecated("Use fromPath and HttpContext", "2026-01-08")
  def create(baseurl: String, path: String): HttpRequest = {
    val s = StringUtils.concatPath(baseurl, path)
    fromUrl(GET, new URL(s))
  }

  def parseQuery(p: String): Record = {
    val a = if (p.startsWith("?")) p.substring(1) else p
    val bs = Strings.totokens(a, "&")
    val xs = bs.map(x => StringUtils.tokeyvalue(x, "=")).map {
      case (k, v) => k -> URLDecoder.decode(v, "UTF-8")
    }
    Record.create(xs)
  }

  def fromCurlLike(args: Seq[String]): Consequence[HttpRequest] =
    _from_curl_like(_curl_operation_definition, args)

  def fromCurlLike(
    op: OperationDefinition,
    args: Seq[String]
  ): Consequence[HttpRequest] =
    _from_curl_like(op, args)

  private def _from_curl_like(
    op: OperationDefinition,
    args: Seq[String]
  ): Consequence[HttpRequest] = {
    val argv = Array("curl") ++ args.toArray
    for {
      req  <- Request.parseArgs(op.specification.request, argv)
      opreq <- op.createOperationRequest(req)
      http <- _to_http_request(opreq)
    } yield http
  }

  private def _to_http_request(
    opreq: OperationRequest
  ): Consequence[HttpRequest] =
    _to_http_request(opreq.request.arguments, opreq.request.properties)

  private def _to_http_request(
    arguments: List[Argument],
    properties: List[Property]
  ): Consequence[HttpRequest] = {
    val urlstring = arguments.lastOption.map(_.value.toString)
    for {
      path <- Consequence.fromOption(urlstring, "No path specified")
    } yield {
      val method = _method(properties)
      val header = _headers(properties)
      val body = _body(properties)
      if (_is_absolute_url(path)) {
        val url = new URL(path)
        val query = Option(url.getQuery).map(parseQuery).getOrElse(Record.empty)
        fromUrl(method, url, query = query, header = header, body = body)
      } else {
        fromPath(method, path, header = header, body = body)
      }
    }
  }

  private def _method(properties: List[Property]): HttpRequest.Method = {
    val name = _property_value(properties, "method")
    name.flatMap { value =>
      Method.elements.find(_.name.equalsIgnoreCase(value))
    }.getOrElse(GET)
  }

  private def _headers(properties: List[Property]): Record = {
    val headers = _property_values(properties, "header").map { value =>
      val (k, v) = StringUtils.tokeyvalue(value, ":")
      k.trim -> v.trim
    }
    if (headers.isEmpty) Record.empty
    else Record.create(headers)
  }

  private def _body(properties: List[Property]): Option[Bag] =
    _property_value(properties, "data").map(Bag.text(_))

  private def _property_value(
    properties: List[Property],
    name: String
  ): Option[String] =
    properties.find(_.name == name).map(_.value.toString)

  private def _property_values(
    properties: List[Property],
    name: String
  ): Vector[String] =
    properties.collect { case p if p.name == name => p.value.toString }.toVector

  private val _curl_operation_definition: OperationDefinition =
    OperationDefinition(
      content = BaseContent.simple("curl"),
      request = RequestDefinition.curlLike,
      response = ResponseDefinition()
    )

  private def _is_absolute_url(p: String): Boolean =
    p.startsWith("http://") || p.startsWith("https://")
}
