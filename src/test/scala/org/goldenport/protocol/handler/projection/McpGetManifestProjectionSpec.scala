package org.goldenport.protocol.handler.projection

import cats.data.NonEmptyVector
import org.goldenport.Consequence
import org.goldenport.protocol.handler.projection.McpGetManifestProjection
import org.goldenport.protocol.spec.{OperationDefinition, OperationDefinitionGroup, ParameterDefinition, RequestDefinition, ResponseDefinition, ServiceDefinition, ServiceDefinitionGroup}
import org.goldenport.schema.{Multiplicity, ValueDomain, XString}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Dec. 30, 2025
 * @version Apr. 11, 2026
 * @author  ASAMI, Tomoharu
 */
class McpGetManifestProjectionSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen {

  "McpGetManifestProjection" should {
    "render a basic manifest from definitions" in {
      Given("a service with one operation and one argument")
      val operation = OperationDefinition(
        content = org.goldenport.value.BaseContent.simple("query"),
        request = RequestDefinition(
          parameters = List(
            ParameterDefinition(
              content = org.goldenport.value.BaseContent.simple("query"),
              kind = ParameterDefinition.Kind.Argument,
              domain = ValueDomain(datatype = XString, multiplicity = Multiplicity.One)
            )
          )
        ),
        response = ResponseDefinition.void
      )
      val service = ServiceDefinition(
        name = "search",
        operations = OperationDefinitionGroup(NonEmptyVector.of(operation))
      )
      val defs = ServiceDefinitionGroup(Vector(service))

      When("projecting the manifest")
      val result = new McpGetManifestProjection().project(defs)

      Then("the manifest contains service, operation, and parameter data")
      result match {
        case Consequence.Success(json) =>
          json.hcursor.downField("service").get[String]("name").toOption.shouldBe(Some("search"))
          json.hcursor.downField("operations").downArray.get[String]("name").toOption.shouldBe(Some("query"))
          json.hcursor.downField("operations").downArray.downField("parameters").downArray.get[String]("kind").toOption
            .shouldBe(Some("argument"))
          json.hcursor.downField("operations").downArray.downField("parameters").downArray.get[String]("datatype").toOption
            .shouldBe(Some("string"))
        case _ =>
          fail("expected Success for MCP get_manifest projection")
      }
    }

    "render mixed parameter kinds with required and multiple flags" in {
      Given("a service with mixed parameter kinds")
      val operation = OperationDefinition(
        content = org.goldenport.value.BaseContent.simple("query"),
        request = RequestDefinition(
          parameters = List(
            ParameterDefinition(
              content = org.goldenport.value.BaseContent.simple("query"),
              kind = ParameterDefinition.Kind.Argument,
              domain = ValueDomain(datatype = XString, multiplicity = Multiplicity.One)
            ),
            ParameterDefinition(
              content = org.goldenport.value.BaseContent.simple("tag"),
              kind = ParameterDefinition.Kind.Property,
              domain = ValueDomain(datatype = XString, multiplicity = Multiplicity.ZeroMore)
            ),
            ParameterDefinition(
              content = org.goldenport.value.BaseContent.simple("verbose"),
              kind = ParameterDefinition.Kind.Switch
            )
          )
        ),
        response = ResponseDefinition.void
      )
      val service = ServiceDefinition(
        name = "search",
        operations = OperationDefinitionGroup(NonEmptyVector.of(operation))
      )
      val defs = ServiceDefinitionGroup(Vector(service))

      When("projecting the manifest")
      val result = new McpGetManifestProjection().project(defs)

      Then("parameter kinds and multiplicity flags are preserved")
      result match {
        case Consequence.Success(json) =>
          val params = json.hcursor.downField("operations").downArray.downField("parameters")
          params.downArray.get[String]("kind").toOption.shouldBe(Some("argument"))
          params.downArray.downField("required").as[Boolean].toOption.shouldBe(Some(true))
          params.downArray.downField("multiple").as[Boolean].toOption.shouldBe(Some(false))
        case _ =>
          fail("expected Success for MCP get_manifest projection")
      }
    }

    "omit optional description fields when absent" in {
      Given("a service with no description fields")
      val operation = OperationDefinition(
        content = org.goldenport.value.BaseContent.simple("query"),
        request = RequestDefinition(parameters = Nil),
        response = ResponseDefinition.void
      )
      val service = ServiceDefinition(
        name = "search",
        operations = OperationDefinitionGroup(NonEmptyVector.of(operation))
      )
      val defs = ServiceDefinitionGroup(Vector(service))

      When("projecting the manifest")
      val result = new McpGetManifestProjection().project(defs)

      Then("description fields are absent")
      result match {
        case Consequence.Success(json) =>
          json.hcursor.downField("service").downField("description").succeeded.shouldBe(false)
          json.hcursor.downField("operations").downArray.downField("description").succeeded.shouldBe(false)
        case _ =>
          fail("expected Success for MCP get_manifest projection")
      }
    }
  }
}
