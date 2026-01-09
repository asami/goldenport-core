package org.goldenport.protocol.handler.ingress

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers

import org.goldenport.Consequence
import org.goldenport.bag.Bag
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
    "encode GET with no query and no body as an empty Request" in {
      Given("an HttpRequest with GET method and no parameters")
      val http =
        HttpRequest.fromPath(
          method = HttpRequest.GET,
          path = "/any/service/op",
          query = Record.empty,
          header = Record.empty,
          body = None
        )

      val opdef =
        OperationDefinition(
          name = "op",
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
          Then("it succeeds and produces empty parameters")
          req.operation shouldBe "op"
          req.arguments shouldBe Nil
          req.switches shouldBe Nil
          req.properties shouldBe Nil

        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "encode GET query parameters as named Arguments" in {
      Given("an HttpRequest with GET method and query parameters")
      val http =
        HttpRequest.fromPath(
          method = HttpRequest.GET,
          path = "/query",
          query = Record.data("query" -> "target"),
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
        HttpRequest.fromPath(
          method = HttpRequest.POST,
          path = "/query",
          query = Record.empty,
          header = Record.empty,
          form = Record.data("name" -> "alice")
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

    "encode POST with body but no parameters as an empty Request" in {
      Given("an HttpRequest with POST method and body only")
      val http =
        HttpRequest.fromPath(
          method = HttpRequest.POST,
          path = "/query",
          query = Record.empty,
          header = Record.empty,
          body = Some(Bag.text("payload")),
          form = Record.empty
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
          Then("it succeeds and produces empty parameters")
          req.operation shouldBe "query"
          req.arguments shouldBe Nil
          req.switches shouldBe Nil
          req.properties shouldBe Nil

        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }
  }
}
