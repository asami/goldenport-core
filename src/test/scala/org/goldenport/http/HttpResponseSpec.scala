package org.goldenport.http

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import org.goldenport.bag.Bag
import org.goldenport.datatype.{ContentType, MimeType}
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalacheck.Gen

/*
 * @since   Apr. 25, 2026
 * @version Sep. 29, 2026
 * @author  ASAMI, Tomoharu
 */
final class HttpResponseSpec
  extends AnyWordSpec
  with GivenWhenThen
  with Matchers
  with ScalaCheckDrivenPropertyChecks {

  private val _safe_utf8_character_gen =
    Gen.frequency(
      8 -> Gen.alphaNumChar,
      1 -> Gen.const('\n'),
      1 -> Gen.const('é'),
      1 -> Gen.const('日')
    )
  private val _body_gen =
    Gen.choose(0, 128).flatMap(size =>
      Gen.listOfN(size, _safe_utf8_character_gen).map(_.mkString)
    )
  private val _header_reference_gen =
    Gen.choose(1, 64).flatMap(size =>
      Gen.listOfN(size, Gen.alphaNumChar).map(_.mkString)
    )
  private val _unsupported_status_code_gen = Gen.choose(600, 999)

  "HttpResponse headers" should {
    "default to empty while preserving the existing Text constructor" in {
      Given("a status, UTF-8 content type, and text body")
      val status = HttpStatus.Ok
      val contenttype = ContentType(MimeType("text/plain"), Some(StandardCharsets.UTF_8))
      val body = Bag.text("ok", StandardCharsets.UTF_8)

      When("the existing three-argument Text constructor is used")
      val response = HttpResponse.Text(
        status,
        contenttype,
        body
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

  "HttpStatus service unavailable" should {
    "preserve generated text responses and attached headers" in {
      Given("bounded UTF-8 payloads and nonempty header references")

      forAll(_body_gen, _header_reference_gen) { (body, headerreference) =>
        Given("a service-unavailable text response and response header")
        val header = Record.data(headerreference -> "available")

        When("the 503 status is selected and the response is created with the header")
        val selectedstatus = HttpStatus.fromInt(503)
        val response = HttpResponse.text(
          selectedstatus.getOrElse(HttpStatus.InternalServerError),
          body
        ).withHeader(header)

        Then("the selected 503 status, payload, UTF-8 content type, and header are preserved")
        selectedstatus shouldBe Some(HttpStatus.ServiceUnavailable)
        response.code shouldBe 503
        response.getString shouldBe Some(body)
        response.contentType shouldBe ContentType.TEXT_PLAIN_UTF8
        response.headerValue(headerreference.toUpperCase(java.util.Locale.ROOT)) shouldBe Some("available")
      }
    }

    "parse generated upstream 503 text responses with case-insensitive metadata" in {
      Given("bounded UTF-8 payloads and nonempty header references")

      forAll(_body_gen, _header_reference_gen) { (body, headerreference) =>
        Given("an upstream service-unavailable response with text content and a header")
        val headers = Map(
          "cOnTeNt-TyPe" -> IndexedSeq("text/plain; charset=utf-8"),
          headerreference -> IndexedSeq("available")
        )
        val input = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))

        When("the public HTTP response parser reads the response")
        val response = HttpResponse.parser(503, headers, input)

        Then("the 503 code, payload, UTF-8 content type, and metadata are preserved")
        response.code shouldBe 503
        response.getString shouldBe Some(body)
        response.contentType shouldBe ContentType.TEXT_PLAIN_UTF8
        response.headerValue("CONTENT-TYPE") shouldBe Some("text/plain; charset=utf-8")
        response.headerValue(headerreference.toUpperCase(java.util.Locale.ROOT)) shouldBe Some("available")
      }
    }

    "retain the established parser fallback for unsupported status codes" in {
      Given("unsupported status codes, bounded UTF-8 payloads, and nonempty header references")

      forAll(_unsupported_status_code_gen, _body_gen, _header_reference_gen) {
        (statuscode, body, headerreference) =>
          Given("an unsupported upstream text response with metadata")
          val headers = Map(
            "Content-Type" -> IndexedSeq("text/plain; charset=utf-8"),
            headerreference -> IndexedSeq("available")
          )
          val input = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))

          When("the status is selected and the public HTTP response parser reads the response")
          val status = HttpStatus.fromInt(statuscode)
          val response = HttpResponse.parser(statuscode, headers, input)

          Then("the unsupported selection uses the established 500 fallback and preserves body and metadata")
          status shouldBe None
          response.status shouldBe HttpStatus.InternalServerError
          response.code shouldBe 500
          response.getString shouldBe Some(body)
          response.contentType shouldBe ContentType.TEXT_PLAIN_UTF8
          response.headerValue("content-type") shouldBe Some("text/plain; charset=utf-8")
          response.headerValue(headerreference.toUpperCase(java.util.Locale.ROOT)) shouldBe Some("available")
      }
    }
  }
}
