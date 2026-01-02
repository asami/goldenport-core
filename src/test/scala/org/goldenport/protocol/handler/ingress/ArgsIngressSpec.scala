package org.goldenport.protocol.handler.ingress

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers

import org.goldenport.protocol.{Argument, Property, Switch}
import org.goldenport.protocol.spec.{OperationDefinition, OperationDefinitionGroup, ParameterDefinition, RequestDefinition, ResponseDefinition, ServiceDefinition, ServiceDefinitionGroup}
import org.goldenport.model.value.BaseContent
import cats.data.NonEmptyVector

/*
 * Executable Spec for ArgsIngress
 *
 * Purpose:
 * - Fix the canonical behavior of ArgsIngress
 * - args:Array[String] -> Request
 * - Service/operation identification is syntactic and uses ServiceDefinitionGroup
 */
/*
 * Canonical Parsing Contract (Normative)
 *
 * ArgsIngress performs syntactic canonicalization only.
 * Semantic resolution is explicitly out of scope.
 *
 * Supported invocation patterns:
 *
 * 1. operation arg1 arg2 ...
 *    - No explicit service name
 *    - operation is resolved by scanning ServiceDefinitionGroup
 *
 * 2. service operation arg1 arg2 ...
 *    - Explicit service name
 *    - operation is resolved within the specified service
 *
 * Service identification rules:
 * - If args(0) matches a service name in ServiceDefinitionGroup,
 *   it is treated as service and args(1) as operation.
 * - Otherwise, args(0) is treated as operation name and searched
 *   across all services.
 *
 * Argument classification rules:
 * - Switch / Property / Argument classification is performed here.
 * - OperationDefinition may be consulted ONLY to classify parameter kinds.
 * - Positional arguments are syntactically named as param1, param2, ...
 *
 * ArgsIngress MUST NOT:
 * - Perform semantic validation
 * - Interpret business meaning of arguments
 * - Resolve overloads beyond syntactic disambiguation
 */
/*
 * @since   Jan.  1, 2026
 * @version Jan.  2, 2026
 * @author  ASAMI, Tomoharu
 */
