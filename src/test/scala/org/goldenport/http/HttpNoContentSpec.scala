package org.goldenport.http

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.Locale
import org.goldenport.bag.Bag
import org.goldenport.datatype.ContentType
import org.goldenport.record.Record
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Oct. 16, 2026
 * @version Oct. 16, 2026
 */
final class HttpNoContentSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  private val _header_token_gen = Gen.choose(1, 32).flatMap(size =>
    Gen.listOfN(size, Gen.alphaNumChar).map(_.mkString)
  )
  private val _content_type_key_gen = Gen.oneOf("Content-Type", "content-type", "cOnTeNt-TyPe")

  "HTTP No Content" should {
    "select the core-owned singleton" which {
      "NC1: preserves the 204 code and singleton identity on every selection" in {
        Given("the HTTP No Content integer code")
        val code = 204

        When("the public integer mapping selects the status twice")
        val first = HttpStatus.fromInt(code)
        val second = HttpStatus.fromInt(code)

        Then("both selections are the NoContent singleton with numeric code 204")
        first shouldBe Some(HttpStatus.NoContent)
        second shouldBe first
        first.map(_ eq HttpStatus.NoContent) shouldBe Some(true)
        second.map(_ eq HttpStatus.NoContent) shouldBe Some(true)
        HttpStatus.NoContent.code shouldBe 204
      }
    }

    "preserve empty response forms and metadata" which {
      "NC2a: constructs empty text and binary responses through the existing public APIs" in {
        Given("NoContent and a case-insensitively queried response header")
        val header = Record.data("X-Empty-Response" -> "accepted")

        When("the existing text and binary constructors receive empty bodies and attached headers")
        val text = HttpResponse.text(HttpStatus.NoContent, "", StandardCharsets.UTF_8).withHeader(header)
        val binary = HttpResponse.Binary(
          HttpStatus.NoContent, ContentType.APPLICATION_OCTET_STREAM, Bag.binary(Array.empty[Byte])
        ).withHeader(header)
        val bodies = Vector(text, binary).map(_body_bytes)

        Then("status, content type, empty representation, and supplied metadata survive")
        Vector(text, binary).foreach { response =>
          response.status shouldBe HttpStatus.NoContent
          response.code shouldBe 204
          response.header shouldBe header
          response.headerValue("x-empty-response") shouldBe Some("accepted")
          response.isSuccess shouldBe false
        }
        bodies shouldBe Vector(Vector.empty[Byte], Vector.empty[Byte])
        text.contentType shouldBe ContentType.TEXT_PLAIN_UTF8
        text.getString shouldBe Some("")
        text.getBinary shouldBe None
        binary.contentType shouldBe ContentType.APPLICATION_OCTET_STREAM
        binary.getString shouldBe None
        binary.getBinary.isDefined shouldBe true
      }

      "NC2b: parses an empty text response with mixed-case Content-Type and header lookup" in {
        Given("an empty 204 response with mixed-case text metadata")
        val headers = Map(
          "cOnTeNt-TyPe" -> IndexedSeq("text/plain; charset=utf-8"),
          "X-Empty-Response" -> IndexedSeq("text")
        )

        When("the public parser reads the empty input stream")
        val response = _parse_empty(headers)
        val bytes = _body_bytes(response)

        Then("the text body remains empty and all metadata and the 204 identity survive")
        response.status shouldBe HttpStatus.NoContent
        response.code shouldBe 204
        response.contentType shouldBe ContentType.TEXT_PLAIN_UTF8
        response.getString shouldBe Some("")
        response.getBinary shouldBe None
        bytes shouldBe empty
        response.headerValue("CONTENT-TYPE") shouldBe Some("text/plain; charset=utf-8")
        response.headerValue("x-empty-response") shouldBe Some("text")
        response.isSuccess shouldBe false
      }

      "NC2c: parses an empty binary response with or without Content-Type" in {
        Given("empty 204 response metadata with an explicit binary type and with no type")
        val headers = Vector(
          Map("content-type" -> IndexedSeq("application/octet-stream"), "X-Empty-Response" -> IndexedSeq("binary")),
          Map("X-Empty-Response" -> IndexedSeq("binary"))
        )

        When("the public parser reads both empty binary input streams")
        val responses = headers.map(_parse_empty)
        val bodies = responses.map(_body_bytes)

        Then("both responses retain NoContent and the existing octet-stream binary default")
        responses.foreach { response =>
          response.status shouldBe HttpStatus.NoContent
          response.code shouldBe 204
          response.contentType shouldBe ContentType.APPLICATION_OCTET_STREAM
          response.getString shouldBe None
          response.getBinary.isDefined shouldBe true
          response.headerValue("x-empty-response") shouldBe Some("binary")
          response.isSuccess shouldBe false
        }
        bodies shouldBe Vector(Vector.empty[Byte], Vector.empty[Byte])
        responses.head.headerValue("Content-Type") shouldBe Some("application/octet-stream")
        responses.last.headerValue("Content-Type") shouldBe None
      }
    }

    "retain compatibility across bounded generated empty forms" which {
      "NC3a: preserves safe generated headers in empty text, binary, and untyped parser forms" in {
        Given("bounded nonempty ASCII header tokens, metadata casing, and all three empty forms")
        val property = Prop.forAll(_header_token_gen, _header_token_gen, _content_type_key_gen) {
          (suffix, value, typekey) =>
            Given("safe response metadata and one empty body for each supported representation")
            val headername = "X-Empty-" + suffix
            val header = Record.data(headername -> value)
            val forms = Vector(
              Some("text/plain; charset=utf-8"), Some("application/octet-stream"), None
            )
            val metadata = forms.map { contenttype =>
              Map(headername -> IndexedSeq(value)) ++ contenttype.map(text => typekey -> IndexedSeq(text))
            }

            When("each empty form is parsed and the existing empty constructors receive the generated header")
            val parsed = metadata.map(_parse_empty)
            val constructed = Vector(
              HttpResponse.text(HttpStatus.NoContent, "").withHeader(header),
              HttpResponse.Binary(
                HttpStatus.NoContent, ContentType.APPLICATION_OCTET_STREAM, Bag.binary(Array.empty[Byte])
              ).withHeader(header)
            )
            val bodies = (parsed ++ constructed).map(_body_bytes)

            Then("all empty bodies, numeric statuses, header values, and representation choices survive")
            (parsed ++ constructed).foreach { response =>
              response.status shouldBe HttpStatus.NoContent
              response.code shouldBe 204
              response.headerValue(headername.toUpperCase(Locale.ROOT)) shouldBe Some(value)
              response.isSuccess shouldBe false
            }
            bodies.foreach(_ shouldBe empty)
            parsed.head.getString shouldBe Some("")
            parsed.head.contentType shouldBe ContentType.TEXT_PLAIN_UTF8
            parsed.head.headerValue("CONTENT-TYPE") shouldBe forms.head
            parsed.tail.foreach { response =>
              response.getString shouldBe None
              response.getBinary.isDefined shouldBe true
              response.contentType shouldBe ContentType.APPLICATION_OCTET_STREAM
            }
            parsed(1).headerValue("Content-Type") shouldBe forms(1)
            parsed(2).headerValue("Content-Type") shouldBe None
            constructed.head.getString shouldBe Some("")
            constructed.last.getBinary.isDefined shouldBe true
            true
        }

        When("ScalaCheck evaluates sixty successful generated metadata cases without discards")
        val result = Test.check(Test.Parameters.default.withMinSuccessfulTests(60), property)

        Then("at least fifty generated cases satisfy all three parser forms and both constructors")
        result.passed shouldBe true
        result.succeeded should be >= 50
        result.discarded shouldBe 0
      }

      "NC3b: retains named status identities, non-204 unsupported codes, and Ok-only success" in {
        Given("established named successes and redirects, all redirection codes, and other unsupported probes")
        val named = Vector(
          200 -> HttpStatus.Ok, 201 -> HttpStatus.Created,
          303 -> HttpStatus.SeeOther, 307 -> HttpStatus.TemporaryRedirect
        )
        val unsupported = Vector(202, 203, 205, 206, 299, 418, 599, 600, 999)

        When("the public mapping selects those codes and existing responses expose their success predicate")
        val identities = named.map { case (code, status) => HttpStatus.fromInt(code).map(_ eq status) }
        val redirects = (300 to 399).map(code => HttpStatus.fromInt(code).map(_.code)).toVector
        val absent = unsupported.map(HttpStatus.fromInt)
        val successes = (named.map(_._2) :+ HttpStatus.NoContent).map(status =>
          HttpResponse.text(status, "").isSuccess
        )

        Then("existing singletons and all 3xx numbers remain, unsupported selections are absent, and only Ok succeeds")
        identities shouldBe Vector.fill(4)(Some(true))
        redirects shouldBe (300 to 399).map(Some(_)).toVector
        absent shouldBe Vector.fill(unsupported.size)(None)
        successes shouldBe Vector(true, false, false, false, false)
      }
    }
  }

  private def _parse_empty(headers: Map[String, IndexedSeq[String]]): HttpResponse = {
    val input = new ByteArrayInputStream(Array.empty[Byte])
    try HttpResponse.parser(204, headers, input)
    finally input.close()
  }

  private def _body_bytes(response: HttpResponse): Vector[Byte] = {
    val input = response.bag.openInputStream()
    try input.readAllBytes().toVector
    finally input.close()
  }
}
