package org.goldenport.protocol.handler.projection

import cats.data.NonEmptyVector
import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.protocol.handler.projection.OpenApiProjection
import org.goldenport.protocol.spec.{OperationDefinition, OperationDefinitionGroup, ParameterDefinition, RequestDefinition, ResponseDefinition, ServiceDefinition, ServiceDefinitionGroup}
import org.goldenport.schema.{Constraint, Multiplicity, ValueDomain, WebColumn, WebValidationHints, XBlob, XClob, XNonNegativeInteger, XString}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Dec. 30, 2025
 * @version Apr. 26, 2026
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
        content = org.goldenport.value.BaseContent.simple("query"),
        request = RequestDefinition(
          parameters = List(
            ParameterDefinition(
              content = org.goldenport.value.BaseContent.simple("query"),
              kind = ParameterDefinition.Kind.Argument,
              domain = ValueDomain(datatype = XString, multiplicity = Multiplicity.One)
            ),
            ParameterDefinition(
              content = org.goldenport.value.BaseContent.simple("limit"),
              kind = ParameterDefinition.Kind.Property,
              domain = ValueDomain(datatype = XString, multiplicity = Multiplicity.ZeroOne)
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
        content = org.goldenport.value.BaseContent.simple("query"),
        request = RequestDefinition(
          parameters = List(
            ParameterDefinition(
              content = org.goldenport.value.BaseContent.simple("p1"),
              kind = ParameterDefinition.Kind.Argument,
              domain = ValueDomain(datatype = XString, multiplicity = Multiplicity.One),
              default = ParameterDefinition.Default.Value(5)
            ),
            ParameterDefinition(
              content = org.goldenport.value.BaseContent.simple("p2"),
              kind = ParameterDefinition.Kind.Property,
              domain = ValueDomain(datatype = XString, multiplicity = Multiplicity.ZeroOne),
              default = ParameterDefinition.Default.Empty
            ),
            ParameterDefinition(
              content = org.goldenport.value.BaseContent.simple("p3"),
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
        response = ResponseDefinition.void
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

    "render Blob and Clob schema metadata" in {
      Given("an operation with Blob and Clob parameters")
      val operation = OperationDefinition(
        content = org.goldenport.value.BaseContent.simple("upload"),
        request = RequestDefinition(
          parameters = List(
            ParameterDefinition(
              content = org.goldenport.value.BaseContent.simple("payload"),
              kind = ParameterDefinition.Kind.Argument,
              domain = ValueDomain(datatype = XBlob, multiplicity = Multiplicity.One)
            ),
            ParameterDefinition(
              content = org.goldenport.value.BaseContent.simple("description"),
              kind = ParameterDefinition.Kind.Property,
              domain = ValueDomain(datatype = XClob, multiplicity = Multiplicity.ZeroOne)
            )
          )
        ),
        response = ResponseDefinition.void
      )
      val service = ServiceDefinition(
        name = "blob",
        operations = OperationDefinitionGroup(NonEmptyVector.of(operation))
      )
      val defs = ServiceDefinitionGroup(Vector(service))

      When("projecting OpenAPI")
      val result = new OpenApiProjection().project(defs)

      Then("Blob is rendered as a binary string and Clob as a string")
      result match {
        case Consequence.Success(json) =>
          val payloadSchema =
            json.hcursor
              .downField("paths")
              .downField("/blob/upload")
              .downField("post")
              .downField("requestBody")
              .downField("content")
              .downField("application/json")
              .downField("schema")
              .downField("properties")
              .downField("payload")
          payloadSchema.get[String]("type").toOption.shouldBe(Some("string"))
          payloadSchema.get[String]("format").toOption.shouldBe(Some("binary"))

          val descriptionSchema =
            json.hcursor
              .downField("paths")
              .downField("/blob/upload")
              .downField("post")
              .downField("parameters")
              .downArray
              .downField("schema")
          descriptionSchema.get[String]("type").toOption.shouldBe(Some("string"))
          descriptionSchema.get[String]("format").toOption.shouldBe(None)
        case _ =>
          fail("expected Success for OpenAPI projection")
      }
    }

    "render web validation hints as JSON Schema validation keywords" in {
      Given("parameters with web validation hints")
      val operation = OperationDefinition(
        content = org.goldenport.value.BaseContent.simple("register"),
        request = RequestDefinition(
          parameters = List(
            ParameterDefinition(
              content = org.goldenport.value.BaseContent.simple("code"),
              kind = ParameterDefinition.Kind.Argument,
              domain = ValueDomain(datatype = XString, multiplicity = Multiplicity.One),
              web = WebColumn(validation = WebValidationHints(
                minLength = Some(2),
                maxLength = Some(8),
                pattern = Some("^[A-Z]+$")
              ))
            ),
            ParameterDefinition(
              content = org.goldenport.value.BaseContent.simple("priority"),
              kind = ParameterDefinition.Kind.Property,
              domain = ValueDomain(datatype = XNonNegativeInteger, multiplicity = Multiplicity.ZeroOne),
              web = WebColumn(validation = WebValidationHints(
                min = Some(BigDecimal(1)),
                max = Some(BigDecimal(100)),
                step = Some(BigDecimal(5))
              ))
            )
          )
        ),
        response = ResponseDefinition.void
      )
      val service = ServiceDefinition(
        name = "admin",
        operations = OperationDefinitionGroup(NonEmptyVector.of(operation))
      )
      val defs = ServiceDefinitionGroup(Vector(service))

      When("projecting OpenAPI")
      val result = new OpenApiProjection().project(defs)

      Then("schemas include portable validation keywords")
      result match {
        case Consequence.Success(json) =>
          val codeSchema =
            json.hcursor
              .downField("paths")
              .downField("/admin/register")
              .downField("post")
              .downField("requestBody")
              .downField("content")
              .downField("application/json")
              .downField("schema")
              .downField("properties")
              .downField("code")
          codeSchema.get[Int]("minLength").toOption.shouldBe(Some(2))
          codeSchema.get[Int]("maxLength").toOption.shouldBe(Some(8))
          codeSchema.get[String]("pattern").toOption.shouldBe(Some("^[A-Z]+$"))

          val prioritySchema =
            json.hcursor
              .downField("paths")
              .downField("/admin/register")
              .downField("post")
              .downField("parameters")
              .downArray
              .downField("schema")
          prioritySchema.get[BigDecimal]("minimum").toOption.shouldBe(Some(BigDecimal(1)))
          prioritySchema.get[BigDecimal]("maximum").toOption.shouldBe(Some(BigDecimal(100)))
          prioritySchema.get[BigDecimal]("multipleOf").toOption.shouldBe(Some(BigDecimal(5)))
        case _ =>
          fail("expected Success for OpenAPI projection")
      }
    }
  }
}
