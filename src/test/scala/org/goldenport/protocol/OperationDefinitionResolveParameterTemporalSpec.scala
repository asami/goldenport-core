package org.goldenport.protocol

import java.time.{LocalDateTime, OffsetDateTime, YearMonth, ZoneId, ZoneOffset, ZonedDateTime}
import org.scalacheck.Gen
import org.goldenport.Consequence
import org.goldenport.observation.Cause
import org.goldenport.protocol.Request
import org.goldenport.protocol.operation.OperationRequest
import org.goldenport.protocol.spec.{OperationDefinition, ParameterDefinition, RequestDefinition, ResponseDefinition}
import org.goldenport.schema.{Multiplicity, ValueDomain, XDateTime, XLocalDateTime, XYearMonth}
import org.goldenport.test.matchers.ConsequenceMatchers
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Dec. 30, 2025
 * @version Jan. 28, 2026
 * @author  ASAMI, Tomoharu
 */
class OperationDefinitionResolveParameterTemporalSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks
  with ConsequenceMatchers {

  private final case class TemporalRequest(request: Request, value: Any) extends OperationRequest

  // private def assert_cause(result: Consequence[?], expected: Cause): Unit =
  //   result match {
  //     case Consequence.Failure(conclusion) =>
  //       conclusion.observation.cause.shouldBe(Some(expected))
  //     case _ =>
  //       fail("expected Failure")
  //   }

  private final class TemporalOperationDefinition(
    datatype: org.goldenport.schema.DataType,
    multiplicity: Multiplicity = Multiplicity.One
  ) extends OperationDefinition {
    override val specification: OperationDefinition.Specification =
      OperationDefinition.Specification(
        name = "temporal-operation",
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
        case ResolvedSingle(v, _) =>
          Consequence.success(TemporalRequest(req, v))
        case ResolvedEmpty(_) =>
          Consequence.failure("parameter missing")
        case ResolvedMultiple(_, _) =>
          Consequence.failure("multiple values not allowed")
      }
    }
  }

  private def _request_with(value: Any, extra: List[Any] = Nil): Request =
    Request(
      component = None,
      service = None,
      operation = "temporal-operation",
      arguments = Argument("value", value, None) :: extra.map(v => Argument("value", v, None)),
      switches = Nil,
      properties = Nil
    )

  "OperationDefinition.resolveParameter (temporal)" should {
    "normalize DateTime inputs to ZonedDateTime" in {
      Given("an operation using XDateTime")
      val op = new TemporalOperationDefinition(XDateTime)
      val zone = ZoneId.of("Asia/Tokyo")
      val ldt = LocalDateTime.parse("2025-01-01T10:00:00")
      val zdt = ZonedDateTime.of(ldt, zone)
      val odt = OffsetDateTime.of(ldt, ZoneOffset.ofHours(9))
      When("resolving ZonedDateTime, OffsetDateTime, and string forms")
      op.createOperationRequest(_request_with(zdt)) match {
        case Consequence.Success(TemporalRequest(req, value: ZonedDateTime)) =>
          value.shouldBe(zdt)
        case _ => fail("expected ZonedDateTime success")
      }
      op.createOperationRequest(_request_with(odt)) match {
        case Consequence.Success(TemporalRequest(req, value: ZonedDateTime)) =>
          value.shouldBe(odt.toZonedDateTime)
        case _ => fail("expected OffsetDateTime success")
      }
      op.createOperationRequest(_request_with("2025-01-01T10:00:00+09:00[Asia/Tokyo]")) match {
        case Consequence.Success(TemporalRequest(req, value: ZonedDateTime)) =>
          value.shouldBe(zdt)
        case _ => fail("expected zoned string success")
      }
      op.createOperationRequest(_request_with("2025-01-01T10:00:00+09:00")) match {
        case Consequence.Success(TemporalRequest(req, value: ZonedDateTime)) =>
          value.shouldBe(odt.toZonedDateTime)
        case _ => fail("expected offset string success")
      }
      Then("values are canonicalized without locale overrides")
    }

    "normalize LocalDateTime and YearMonth inputs" in {
      Given("operations using XLocalDateTime and XYearMonth")
      val ldtOp = new TemporalOperationDefinition(XLocalDateTime)
      val ymOp = new TemporalOperationDefinition(XYearMonth)
      When("resolving string and value forms")
      ldtOp.createOperationRequest(_request_with("2025-01-01T10:00:00")) match {
        case Consequence.Success(TemporalRequest(req, value: LocalDateTime)) =>
          value.shouldBe(LocalDateTime.parse("2025-01-01T10:00:00"))
        case _ => fail("expected LocalDateTime success")
      }
      ymOp.createOperationRequest(_request_with("2025-01")) match {
        case Consequence.Success(TemporalRequest(req, value: YearMonth)) =>
          value.shouldBe(YearMonth.parse("2025-01"))
        case _ => fail("expected YearMonth success")
      }
      Then("values are canonicalized to java.time types")
    }

    "normalize temporal inputs property-wise" in {
      Given("operations using temporal datatypes")
      val ldtOp = new TemporalOperationDefinition(XLocalDateTime)
      val ymOp = new TemporalOperationDefinition(XYearMonth)
      val dtOp = new TemporalOperationDefinition(XDateTime)
      When("resolving arbitrary valid strings")
      forAll(_genLocalDateTime) { ldt =>
        ldtOp.createOperationRequest(_request_with(ldt.toString)) match {
          case Consequence.Success(TemporalRequest(req, value: LocalDateTime)) =>
            value.shouldBe(ldt)
          case _ => fail("expected LocalDateTime success")
        }
      }
      forAll(_genYearMonth) { ym =>
        ymOp.createOperationRequest(_request_with(ym.toString)) match {
          case Consequence.Success(TemporalRequest(req, value: YearMonth)) =>
            value.shouldBe(ym)
          case _ => fail("expected YearMonth success")
        }
      }
      forAll(_genOffsetDateTime) { odt =>
        dtOp.createOperationRequest(_request_with(odt.toString)) match {
          case Consequence.Success(TemporalRequest(req, value: ZonedDateTime)) =>
            value.shouldBe(odt.toZonedDateTime)
          case _ => fail("expected DateTime success")
        }
      }
      Then("canonical values are produced consistently")
    }

    "fail with FormatError for invalid temporal formats" in {
      Given("an operation using XDateTime")
      val op = new TemporalOperationDefinition(XDateTime)
      When("resolving invalid inputs")
      op.createOperationRequest(_request_with("not-a-datetime")) should be_argument_format_error_failure
      op.createOperationRequest(_request_with(true)) should be_argument_invalid_failure
      op.createOperationRequest(_request_with(1.0d)) should be_argument_invalid_failure
      Then("each failure reports FormatError")
    }

    "keep Argument Missing and Redundant semantics" in {
      Given("an operation with a required temporal argument")
      val op = new TemporalOperationDefinition(XDateTime)
      val missing = Request(
        component = None,
        service = None,
        operation = "temporal-operation",
        arguments = Nil,
        switches = Nil,
        properties = Nil
      )
      When("resolving a request with a missing argument")
      op.createOperationRequest(missing) should be_argument_missing_failure
      When("resolving a request with redundant arguments")
      val redundant = _request_with("2025-01-01T10:00:00+09:00", extra = List("2025-01-01T11:00:00+09:00"))
      op.createOperationRequest(redundant) should be_argument_redundant_failure
      Then("Missing and Redundant are reported as Argument errors")
    }
  }

  private def _genLocalDateTime: Gen[LocalDateTime] =
    for {
      year <- Gen.chooseNum(2000, 2100)
      month <- Gen.chooseNum(1, 12)
      day <- Gen.chooseNum(1, 28)
      hour <- Gen.chooseNum(0, 23)
      minute <- Gen.chooseNum(0, 59)
      second <- Gen.chooseNum(0, 59)
      nano <- Gen.chooseNum(0, 999999999)
    } yield LocalDateTime.of(year, month, day, hour, minute, second, nano)

  private def _genYearMonth: Gen[YearMonth] =
    for {
      year <- Gen.chooseNum(2000, 2100)
      month <- Gen.chooseNum(1, 12)
    } yield YearMonth.of(year, month)

  private def _genOffsetDateTime: Gen[OffsetDateTime] =
    for {
      ldt <- _genLocalDateTime
      offsetHours <- Gen.chooseNum(-18, 18)
    } yield OffsetDateTime.of(ldt, ZoneOffset.ofHours(offsetHours))
}
