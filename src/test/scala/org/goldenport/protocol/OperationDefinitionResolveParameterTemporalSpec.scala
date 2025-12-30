package org.goldenport.protocol

import java.time.{LocalDateTime, OffsetDateTime, YearMonth, ZoneId, ZoneOffset, ZonedDateTime}
import org.scalacheck.Gen
import org.goldenport.Consequence
import org.goldenport.observation.Cause
import org.goldenport.protocol.operation.OperationRequest
import org.goldenport.protocol.spec.{OperationDefinition, ParameterDefinition, RequestDefinition, ResponseDefinition}
import org.goldenport.schema.{Multiplicity, ValueDomain, XDateTime, XLocalDateTime, XYearMonth}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Dec. 30, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
class OperationDefinitionResolveParameterTemporalSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks {

  private final case class TemporalRequest(value: Any) extends OperationRequest

  private def assert_cause(result: Consequence[?], expected: Cause): Unit =
    result match {
      case Consequence.Failure(conclusion) =>
        conclusion.observation.cause.shouldBe(Some(expected))
      case _ =>
        fail("expected Failure")
    }

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
          Consequence.success(TemporalRequest(v))
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
      op.createOperationRequest(requestWith(zdt)) match {
        case Consequence.Success(TemporalRequest(value: ZonedDateTime)) =>
          value.shouldBe(zdt)
        case _ => fail("expected ZonedDateTime success")
      }
      op.createOperationRequest(requestWith(odt)) match {
        case Consequence.Success(TemporalRequest(value: ZonedDateTime)) =>
          value.shouldBe(odt.toZonedDateTime)
        case _ => fail("expected OffsetDateTime success")
      }
      op.createOperationRequest(requestWith("2025-01-01T10:00:00+09:00[Asia/Tokyo]")) match {
        case Consequence.Success(TemporalRequest(value: ZonedDateTime)) =>
          value.shouldBe(zdt)
        case _ => fail("expected zoned string success")
      }
      op.createOperationRequest(requestWith("2025-01-01T10:00:00+09:00")) match {
        case Consequence.Success(TemporalRequest(value: ZonedDateTime)) =>
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
      ldtOp.createOperationRequest(requestWith("2025-01-01T10:00:00")) match {
        case Consequence.Success(TemporalRequest(value: LocalDateTime)) =>
          value.shouldBe(LocalDateTime.parse("2025-01-01T10:00:00"))
        case _ => fail("expected LocalDateTime success")
      }
      ymOp.createOperationRequest(requestWith("2025-01")) match {
        case Consequence.Success(TemporalRequest(value: YearMonth)) =>
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
        ldtOp.createOperationRequest(requestWith(ldt.toString)) match {
          case Consequence.Success(TemporalRequest(value: LocalDateTime)) =>
            value.shouldBe(ldt)
          case _ => fail("expected LocalDateTime success")
        }
      }
      forAll(_genYearMonth) { ym =>
        ymOp.createOperationRequest(requestWith(ym.toString)) match {
          case Consequence.Success(TemporalRequest(value: YearMonth)) =>
            value.shouldBe(ym)
          case _ => fail("expected YearMonth success")
        }
      }
      forAll(_genOffsetDateTime) { odt =>
        dtOp.createOperationRequest(requestWith(odt.toString)) match {
          case Consequence.Success(TemporalRequest(value: ZonedDateTime)) =>
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
      assert_cause(op.createOperationRequest(requestWith("not-a-datetime")), Cause.FormatError)
      assert_cause(op.createOperationRequest(requestWith(true)), Cause.FormatError)
      assert_cause(op.createOperationRequest(requestWith(1.0d)), Cause.FormatError)
      Then("each failure reports FormatError")
    }

    "keep Argument Missing and Redundant semantics" in {
      Given("an operation with a required temporal argument")
      val op = new TemporalOperationDefinition(XDateTime)
      val missing = Request(
        service = None,
        operation = "temporal-operation",
        arguments = Nil,
        switches = Nil,
        properties = Nil
      )
      When("resolving a request with a missing argument")
      assert_cause(op.createOperationRequest(missing), Cause.Argument(Cause.Reason.Missing))
      When("resolving a request with redundant arguments")
      val redundant = requestWith("2025-01-01T10:00:00+09:00", extra = List("2025-01-01T11:00:00+09:00"))
      assert_cause(op.createOperationRequest(redundant), Cause.Argument(Cause.Reason.Redundant))
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
