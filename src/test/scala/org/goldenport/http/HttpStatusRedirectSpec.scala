package org.goldenport.http

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * Executable specification for redirect HTTP status support.
 *
 * @since   Apr. 26, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class HttpStatusRedirectSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks {
  private val _redirection_code_gen = Gen.choose(300, 399)
  private val _safe_utf8_character_gen =
    Gen.frequency(
      8 -> Gen.alphaNumChar,
      1 -> Gen.const('\n'),
      1 -> Gen.const('é'),
      1 -> Gen.const('日')
    )
  private val _body_gen =
    Gen.choose(0, 64).flatMap(size =>
      Gen.listOfN(size, _safe_utf8_character_gen).map(_.mkString)
    )

  "HttpStatus" should {
    "resolve redirect statuses used by Blob access" in {
      Given("the See Other and Temporary Redirect codes used by Blob access")
      val codes = Vector(303, 307)

      When("mapping the redirect codes from integers")
      val statuses = codes.map(HttpStatus.fromInt)

      Then("See Other and Temporary Redirect are first-class statuses")
      statuses shouldBe Vector(Some(HttpStatus.SeeOther), Some(HttpStatus.TemporaryRedirect))
    }

    "retain established named HTTP status identities" in {
      Given("the complete established named status mappings")
      val mappings: Vector[(Int, HttpStatus)] = Vector(
        200 -> HttpStatus.Ok,
        201 -> HttpStatus.Created,
        303 -> HttpStatus.SeeOther,
        307 -> HttpStatus.TemporaryRedirect,
        400 -> HttpStatus.BadRequest,
        401 -> HttpStatus.Unauthorized,
        403 -> HttpStatus.Forbidden,
        404 -> HttpStatus.NotFound,
        500 -> HttpStatus.InternalServerError,
        503 -> HttpStatus.ServiceUnavailable
      )

      When("selecting each named status through the public integer mapping")
      val selections = mappings.map { case (code, namedstatus) =>
        (HttpStatus.fromInt(code), namedstatus)
      }

      Then("every selection retains its established singleton identity")
      selections.foreach { case (selected, namedstatus) =>
        selected shouldBe Some(namedstatus)
        selected.map(_ eq namedstatus) shouldBe Some(true)
      }
    }

    "represent bounded redirection status codes without changing their numbers" in {
      Given("the explicit redirection boundaries, the original 302, and bounded generated redirection codes")
      val fixedcodes = Vector(300, 302, 399)

      forAll(_redirection_code_gen) { generatedcode =>
        Given("the fixed probes and a generated code within 300 through 399")
        val codes = fixedcodes :+ generatedcode

        When("selecting each redirection status twice through the public integer mapping")
        val selections = codes.map { code =>
          (code, HttpStatus.fromInt(code), HttpStatus.fromInt(code))
        }

        Then("both selections exist, preserve the numeric code, and have value equality")
        selections.foreach { case (code, first, second) =>
          first.isDefined shouldBe true
          second.isDefined shouldBe true
          first.map(_.code) shouldBe Some(code)
          second.map(_.code) shouldBe Some(code)
          second shouldBe first
        }
      }
    }

    "return an original 302 response with its payload and Location" in {
      Given("an original 302 response with a UTF-8 payload and Location header")
      val body = "redirect: café 日本"
      val location = "https://example.invalid/next"
      val headers = Map(
        "Content-Type" -> IndexedSeq("text/plain; charset=utf-8"),
        "Location" -> IndexedSeq(location)
      )
      val input = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))

      try {
        When("the public HTTP response parser reads the original 302 response")
        val response = HttpResponse.parser(302, headers, input)

        Then("the original status, payload, and Location survive and success remains false")
        response.code shouldBe 302
        response.getString shouldBe Some(body)
        response.headerValue("Location") shouldBe Some(location)
        response.isSuccess shouldBe false
      } finally {
        input.close()
      }
    }

    "preserve generated 3xx payload and metadata" in {
      Given("redirection codes within 300 through 399 and UTF-8 payloads of up to 64 characters")

      forAll(_redirection_code_gen, _body_gen) { (code, body) =>
        Given("a generated redirection response with text content and a Location header")
        val location = "https://example.invalid/generated"
        val headers = Map(
          "Content-Type" -> IndexedSeq("text/plain; charset=utf-8"),
          "Location" -> IndexedSeq(location)
        )
        val input = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))

        try {
          When("the public HTTP response parser reads the generated redirection response")
          val response = HttpResponse.parser(code, headers, input)

          Then("the original numeric code, UTF-8 payload, and response metadata survive")
          response.code shouldBe code
          response.getString shouldBe Some(body)
          response.headerValue("Location") shouldBe Some(location)
          response.headerValue("Content-Type") shouldBe Some("text/plain; charset=utf-8")
        } finally {
          input.close()
        }
      }
    }

    "retain unsupported nonredirection status fallback" in {
      Given("unsupported nonredirection probes and a representative UTF-8 response with metadata")
      val codes = Vector(299, 418, 599, 600, 999)
      val body = "unsupported: café 日本"
      val location = "https://example.invalid/unsupported"
      val headers = Map(
        "Content-Type" -> IndexedSeq("text/plain; charset=utf-8"),
        "Location" -> IndexedSeq(location)
      )
      val input = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))

      try {
        When("mapping the probes and parsing the representative unsupported response")
        val selections = codes.map(HttpStatus.fromInt)
        val response = HttpResponse.parser(299, headers, input)

        Then("unsupported selections remain absent and the established 500 fallback preserves payload and headers")
        selections.foreach { selected =>
          selected shouldBe None
        }
        response.status shouldBe HttpStatus.InternalServerError
        response.code shouldBe 500
        response.getString shouldBe Some(body)
        response.headerValue("Location") shouldBe Some(location)
        response.headerValue("Content-Type") shouldBe Some("text/plain; charset=utf-8")
      } finally {
        input.close()
      }
    }
  }
}
