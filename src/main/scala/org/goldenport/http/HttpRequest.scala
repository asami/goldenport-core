package org.goldenport.http

import java.net.URL
import java.net.URLDecoder
import org.goldenport.record.Record
import org.goldenport.util.StringUtils
import org.goldenport.util.Strings

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
  header: Record
) {
  import HttpRequest._

  def isGet = method == GET
  def isMutation = !isGet
  def urlStringWithQuery = buildUrlStringWithQuery(url, query)
  // lazy val pathName = PathName(pathname)
  def show = s"Request(${url})"
}

object HttpRequest {
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
}
