package org.goldenport.cli

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.GivenWhenThen

import org.goldenport.protocol.operation.OperationRequest
import org.goldenport.protocol.spec._
import org.goldenport.Consequence
import cats.data.NonEmptyVector

/**
 * CliEngineSpec
 *
 * Executable / Working Specification for CliEngine.
 *
 * This specification describes how an application wires
 * ServiceDefinition and OperationDefinition into CliEngine,
 * and how a CLI request is translated into an OperationRequest.
 */
/*
 * @since   Dec. 26, 2025
 * @version Dec. 26, 2025
 * @author  ASAMI, Tomoharu
 */
final class CliEngineSpec
  extends AnyWordSpec
    with Matchers
    with GivenWhenThen {

  //
  // Test fixtures
  //
  case class Query(query: String) extends OperationRequest

  object QueryOperation extends OperationDefinition {
    override val specification: OperationDefinition.Specification =
      OperationDefinition.Specification(
        name = "query",
        request = RequestDefinition(
          parameters = List(
            ParameterDefinition(
              name = "query",
              kind = ParameterDefinition.Kind.Property
            )
          )
        ),
        response = ResponseDefinition(result = Nil)
      )

    override def createOperationRequest(
      req: org.goldenport.protocol.Request
    ): Consequence[OperationRequest] = {
      given org.goldenport.protocol.Request = req
      take_string("query").map(Query(_))
    }
  }

  val testService =
    ServiceDefinition(
      name = "search",
      operations = OperationDefinitionGroup(
        NonEmptyVector.of(QueryOperation)
      )
    )

  val services =
    ServiceDefinitionGroup(
      services = Vector(testService)
    )

  val engine =
    new CliEngine(
      CliEngine.Config(),
      CliEngine.Specification(services)
    )

  //
  // Specification
  //
  "CliEngine.makeRequest" should {

    "produce an OperationRequest when service and operation exist" in {
      Given("a CLI engine with a registered service and operation")

      When("makeRequest is called with valid arguments")
      val result =
        engine.makeRequest(
          args = Seq("query", "--query=domain model")
        )

      Then("it should succeed with a concrete OperationRequest")
      result match {
        case Consequence.Success(req) =>
          req.shouldBe(Query("domain model"))
        case _ =>
          fail("makeRequest should succeed for query")
      }
    }

    "fail when the service does not exist" in {
      Given("a CLI engine without the requested service")

      When("makeRequest is called with an unknown service")
      val result =
        engine.makeRequest(
          args = Seq("unknown")
        )

      Then("it should fail")
      result match {
        case _: Consequence.Failure[?] =>
          succeed
        case _ =>
          fail("makeRequest should fail for unknown service")
      }
    }

    "fail when the operation does not exist" in {
      Given("a CLI engine with a service but without the requested operation")

      When("makeRequest is called with an unknown operation")
      val result =
        engine.makeRequest(
          args = Seq("unknown")
        )

      Then("it should fail")
      result match {
        case _: Consequence.Failure[?] =>
          succeed
        case _ =>
          fail("makeRequest should fail for unknown operation")
      }
    }
  }
}
