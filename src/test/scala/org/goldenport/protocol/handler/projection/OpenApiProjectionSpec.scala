package org.goldenport.protocol.handler.projection

import cats.data.NonEmptyVector
import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.protocol.handler.projection.OpenApiProjection
import org.goldenport.protocol.spec.{OperationDefinition, OperationDefinitionGroup, ParameterDefinition, RequestDefinition, ResponseDefinition, ServiceDefinition, ServiceDefinitionGroup}
import org.goldenport.schema.{Constraint, Multiplicity, ValueDomain, XString}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Dec. 30, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
class OpenApiProjectionSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen {

  "OpenApiProjection" should {
    "render a minimal OpenAPI document from definitions" in {
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

      When("projecting OpenAPI")
      val result = new OpenApiProjection().project(defs)

      Then("the JSON contains core OpenAPI fields")
      result match {
        case Consequence.Success(json) =>
          json.hcursor.get[String]("openapi").toOption.shouldBe(Some("3.0.0"))
          json.hcursor.downField("paths").downField("/search/query").focus.isDefined.shouldBe(true)
          json.hcursor.downField("paths").downField("/search/query").downField("post").downField("parameters").focus.isDefined.shouldBe(true)
          json.hcursor.downField("paths").downField("/search/query").downField("post").downField("requestBody").focus.isDefined.shouldBe(true)
        case _ =>
          fail("expected Success for OpenAPI projection")
      }
    }

    "render default and constraint metadata in schemas" in {
      Given("parameters with default values and constraints")
      val operation = OperationDefinition(
        name = "query",
        request = RequestDefinition(
          parameters = List(
            ParameterDefinition(
              name = "p1",
              kind = ParameterDefinition.Kind.Argument,
              domain = ValueDomain(datatype = XString, multiplicity = Multiplicity.One),
              default = ParameterDefinition.Default.Value(5)
            ),
            ParameterDefinition(
              name = "p2",
              kind = ParameterDefinition.Kind.Property,
              domain = ValueDomain(datatype = XString, multiplicity = Multiplicity.ZeroOne),
              default = ParameterDefinition.Default.Empty
            ),
            ParameterDefinition(
              name = "p3",
              kind = ParameterDefinition.Kind.Property,
              domain = ValueDomain(
                datatype = XString,
                multiplicity = Multiplicity.ZeroOne,
                constraints = Vector(Constraint.Enum(Set("opaque")))
              ),
              default = ParameterDefinition.Default.Undefined
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

      When("projecting OpenAPI")
      val result = new OpenApiProjection().project(defs)

      Then("schemas include default and constraint metadata")
      result match {
        case Consequence.Success(json) =>
          val argSchema =
            json.hcursor
              .downField("paths")
              .downField("/search/query")
              .downField("post")
              .downField("requestBody")
              .downField("content")
              .downField("application/json")
              .downField("schema")
              .downField("properties")
              .downField("p1")
          argSchema.get[Int]("default").toOption.shouldBe(Some(5))

          val paramSchema =
            json.hcursor
              .downField("paths")
              .downField("/search/query")
              .downField("post")
              .downField("parameters")
              .downArray
              .downField("schema")

          paramSchema.get[String]("x-default-kind").toOption.shouldBe(Some("empty"))
          paramSchema.get[String]("default").toOption.shouldBe(None)

          val constraintsSchema =
            json.hcursor
              .downField("paths")
              .downField("/search/query")
              .downField("post")
              .downField("parameters")
              .downArray
              .right
              .downField("schema")

          constraintsSchema.get[String]("x-constraints").toOption.shouldBe(Some("opaque"))
        case _ =>
          fail("expected Success for OpenAPI projection")
      }
    }
  }
}
