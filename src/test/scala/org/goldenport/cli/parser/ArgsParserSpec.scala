package org.goldenport.cli.parser

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalacheck.Gen

import org.goldenport.protocol.{Argument, Property, Switch}
import org.goldenport.protocol.spec.{OperationDefinition, OperationDefinitionGroup, RequestDefinition, ResponseDefinition, ServiceDefinition}
import cats.data.NonEmptyVector

/*
 * @since   Mar. 12, 2026
 * @version Apr. 11, 2026
 * @author  ASAMI, Tomoharu
 */
class ArgsParserSpec
  extends AnyWordSpec
    with GivenWhenThen
    with Matchers
    with ScalaCheckDrivenPropertyChecks {

  private val service = {
    val operation =
      OperationDefinition(
        content = org.goldenport.value.BaseContent.simple("op"),
        request = RequestDefinition(parameters = Nil),
        response = ResponseDefinition.void
      )
    ServiceDefinition(
      name = "test",
      operations = OperationDefinitionGroup(NonEmptyVector.of(operation))
    )
  }

  private val parser = new ArgsParser(ArgsParser.Config())

  "ArgsParser" should {
    "treat --name value as Property" in {
      Given("CLI args with --name value")
      val args = List("op", "--name", "taro")

      When("parsing the args")
      val req = parser.parse(service, args)

      Then("the named input is a Property")
      req.properties shouldBe List(Property("name", "taro", None))
      req.switches shouldBe Nil
    }

    "treat --flag as Switch when no value follows" in {
      Given("CLI args with --flag and no value")
      val args = List("op", "--verbose")

      When("parsing the args")
      val req = parser.parse(service, args)

      Then("the flag is recorded as a Switch")
      req.switches shouldBe List(Switch("verbose", true, None))
    }

    "parse named properties property-wise" in {
      Given("arbitrary property names and values")
      val keygen = Gen.alphaLowerStr.suchThat(_.nonEmpty)
      val valuegen = Gen.alphaStr.suchThat(_.nonEmpty)

      When("parsing --name value pairs")
      forAll(keygen, valuegen) { (key, value) =>
        val req = parser.parse(service, List("op", s"--$key", value))
        req.properties shouldBe List(Property(key, value, None))
        req.switches shouldBe Nil
      }

      Then("each input becomes a Property")
    }

    "parse parameters using OperationDefinition" in {
      Given("an operation definition with argument, property, and switch")
      val opdef =
        OperationDefinition(
          content = org.goldenport.value.BaseContent.simple("op"),
          request = RequestDefinition(
            parameters = List(
              org.goldenport.protocol.spec.ParameterDefinition(
                content = org.goldenport.value.BaseContent.simple("name"),
                kind = org.goldenport.protocol.spec.ParameterDefinition.Kind.Argument
              ),
              org.goldenport.protocol.spec.ParameterDefinition(
                content = org.goldenport.value.BaseContent.simple("format"),
                kind = org.goldenport.protocol.spec.ParameterDefinition.Kind.Property
              ),
              org.goldenport.protocol.spec.ParameterDefinition(
                content = org.goldenport.value.BaseContent.simple("verbose"),
                kind = org.goldenport.protocol.spec.ParameterDefinition.Kind.Switch
              )
            )
          ),
          response = ResponseDefinition.void
        )

      When("parsing arguments with explicit names")
      val req = parser.parse(opdef, List("--name", "taro", "--format", "json", "--verbose"))

      Then("it classifies argument, property, and switch correctly")
      req.arguments shouldBe List(Argument("name", "taro", None))
      req.properties shouldBe List(Property("format", "json", None))
      req.switches shouldBe List(Switch("verbose", true, None))
    }

    "assign positional arguments to defined argument names" in {
      Given("an operation definition with a single argument")
      val opdef =
        OperationDefinition(
          content = org.goldenport.value.BaseContent.simple("op"),
          request = RequestDefinition(
            parameters = List(
              org.goldenport.protocol.spec.ParameterDefinition(
                content = org.goldenport.value.BaseContent.simple("name"),
                kind = org.goldenport.protocol.spec.ParameterDefinition.Kind.Argument
              )
            )
          ),
          response = ResponseDefinition.void
        )

      When("parsing a positional argument")
      val req = parser.parse(opdef, List("taro"))

      Then("it assigns the value to the argument name")
      req.arguments shouldBe List(Argument("name", "taro", None))
      req.properties shouldBe Nil
      req.switches shouldBe Nil
    }

  }
}
