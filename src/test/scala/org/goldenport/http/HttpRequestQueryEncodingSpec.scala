package org.goldenport.http

import java.net.{URI, URLDecoder}
import java.nio.charset.StandardCharsets
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Oct. 13, 2026
 * @version Oct. 13, 2026
 */
final class HttpRequestQueryEncodingSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen {

  "HttpRequest query serialization" should {
    "preserve the reported Start JSON input in a valid URI" in {
      Given("the Start route and the original named JSON input")
      val input = """{"phaseIdentity":"phase-2","repositoryIdentity":"sm-workflow"}"""
      val base = "http://127.0.0.1/rest/v1/org-simplemodeling-textus-sm-workflow/goal-phase-service/start-goal-phase"
      val request = HttpRequest.fromUrl(
        HttpRequest.POST,
        URI.create(base).toURL,
        query = Record.data("input" -> input)
      )

      When("the request query is serialized and interpreted as a URI")
      val uri = URI.create(request.urlStringWithQuery)

      Then("the receiver recovers the exact original JSON")
      _decode_query(uri) shouldBe Vector("input" -> input)
      uri.getPath shouldBe URI.create(base).getPath
    }

    "round-trip UTF-8 and reserved characters in names and values" in {
      Given("raw query names and values containing Japanese and every reserved input character")
      val pairs = Vector(
        "名前 空白+%&=#?" -> "日本語 空白+%&=#?",
        "literal-percent" -> "%20+%2B"
      )
      val request = HttpRequest.fromPath(
        HttpRequest.GET,
        "/query",
        query = Record.create(pairs)
      )

      When("the request query is serialized and interpreted as a URI")
      val uri = URI.create(request.urlStringWithQuery)

      Then("standard URL decoding restores both names and values exactly once")
      _decode_query(uri) shouldBe pairs
      Option(uri.getFragment) shouldBe None
    }

    "keep the established spelling of plain ASCII parameters" in {
      Given("plain ASCII names and values")
      val request = HttpRequest.fromPath(
        HttpRequest.GET,
        "/query",
        query = Record.data("name" -> "alpha", "title" -> "Alpha-2._*")
      )

      When("the request query is serialized")
      val actual = request.urlStringWithQuery

      Then("the existing URL spelling is preserved")
      actual shouldBe "/query?name=alpha&title=Alpha-2._*"
    }

    "keep absolute and relative base URIs unchanged for an empty query" in {
      Given("requests with empty query records")
      val base = "http://127.0.0.1/query"
      val absolute = HttpRequest.fromUrl(HttpRequest.GET, URI.create(base).toURL)
      val relative = HttpRequest.fromPath(HttpRequest.GET, "/query")

      When("the requests are serialized")
      val actual = Vector(absolute.urlStringWithQuery, relative.urlStringWithQuery)

      Then("no query delimiter or parameter is added")
      actual shouldBe Vector(base, "/query")
    }
  }

  private def _decode_query(uri: URI): Vector[(String, String)] =
    uri.getRawQuery.split("&").toVector.map { pair =>
      val parts = pair.split("=", 2)
      URLDecoder.decode(parts(0), StandardCharsets.UTF_8) ->
        URLDecoder.decode(parts(1), StandardCharsets.UTF_8)
    }
}