class ArgsIngressSpec
  extends AnyWordSpec
    with GivenWhenThen
    with Matchers {

  private val ingress = new DefaultArgsIngress()
  private val services = {
    val operation =
      OperationDefinition(
        name = "query",
        request = RequestDefinition(parameters = Nil),
        response = ResponseDefinition()
      )
    val service =
      ServiceDefinition(
        name = "test",
        operations = OperationDefinitionGroup(
          NonEmptyVector.of(operation)
        )
      )
    ServiceDefinitionGroup(Vector(service))
  }

  "ArgsIngress" should {

    "extract operation name from args" in {
      Given("CLI-like arguments with only an operation name")
      val args = Array("query")

      When("encoding args into Request")
      val result = ingress.encode(services, args)

      result match {
        case org.goldenport.Consequence.Success(req) =>
          Then("it succeeds and sets operation")
          req.operation shouldBe "query"
          req.arguments shouldBe Nil
          req.properties shouldBe Nil
          req.switches shouldBe Nil
        case org.goldenport.Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "extract service name and operation name from args when service is explicit" in {
      Given("CLI-like arguments with explicit service and operation")
      val args = Array("test", "query", "hello")

      When("encoding args into Request")
      val result = ingress.encode(services, args)

      result match {
        case org.goldenport.Consequence.Success(req) =>
          Then("it succeeds and sets service and operation")
          req.service shouldBe Some("test")
          req.operation shouldBe "query"
          req.arguments shouldBe List(
            Argument("param1", "hello", None)
          )
          req.switches shouldBe Nil
          req.properties shouldBe Nil
        case org.goldenport.Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "treat first arg as operation when it does not match a service" in {
      Given("CLI-like arguments without explicit service")
      val args = Array("query", "hello")

      When("encoding args into Request")
      val result = ingress.encode(services, args)

      result match {
        case org.goldenport.Consequence.Success(req) =>
          Then("it succeeds and treats first arg as operation")
          req.service shouldBe None
          req.operation shouldBe "query"
          req.arguments shouldBe List(
            Argument("param1", "hello", None)
          )
          req.switches shouldBe Nil
          req.properties shouldBe Nil
        case org.goldenport.Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "extract properties from --key value form" in {
      Given("CLI-like arguments with a property")
      val args = Array("query", "--text", "hello")

      When("encoding args into Request")
      val result = ingress.encode(services, args)

      result match {
        case org.goldenport.Consequence.Success(req) =>
          Then("it succeeds and produces a Property")
          req.operation shouldBe "query"
          req.arguments shouldBe Nil
          req.switches shouldBe Nil
          req.properties shouldBe List(
            Property("text", "hello", None)
          )
        case org.goldenport.Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "extract positional arguments as Argument values" in {
      Given("CLI-like arguments with positional parameters")
      val args = Array("query", "hello", "world")

      When("encoding args into Request")
      val result = ingress.encode(services, args)

      result match {
        case org.goldenport.Consequence.Success(req) =>
          Then("it succeeds and produces Argument values")
          req.operation shouldBe "query"
          req.arguments shouldBe List(
            Argument("param1", "hello", None),
            Argument("param2", "world", None)
          )
          req.switches shouldBe Nil
          req.properties shouldBe Nil
        case org.goldenport.Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "classify switches and properties using an OperationDefinition" in {
      Given("CLI-like arguments and an OperationDefinition with switch/property names")
      val args = Array("query", "--mode", "--verbose", "hello")
      val opdef =
        OperationDefinition(
          name = "query",
          request =
            RequestDefinition(
              parameters = List(
                ParameterDefinition(
                  content = BaseContent.simple("mode"),
                  kind = ParameterDefinition.Kind.Property
                ),
                ParameterDefinition(
                  content = BaseContent.simple("verbose"),
                  kind = ParameterDefinition.Kind.Switch
                )
              )
            ),
          response = ResponseDefinition()
        )

      When("encoding args into Request with OperationDefinition")
      val result = ingress.encode(opdef, args)

      result match {
        case org.goldenport.Consequence.Success(req) =>
          Then("it succeeds and uses definition names for switch/property classification")
          req.operation shouldBe "query"
          req.arguments shouldBe List(
            Argument("param1", "hello", None)
          )
          req.switches shouldBe List(
            Switch("verbose", true, None)
          )
          req.properties shouldBe List(
            Property("mode", "", None)
          )
        case org.goldenport.Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "treat --name value as Argument when ParameterDefinition kind is Argument" in {
      Given("CLI-like arguments with a named argument using --name value form")
      val args = Array("query", "--limit", "10", "hello")
      import org.goldenport.protocol.spec.{RequestDefinition, ResponseDefinition}

      val opdef =
        OperationDefinition(
          name = "query",
          request =
            RequestDefinition(
              parameters = List(
                ParameterDefinition(
                  content = BaseContent.simple("query"),
                  kind = ParameterDefinition.Kind.Argument
                ),
                ParameterDefinition(
                  content = BaseContent.simple("limit"),
                  kind = ParameterDefinition.Kind.Argument
                )
              )
            ),
          response = ResponseDefinition()
        )

      When("encoding args into Request with OperationDefinition")
      val result = ingress.encode(opdef, args)

      result match {
        case org.goldenport.Consequence.Success(req) =>
          Then("it succeeds and treats --limit as an Argument, not a Property")
          req.operation shouldBe "query"
          req.arguments shouldBe List(
            Argument("limit", "10", None),
            Argument("query", "hello", None)
          )
          req.switches shouldBe Nil
          req.properties shouldBe Nil
        case org.goldenport.Consequence.Failure(err) =>
          fail(err.toString)
      }
    }
  }
}
