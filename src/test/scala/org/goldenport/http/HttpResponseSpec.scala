package org.goldenport.http

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import org.goldenport.bag.Bag
import org.goldenport.datatype.{ContentType, MimeType}
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Apr. 25, 2026
 * @version Apr. 25, 2026
 * @author  ASAMI, Tomoharu
 */
final class HttpResponseSpec
  extends AnyWordSpec
  with GivenWhenThen
  with Matchers {

  "HttpResponse headers" should {
    "default to empty while preserving the existing Text constructor" in {
      Given("a text response built with the existing three-argument constructor")
      val response = HttpResponse.Text(
        HttpStatus.Ok,
        ContentType(MimeType("text/plain"), Some(StandardCharsets.UTF_8)),
        Bag.text("ok", StandardCharsets.UTF_8)
      )

      Then("the response has no headers by default")
      response.header shouldBe Record.empty
      response.headerValue("X-Textus-Job-Id") shouldBe None
    }

    "attach headers without changing response body or status" in {
      Given("a response and a response header record")
      val response = HttpResponse.text(HttpStatus.Ok, "ok")
      val header = Record.data("X-Textus-Job-Id" -> "job-1")

      When("the header is attached")
      val actual = response.withHeader(header)

      Then("status content and body are preserved")
      actual.status shouldBe response.status
      actual.contentType shouldBe response.contentType
      actual.getString shouldBe Some("ok")
      actual.show shouldBe response.show

      And("headers are exposed case-insensitively")
      actual.header shouldBe header
      actual.headerValue("x-textus-job-id") shouldBe Some("job-1")
    }

    "preserve parser response headers" in {
      Given("a parsed HTTP response with response headers")
      val body = new ByteArrayInputStream("ok".getBytes(StandardCharsets.UTF_8))
      val headers = Map(
        "Content-Type" -> IndexedSeq("text/plain; charset=utf-8"),
        "X-Textus-Job-Id" -> IndexedSeq("job-2")
      )

      When("the core parser builds the response")
      val response = HttpResponse.parser(200, headers, body)

      Then("the body and response headers are both available")
      response.getString shouldBe Some("ok")
      response.headerValue("x-textus-job-id") shouldBe Some("job-2")
      response.headerValue("content-type") shouldBe Some("text/plain; charset=utf-8")
    }

    "parse content type case-insensitively" in {
      Given("a parsed HTTP response with lowercase content-type")
      val body = new ByteArrayInputStream("ok".getBytes(StandardCharsets.UTF_8))
      val headers = Map(
        "content-type" -> IndexedSeq("text/plain; charset=utf-8")
      )

      When("the core parser builds the response")
      val response = HttpResponse.parser(200, headers, body)

      Then("the body is decoded as text")
      response.getString shouldBe Some("ok")
      response.headerValue("Content-Type") shouldBe Some("text/plain; charset=utf-8")
    }
  }
}
