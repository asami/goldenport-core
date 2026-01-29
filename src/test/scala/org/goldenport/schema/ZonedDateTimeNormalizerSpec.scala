package org.goldenport.schema

import java.time.{LocalDateTime, OffsetDateTime, ZoneId, ZoneOffset, ZonedDateTime}
import org.scalacheck.Gen
import org.goldenport.Consequence
import org.goldenport.test.matchers.ConclusionMatchers
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Dec. 30, 2025
 * @version Jan. 28, 2026
 * @author  ASAMI, Tomoharu
 */
class ZonedDateTimeNormalizerSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks
  with ConclusionMatchers {

  private def _assert_success(value: Any, expected: ZonedDateTime): Unit = {
    ZonedDateTimeNormalizer(XDateTime).normalize(value) match {
      case Consequence.Success(result) => result.shouldBe(expected)
      case _ => fail(s"expected Success for value: ${value}")
    }
  }

  private def _assert_invalid(value: Any): Unit = {
    ZonedDateTimeNormalizer(XDateTime).normalize(value) match {
      case Consequence.Failure(conclusion) =>
        conclusion should be_invalid_failure_conclusion
      case _ => fail(s"expected Failure for value: ${value}")
    }
  }

  private def _assert_format_error(value: Any): Unit = {
    ZonedDateTimeNormalizer(XDateTime).normalize(value) match {
      case Consequence.Failure(conclusion) =>
        conclusion should be_format_error_failure_conclusion
      case _ => fail(s"expected Failure for value: ${value}")
    }
  }

  "ZonedDateTimeNormalizer" should {
    "normalize valid datetime inputs" in {
      Given("a ZonedDateTimeNormalizer")
      val zone = ZoneId.of("Asia/Tokyo")
      val ldt = LocalDateTime.parse("2025-01-01T10:00:00")
      val zdt = ZonedDateTime.of(ldt, zone)
      val odt = OffsetDateTime.of(ldt, ZoneOffset.ofHours(9))
      When("normalizing zoned, offset, and string representations")
      _assert_success(zdt, zdt)
      _assert_success(odt, odt.toZonedDateTime)
      _assert_success("2025-01-01T10:00:00+09:00[Asia/Tokyo]", zdt)
      _assert_success("2025-01-01T10:00:00+09:00", odt.toZonedDateTime)
      Then("all inputs normalize to ZonedDateTime")
    }

    "normalize datetime inputs property-wise" in {
      Given("a ZonedDateTimeNormalizer")
      val zone = ZoneId.of("Asia/Tokyo")
      When("normalizing arbitrary zoned and offset values")
      forAll(_genLocalDateTime) { ldt =>
        val zdt = ZonedDateTime.of(ldt, zone)
        ZonedDateTimeNormalizer(XDateTime).normalize(zdt.toString) match {
          case Consequence.Success(result) => result.shouldBe(zdt)
          case _ => fail("expected Success for zoned string")
        }
      }
      forAll(_genOffsetDateTime) { odt =>
        ZonedDateTimeNormalizer(XDateTime).normalize(odt.toString) match {
          case Consequence.Success(result) => result.shouldBe(odt.toZonedDateTime)
          case _ => fail("expected Success for offset string")
        }
      }
      Then("string forms round-trip to canonical ZonedDateTime")
    }

    "reject invalid datetime inputs as FormatError" in {
      Given("a ZonedDateTimeNormalizer")
      When("normalizing invalid inputs")
      _assert_format_error("not-a-datetime")
      _assert_invalid(true)
      _assert_invalid(1.0d)
      _assert_invalid(null)
      Then("each input fails with FormatError")
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

  private def _genOffsetDateTime: Gen[OffsetDateTime] =
    for {
      ldt <- _genLocalDateTime
      offsetHours <- Gen.chooseNum(-18, 18)
    } yield OffsetDateTime.of(ldt, ZoneOffset.ofHours(offsetHours))
}
