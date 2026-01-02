package org.goldenport.protocol.handler.ingress

import java.net.URL

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers

import org.goldenport.Consequence
import org.goldenport.protocol.{Argument, Property}
import org.goldenport.protocol.Request
import org.goldenport.protocol.spec.{OperationDefinition, RequestDefinition, ResponseDefinition}
import org.goldenport.http.HttpRequest
import org.goldenport.record.Record

class RestIngressSpec
    extends AnyWordSpec
    with GivenWhenThen
    with Matchers {

  "RestIngress" should {

    "encode GET query parameters as named Arguments" in {
      Given("an HttpRequest with GET method and query parameters")
      val http =
        HttpRequest(
          url = new URL("http://example.com/query"),
          method = HttpRequest.GET,
          query = Record.data("query" -> "target"),
          form = Record.empty,
          header = Record.empty
        )

      val opdef =
        OperationDefinition(
          name = "query",
          request = RequestDefinition(parameters = Nil),
          response = ResponseDefinition()
        )

      val ingress =
        IngressCollection(
          ingresses = Vector(new RestIngress {})
        ).ingress(http).take

      When("encoding HttpRequest into Request")
      val result = ingress.encode(opdef, http)

      result match {
        case Consequence.Success(req) =>
          Then("it succeeds and produces a canonical Request")
          req.operation shouldBe "query"
          req.arguments shouldBe List(
            Argument("query", "target", None)
          )
          req.properties shouldBe Nil

        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "encode POST form parameters as named Arguments" in {
      Given("an HttpRequest with POST method and form parameters")
      val http =
        HttpRequest(
          url = new URL("http://example.com/query"),
          method = HttpRequest.POST,
          query = Record.empty,
          form = Record.data("name" -> "alice"),
          header = Record.empty
        )

      val opdef =
        OperationDefinition(
          name = "query",
          request = RequestDefinition(parameters = Nil),
          response = ResponseDefinition()
        )

      val ingress =
        IngressCollection(
          ingresses = Vector(new RestIngress {})
        ).ingress(http).take

      When("encoding HttpRequest into Request")
      val result = ingress.encode(opdef, http)

      result match {
        case Consequence.Success(req) =>
          Then("it succeeds and produces a canonical Request")
          req.operation shouldBe "query"
          req.arguments shouldBe List(
            Argument("name", "alice", None)
          )
          req.properties shouldBe Nil

        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }
  }
}
