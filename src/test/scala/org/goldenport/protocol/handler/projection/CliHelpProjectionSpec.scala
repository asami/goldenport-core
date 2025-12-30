package org.goldenport.protocol.handler.projection

import org.goldenport.Consequence
import org.goldenport.protocol.handler.projection.CliHelpProjection
import org.goldenport.protocol.spec.{OperationDefinition, OperationDefinitionGroup, ParameterDefinition, RequestDefinition, ResponseDefinition, ServiceDefinition, ServiceDefinitionGroup}
import org.goldenport.schema.{Multiplicity, ValueDomain, XString}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import cats.data.NonEmptyVector

/*
 * @since   Dec. 30, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
class CliHelpProjectionSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks {

  "CliHelpProjection" should {
    "render service and operation help from definitions" in {
      Given("a service with one operation and parameters")
      val operation = OperationDefinition(
        name = "query",
        request = RequestDefinition(
          parameters = List(
            ParameterDefinition(
              name = "query",
              kind = ParameterDefinition.Kind.Argument,
              domain = ValueDomain(datatype = XString, multiplicity = Multiplicity.One)
            ),
            ParameterDefinition(
              name = "limit",
              kind = ParameterDefinition.Kind.Property,
              domain = ValueDomain(datatype = XString, multiplicity = Multiplicity.ZeroOne)
            ),
            ParameterDefinition(
              name = "verbose",
              kind = ParameterDefinition.Kind.Switch
            )
          )
        ),
        response = ResponseDefinition(Nil)
      )
      val service = ServiceDefinition(
        name = "search",
        operations = OperationDefinitionGroup(NonEmptyVector.of(operation))
      )
      val defs = ServiceDefinitionGroup(Vector(service))

      When("projecting CLI help")
      val result = new CliHelpProjection().project(defs)

      Then("the output contains the expected sections and note")
      result match {
        case Consequence.Success(out) =>
          out.contains("search").shouldBe(true)
          out.contains("query").shouldBe(true)
          out.contains("Arguments").shouldBe(true)
          out.contains("Options").shouldBe(true)
          out.contains("Arguments can be specified by position or by name (--arg).").shouldBe(true)
        case _ =>
          fail("expected Success for CLI help projection")
      }
    }
  }
}
