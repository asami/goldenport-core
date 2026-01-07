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
 * @version Dec. 25, 2025
 * @author  ASAMI, Tomoharu
 */
case class HttpRequest(
  url: URL,
  method: HttpRequest.Method,
  query: Record,
  form: Record,
  header: Record,
  body: Option[org.goldenport.bag.Bag] = None
) {
  import HttpRequest._

  def isGet = method == GET
  def isMutation = !isGet
  def urlStringWithQuery = buildUrlStringWithQuery(url, query)
  def pathParts: Vector[String] =
    url.getPath.split("/").toVector.filter(_.nonEmpty)
  // lazy val pathName = PathName(pathname)
  def show = s"Request(${url})"
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

  def apply(url: URL): HttpRequest = HttpRequest(url, GET, Record.empty, Record.empty,Record.empty)

  def create(baseurl: String, path: String): HttpRequest = {
    val s = StringUtils.concatPath(baseurl, path)
    apply(new URL(s))
  }

  def parseQuery(p: String): Record = {
    val a = if (p.startsWith("?")) p.substring(1) else p
    val bs = Strings.totokens(a, "&")
    val xs = bs.map(x => StringUtils.tokeyvalue(x, "=")).map {
      case (k, v) => k -> URLDecoder.decode(v, "UTF-8")
    }
    Record.create(xs)
  }

  def buildUrlStringWithQuery(url: URL, p: Record): String =
    buildUrlStringWithQuery(url.toExternalForm, p)

  def buildUrlStringWithQuery(s: String, p: Record): String =
    StringUtils.addUrlParams(s, p.asNameStringVector)

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
      req <- Request.parseArgs(op.specification.request, argv)
      opreq <- op.createOperationRequest(req)
      http <- _to_http_request(opreq)
    } yield http
  }

  private def _to_http_request(
    opreq: OperationRequest
  ): Consequence[HttpRequest] =
    opreq match {
      case holder: OperationRequest.Core.Holder =>
        _to_http_request(holder.arguments, holder.properties)
      case _ =>
        Consequence.failure("unsupported operation request")
    }

  private def _to_http_request(
    arguments: List[Argument],
    properties: List[Property]
  ): Consequence[HttpRequest] = {
    val urlstring = arguments.lastOption.map(_.value.toString)
    for {
      path <- Consequence.fromOption(urlstring, "No path specified")
      url <- Consequence(new URL(path))
    } yield {
      val method = _method(properties)
      val header = _headers(properties)
      val query = Option(url.getQuery).map(parseQuery).getOrElse(Record.empty)
      val body = _body(properties)
      HttpRequest(url, method, query, Record.empty, header, body)
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
}
