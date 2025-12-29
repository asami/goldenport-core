package org.goldenport.protocol.spec

import org.goldenport.Consequence
import org.goldenport.observation.Cause
import org.goldenport.protocol.{Argument, Request}
import org.goldenport.protocol.operation.OperationRequest
import org.goldenport.schema.{Multiplicity, ValueDomain, XNonNegativeInteger, XPositiveInteger}
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Dec. 29, 2025
 * @version Dec. 29, 2025
 * @author  ASAMI, Tomoharu
 */
class OperationDefinitionResolveParameterSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks {

  private final case class IntRequest(value: BigInt) extends OperationRequest

  private def assert_cause(result: Consequence[?], expected: Cause): Unit =
    result match {
      case Consequence.Failure(conclusion) =>
        conclusion.observation.cause.shouldBe(Some(expected))
      case _ =>
        fail("expected Failure")
    }

  private final class IntOperationDefinition(
    datatype: org.goldenport.schema.DataType,
    multiplicity: Multiplicity = Multiplicity.One
  ) extends OperationDefinition {
    override val specification: OperationDefinition.Specification =
      OperationDefinition.Specification(
        name = "int-operation",
        request = RequestDefinition(
          parameters = List(
            ParameterDefinition(
              name = "value",
              kind = ParameterDefinition.Kind.Argument,
              domain = ValueDomain(
                datatype = datatype,
                multiplicity = multiplicity
              )
            )
          )
        ),
        response = ResponseDefinition(result = Nil)
      )

    override def createOperationRequest(
      req: Request
    ): Consequence[OperationRequest] = {
      given Request = req
      val param = specification.request.parameters.head
      resolveParameter(param).flatMap {
        case ResolvedSingle(v: BigInt, _) =>
          Consequence.success(IntRequest(v))
        case ResolvedSingle(_, _) =>
          Consequence.failure("unexpected parameter type")
        case ResolvedEmpty(_) =>
          Consequence.failure("parameter missing")
        case ResolvedMultiple(_, _) =>
          Consequence.failure("multiple values not allowed")
      }
    }
  }

  private def requestWith(value: Any, extra: List[Any] = Nil): Request =
    Request(
      service = None,
      operation = "int-operation",
      arguments = Argument("value", value, None) :: extra.map(v => Argument("value", v, None)),
      switches = Nil,
      properties = Nil
    )

  "OperationDefinition.resolveParameter" should {
    "produce OperationRequest when normalization and validation succeed" in {
      Given("an operation using XNonNegativeInteger")
      val op = new IntOperationDefinition(XNonNegativeInteger)
      When("resolving a valid integer value")
      val result = op.createOperationRequest(requestWith("0"))

      result match {
        case Consequence.Success(opreq: IntRequest) =>
          Then("the OperationRequest is generated with normalized value")
          opreq.value.shouldBe(BigInt(0))
        case _ =>
          fail("expected Success with IntRequest")
      }
    }

    "produce OperationRequest property-wise for valid integer strings" in {
      Given("operations using integer datatypes")
      val nonNegative = new IntOperationDefinition(XNonNegativeInteger)
      val positive = new IntOperationDefinition(XPositiveInteger)
      When("resolving arbitrary valid values")
      forAll(Gen.chooseNum(0L, Long.MaxValue)) { value =>
        nonNegative.createOperationRequest(requestWith(value.toString)) match {
          case Consequence.Success(opreq: IntRequest) =>
            opreq.value.shouldBe(BigInt(value))
          case _ =>
            fail(s"expected Success for value: ${value}")
        }
      }
      Then("valid values always generate OperationRequest")
      forAll(Gen.chooseNum(1L, Long.MaxValue)) { value =>
        positive.createOperationRequest(requestWith(value.toString)) match {
          case Consequence.Success(opreq: IntRequest) =>
            opreq.value.shouldBe(BigInt(value))
          case _ =>
            fail(s"expected Success for value: ${value}")
        }
      }
      Then("valid values always generate OperationRequest")
    }

    "fail with ValueDomainError for invalid integer domains" in {
      Given("operations using integer datatypes with domain predicates")
      val nonNegative = new IntOperationDefinition(XNonNegativeInteger)
      val positive = new IntOperationDefinition(XPositiveInteger)

      When("resolving domain-violating values")
      Then("each failure reports ValueDomainError")
      assert_cause(nonNegative.createOperationRequest(requestWith("-1")), Cause.ValueDomainError)
      assert_cause(positive.createOperationRequest(requestWith("0")), Cause.ValueDomainError)
    }

    "fail with ValueDomainError property-wise for negative values" in {
      Given("an operation using XNonNegativeInteger")
      val nonNegative = new IntOperationDefinition(XNonNegativeInteger)
      When("resolving negative values")
      forAll(Gen.chooseNum(Long.MinValue, -1L)) { value =>
        Then("each failure reports ValueDomainError")
        assert_cause(
          nonNegative.createOperationRequest(requestWith(value.toString)),
          Cause.ValueDomainError
        )
      }
    }

    "fail with FormatError for invalid integer formats" in {
      Given("an operation using XPositiveInteger")
      val op = new IntOperationDefinition(XPositiveInteger)
      When("resolving a non-numeric string")
      Then("the failure reports FormatError")
      assert_cause(op.createOperationRequest(requestWith("abc")), Cause.FormatError)
    }

    "fail with FormatError property-wise for alphabetic strings" in {
      Given("an operation using XPositiveInteger")
      val op = new IntOperationDefinition(XPositiveInteger)
      When("resolving alphabetic strings")
      forAll(Gen.alphaStr.suchThat(_.nonEmpty)) { value =>
        Then("each failure reports FormatError")
        assert_cause(op.createOperationRequest(requestWith(value)), Cause.FormatError)
      }
    }

    "keep Argument Missing and Redundant semantics" in {
      Given("an operation with a required integer argument")
      val op = new IntOperationDefinition(XNonNegativeInteger)
      val missing = Request(
        service = None,
        operation = "int-operation",
        arguments = Nil,
        switches = Nil,
        properties = Nil
      )

      When("resolving a request with a missing argument")
      Then("Missing is reported as Argument error")
      assert_cause(op.createOperationRequest(missing), Cause.Argument(Cause.Reason.Missing))

      When("resolving a request with redundant arguments")
      val redundant = requestWith("1", extra = List("2"))
      Then("Redundant is reported as Argument error")
      assert_cause(op.createOperationRequest(redundant), Cause.Argument(Cause.Reason.Redundant))
    }
  }
}
