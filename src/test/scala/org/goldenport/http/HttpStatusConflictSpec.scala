package org.goldenport.http

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * Executable specification for the standard HTTP Conflict status.
 *
 * @since   Oct. 21, 2026
 * @version Oct. 21, 2026
 * @author  ASAMI, Tomoharu
 */
final class HttpStatusConflictSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "E1 HTTP Conflict status" should afterWord("in spec:http-status-conflict, example:E1, phase:12") {
    "resolve the named status through the ordinary integer mapping" in {
      Given("the standard HTTP Conflict code 409")
      val code = 409
      When("the public status mapping selects the code")
      val selected = HttpStatus.fromInt(code)
      Then("the selected status is the named Conflict singleton and preserves 409")
      selected shouldBe Some(HttpStatus.Conflict)
      selected.map(_.code) shouldBe Some(409)
      selected.map(_ eq HttpStatus.Conflict) shouldBe Some(true)
    }
  }

  "E2 HTTP Conflict response" should afterWord("in spec:http-status-conflict, example:E2, phase:12") {
    "preserve the original conflict code, UTF-8 payload and response headers" in {
      Given("an HTTP 409 response with a UTF-8 payload and application metadata")
      val body = "conflict: café 日本"
      val input = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))
      val headers = Map(
        "Content-Type" -> IndexedSeq("text/plain; charset=utf-8"),
        "X-Result" -> IndexedSeq("stale-revision")
      )
      try {
        When("the ordinary HTTP response parser reads the response")
        val response = HttpResponse.parser(409, headers, input)
        Then("Conflict remains 409 without losing the payload or headers")
        response.status shouldBe HttpStatus.Conflict
        response.code shouldBe 409
        response.getString shouldBe Some(body)
        response.headerValue("X-Result") shouldBe Some("stale-revision")
        response.headerValue("Content-Type") shouldBe Some("text/plain; charset=utf-8")
        response.isSuccess shouldBe false
      } finally {
        input.close()
      }
    }
  }
}
