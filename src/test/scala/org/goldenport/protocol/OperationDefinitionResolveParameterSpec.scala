package org.goldenport.protocol

import org.goldenport.Consequence
// import org.goldenport.observation.Cause
import org.goldenport.provisional.observation.Taxonomy
import org.goldenport.protocol.Request
import org.goldenport.protocol.operation.OperationRequest
import org.goldenport.protocol.spec.{OperationDefinition, ParameterDefinition, RequestDefinition, ResponseDefinition}
import org.goldenport.schema.{Constraint, DataType, Multiplicity, ValueDomain, XNonNegativeInteger, XPositiveInteger, XString}
import org.goldenport.test.matchers.ConsequenceMatchers
import org.scalacheck.Gen
import org.scalacheck.Shrink
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
/*
 * @since   Dec. 29, 2025
 * @version Jan. 29, 2026
 * @author  ASAMI, Tomoharu
 */
class OperationDefinitionResolveParameterSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks
  with ConsequenceMatchers {

  implicit val noShrink: Shrink[Long] = Shrink.shrinkAny

  private final case class IntRequest(request: Request, value: BigInt) extends OperationRequest
  private final case class StringRequest(request: Request, value: String) extends OperationRequest

  private def assert_taxonomy(result: Consequence[?], expected: Taxonomy): Unit =
    result match {
      case Consequence.Failure(conclusion) =>
        conclusion.observation.taxonomy.shouldBe(expected)
      case Consequence.Success(r) =>
        fail(s"expected Failure but Success: $r")
    }

  private def assert_datatype(result: Consequence[?]): Unit =
    result match {
      case Consequence.Failure(conclusion) =>
        conclusion.observation.taxonomy.shouldBe(Taxonomy.argumentInvalid)
        conclusion.observation.cause.getDataType.nonEmpty.shouldBe(true)
      case Consequence.Success(r) =>
        fail(s"expected Failure but Success: $r")
    }

  private def assert_constraint(result: Consequence[?]): Unit =
    result match {
      case Consequence.Failure(conclusion) =>
        conclusion.observation.taxonomy.shouldBe(Taxonomy.argumentInvalid)
        conclusion.observation.cause.getConstraints.nonEmpty.shouldBe(true)
      case Consequence.Success(r) =>
        fail(s"expected Failure but Success: $r")
    }

  private final class IntOperationDefinition(
    datatype: DataType,
    multiplicity: Multiplicity = Multiplicity.One,
    constraints: Vector[Constraint] = Vector.empty
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
                multiplicity = multiplicity,
                constraints = constraints
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
          Consequence.success(IntRequest(req, v))
        case ResolvedSingle(_, _) =>
          Consequence.failure("unexpected parameter type")
        case ResolvedEmpty(_) =>
          Consequence.failure("parameter missing")
        case ResolvedMultiple(_, _) =>
          Consequence.failure("multiple values not allowed")
      }
    }
  }

  private final class StringOperationDefinition(
    datatype: DataType = XString,
    multiplicity: Multiplicity = Multiplicity.One,
    constraints: Vector[Constraint] = Vector.empty
  ) extends OperationDefinition {
    override val specification: OperationDefinition.Specification =
      OperationDefinition.Specification(
        name = "string-operation",
        request = RequestDefinition(
          parameters = List(
            ParameterDefinition(
              name = "value",
              kind = ParameterDefinition.Kind.Argument,
              domain = ValueDomain(
                datatype = datatype,
                multiplicity = multiplicity,
                constraints = constraints
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
        case ResolvedSingle(v: String, _) =>
          Consequence.success(StringRequest(req, v))
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
      component = None,
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
      // assert_taxonomy(nonNegative.createOperationRequest(requestWith("-1")), Taxonomy.argumentInvalid)
      // assert_taxonomy(positive.createOperationRequest(requestWith("0")), Taxonomy.argumentInvalid)
      nonNegative.createOperationRequest(requestWith("-1")) should be_argument_invalid_failure
      positive.createOperationRequest(requestWith("0")) should be_argument_invalid_failure
    }

    "fail with ValueDomainError property-wise for negative values" in {
      Given("an operation using XNonNegativeInteger")
      val nonNegative = new IntOperationDefinition(XNonNegativeInteger)
      When("resolving negative values")
      forAll(Gen.chooseNum(Long.MinValue, -1L)) { value =>
        Then("each failure reports ValueDomainError")
        assert_datatype(
          nonNegative.createOperationRequest(requestWith(value.toString))
        )
      }
    }

    "fail with FormatError for invalid integer formats" in {
      Given("an operation using XPositiveInteger")
      val op = new IntOperationDefinition(XPositiveInteger)
      When("resolving a non-numeric string")
      Then("the failure reports FormatError")
      assert_taxonomy(
        op.createOperationRequest(requestWith("abc")),
        Taxonomy.argumentFormatError
      )
    }

    "fail with FormatError property-wise for alphabetic strings" in {
      Given("an operation using XPositiveInteger")
      val op = new IntOperationDefinition(XPositiveInteger)
      When("resolving alphabetic strings")
      forAll(Gen.alphaStr.suchThat(_.nonEmpty)) { value =>
        Then("each failure reports FormatError")
        assert_taxonomy(
          op.createOperationRequest(requestWith(value)),
          Taxonomy.argumentFormatError
        )
      }
    }

    "validate enum constraints during OperationRequest construction" in {
      Given("an operation with enum constraints on string parameters")
      val op = new StringOperationDefinition(
        constraints = Vector(Constraint.Enum(Set("a", "b")))
      )

      When("resolving an allowed value")
      op.createOperationRequest(requestWith("a")) match {
        case Consequence.Success(opreq: StringRequest) =>
          Then("the OperationRequest is generated")
          opreq.value.shouldBe("a")
        case _ =>
          fail("expected Success with StringRequest")
      }

      When("resolving a disallowed value")
      Then("the failure reports ValueDomainError")
      assert_constraint(
        op.createOperationRequest(requestWith("c"))
      )
    }

    "validate numeric range constraints during OperationRequest construction" in {
      Given("an operation with integer range constraints")
      val op = new IntOperationDefinition(
        datatype = XNonNegativeInteger,
        constraints = Vector(
          Constraint.MinInclusive(BigInt(0)),
          Constraint.MaxInclusive(BigInt(10))
        )
      )

      When("resolving a value within the range")
      op.createOperationRequest(requestWith("5")) match {
        case Consequence.Success(opreq: IntRequest) =>
          Then("the OperationRequest is generated")
          opreq.value.shouldBe(BigInt(5))
        case _ =>
          fail("expected Success with IntRequest")
      }

      When("resolving values outside the range")
      Then("each failure reports ValueDomainError")
      assert_datatype(
        op.createOperationRequest(requestWith("-1"))
      )
      assert_constraint(
        op.createOperationRequest(requestWith("11"))
      )
    }

    "validate range constraints property-wise" in {
      Given("an operation with integer range constraints")
      val op = new IntOperationDefinition(
        datatype = XNonNegativeInteger,
        constraints = Vector(
          Constraint.MinInclusive(BigInt(0)),
          Constraint.MaxInclusive(BigInt(10))
        )
      )

      When("resolving values within the range")
      forAll(Gen.chooseNum(0L, 10L)) { value =>
        op.createOperationRequest(requestWith(value.toString)) match {
          case Consequence.Success(opreq: IntRequest) =>
            opreq.value.shouldBe(BigInt(value))
          case _ =>
            fail(s"expected Success for value: ${value}")
        }
      }

      When("resolving values outside the range")
      forAll(Gen.oneOf(Gen.chooseNum(-10L, -1L), Gen.chooseNum(11L, 20L))) { value =>
        if (value < 0)
          assert_datatype(
            op.createOperationRequest(requestWith(value.toString))
          )
        else
          assert_constraint(
            op.createOperationRequest(requestWith(value.toString))
          )
      }
    }

    "enforce validation in default OperationDefinition.Instance" in {
      Given("a default OperationDefinition with a notEmpty constraint")
      val op = OperationDefinition(
        name = "string-operation",
        request = RequestDefinition(
          parameters = List(
            ParameterDefinition(
              name = "value",
              kind = ParameterDefinition.Kind.Argument,
              domain = ValueDomain(
                datatype = XString,
                multiplicity = Multiplicity.One,
                constraints = Vector(Constraint.NotEmpty)
              )
            )
          )
        ),
        response = ResponseDefinition(result = Nil)
      )

      When("resolving a violating value through the default instance")
      val req = Request(
        component = None,
        service = None,
        operation = "string-operation",
        arguments = List(Argument("value", "", None)),
        switches = Nil,
        properties = Nil
      )
      val result = op.createOperationRequest(req)

      Then("the failure reports ValueDomainError")
      assert_constraint(result)
    }

    "keep Argument Missing and Redundant semantics" in {
      Given("an operation with a required integer argument")
      val op = new IntOperationDefinition(XNonNegativeInteger)
      val missing = Request(
        component = None,
        service = None,
        operation = "int-operation",
        arguments = Nil,
        switches = Nil,
        properties = Nil
      )

      When("resolving a request with a missing argument")
      Then("Missing is reported as Argument error")
      assert_taxonomy(op.createOperationRequest(missing), Taxonomy.argumentMissing)

      When("resolving a request with redundant arguments")
      val redundant = requestWith("1", extra = List("2"))
      Then("Redundant is reported as Argument error")
      assert_taxonomy(op.createOperationRequest(redundant), Taxonomy.argumentRedundant)
    }
  }
}
