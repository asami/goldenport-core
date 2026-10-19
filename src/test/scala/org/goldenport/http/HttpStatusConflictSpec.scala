package org.goldenport.http

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import org.goldenport.Consequence
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * Executable specification for conflict HTTP status transport.
 *
 * @since   Oct. 19, 2026
 * @version Oct. 19, 2026
 */
final class HttpStatusConflictSpec extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks {

  private val _utf8_character_gen = Gen.frequency(
    8 -> Gen.alphaNumChar,
    1 -> Gen.const('\n'),
    1 -> Gen.const('é'),
    1 -> Gen.const('日')
  )
  private val _body_gen = Gen.choose(0, 64).flatMap(size =>
    Gen.listOfN(size, _utf8_character_gen).map(_.mkString)
  )

  "HTTP conflict status" should {
    "represent conflict responses" which {
      "resolves 409 to the first-class Conflict singleton" in {
        Given("the HTTP Conflict numeric code")
        val code = 409

        When("the public integer mapping selects its HTTP status")
        val selected = HttpStatus.fromInt(code)

        Then("the selection is the Conflict singleton with code 409")
        selected shouldBe Some(HttpStatus.Conflict)
        selected.map(_ eq HttpStatus.Conflict) shouldBe Some(true)
        selected.map(_.code) shouldBe Some(409)
      }

      "carries a public state conflict into a text response" in {
        Given("a diagnostic body describing a conflicting state transition")
        val message = "The editing session has already advanced: café 日本"

        When("the public stateConflict helper is converted through the normal HTTP status bridge")
        val conclusion = Consequence.stateConflict[Unit](message) match {
          case Consequence.Failure(conclusion) => conclusion
          case _ => fail("A state conflict must return Consequence.Failure")
        }
        val selected = HttpStatus.fromInt(conclusion.status.webCode.code)
        val status = selected.getOrElse(HttpStatus.InternalServerError)
        val response = HttpResponse.text(status, message)

        Then("the materialized conflict remains HTTP 409 and preserves the diagnostic body")
        conclusion.status.webCode.code shouldBe 409
        selected shouldBe Some(HttpStatus.Conflict)
        response.status shouldBe HttpStatus.Conflict
        response.code shouldBe 409
        response.getString shouldBe Some(message)
        response.isSuccess shouldBe false
      }

      "preserves parsed conflict status, generated UTF-8 content, and headers" in {
        forAll(_body_gen) { generatedbody =>
          Given("an HTTP 409 response with varying UTF-8 content and conflict metadata")
          val body = s"conflict: café 日本 $generatedbody"
          val headers = Map(
            "Content-Type" -> IndexedSeq("text/plain; charset=utf-8"),
            "X-Conflict-Revision" -> IndexedSeq("revision-2")
          )
          val input = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))

          try {
            When("the public parser interprets the conflict response")
            val response = HttpResponse.parser(409, headers, input)

            Then("the Conflict status, UTF-8 body, and response headers survive")
            response.status shouldBe HttpStatus.Conflict
            response.code shouldBe 409
            response.getString shouldBe Some(body)
            response.headerValue("Content-Type") shouldBe Some("text/plain; charset=utf-8")
            response.headerValue("X-Conflict-Revision") shouldBe Some("revision-2")
            response.isSuccess shouldBe false
          } finally {
            input.close()
          }
        }
      }
    }

    "preserve established status selection" which {
      "retains every existing named status singleton" in {
        Given("the established success, redirect, client error, and server error mappings")
        val mappings: Vector[(Int, HttpStatus)] = Vector(
          200 -> HttpStatus.Ok,
          201 -> HttpStatus.Created,
          204 -> HttpStatus.NoContent,
          303 -> HttpStatus.SeeOther,
          307 -> HttpStatus.TemporaryRedirect,
          400 -> HttpStatus.BadRequest,
          401 -> HttpStatus.Unauthorized,
          403 -> HttpStatus.Forbidden,
          404 -> HttpStatus.NotFound,
          500 -> HttpStatus.InternalServerError,
          503 -> HttpStatus.ServiceUnavailable
        )

        When("the public integer mapping selects each established status")
        val selections = mappings.map { case (code, namedstatus) =>
          (HttpStatus.fromInt(code), namedstatus)
        }

        Then("every selection retains its existing singleton identity and code")
        selections.foreach { case (selected, namedstatus) =>
          selected shouldBe Some(namedstatus)
          selected.map(_ eq namedstatus) shouldBe Some(true)
          selected.map(_.code) shouldBe Some(namedstatus.code)
        }
      }

      "retains unsupported selection and parser fallback" in {
        Given("unsupported nonredirect codes and a representative response body with metadata")
        val codes = Vector(299, 418, 599, 600, 999)
        val body = "unsupported: café 日本"
        val headers = Map(
          "Content-Type" -> IndexedSeq("text/plain; charset=utf-8"),
          "X-Response-Id" -> IndexedSeq("unsupported-response")
        )
        val input = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))

        try {
          When("the public mapping selects the probes and the parser interprets status 299")
          val selections = codes.map(HttpStatus.fromInt)
          val response = HttpResponse.parser(299, headers, input)

          Then("unsupported selections remain absent and the parser retains its 500 fallback with content and headers")
          selections.foreach { selected =>
            selected shouldBe None
          }
          response.status shouldBe HttpStatus.InternalServerError
          response.code shouldBe 500
          response.getString shouldBe Some(body)
          response.headerValue("Content-Type") shouldBe Some("text/plain; charset=utf-8")
          response.headerValue("X-Response-Id") shouldBe Some("unsupported-response")
        } finally {
          input.close()
        }
      }
    }
  }
}
