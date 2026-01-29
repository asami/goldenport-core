package org.goldenport.schema

import java.time.LocalDateTime
import org.scalacheck.Gen
import org.goldenport.Consequence
import org.goldenport.test.matchers.ConclusionMatchers
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Dec. 30, 2025
 * @version Jan. 29, 2026
 * @author  ASAMI, Tomoharu
 */
class LocalDateTimeNormalizerSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks
  with ConclusionMatchers {

  private def _assert_success(value: Any, expected: LocalDateTime): Unit = {
    LocalDateTimeNormalizer(XLocalDateTime).normalize(value) match {
      case Consequence.Success(result) => result.shouldBe(expected)
      case _ => fail(s"expected Success for value: ${value}")
    }
  }

  private def _assert_format_error(value: Any): Unit = {
    LocalDateTimeNormalizer(XLocalDateTime).normalize(value) match {
      case Consequence.Failure(conclusion) =>
        conclusion should be_format_error_failure_conclusion
      case _ => fail(s"expected Failure for value: ${value}")
    }
  }

  private def _assert_invalid(value: Any): Unit = {
    LocalDateTimeNormalizer(XLocalDateTime).normalize(value) match {
      case Consequence.Failure(conclusion) =>
        conclusion should be_invalid_failure_conclusion
      case _ => fail(s"expected Failure for value: ${value}")
    }
  }

  "LocalDateTimeNormalizer" should {
    "normalize valid local datetime inputs" in {
      Given("a LocalDateTimeNormalizer")
      val ldt = LocalDateTime.parse("2025-01-01T10:00:00")
      When("normalizing local datetime values and strings")
      _assert_success(ldt, ldt)
      _assert_success("2025-01-01T10:00:00", ldt)
      Then("inputs normalize to LocalDateTime")
    }

    "normalize local datetime inputs property-wise" in {
      Given("a LocalDateTimeNormalizer")
      When("normalizing arbitrary local datetimes")
      forAll(_genLocalDateTime) { ldt =>
        LocalDateTimeNormalizer(XLocalDateTime).normalize(ldt.toString) match {
          case Consequence.Success(result) => result.shouldBe(ldt)
          case _ => fail("expected Success for local datetime string")
        }
      }
      Then("string forms round-trip to LocalDateTime")
    }

    "reject invalid local datetime inputs as FormatError" in {
      Given("a LocalDateTimeNormalizer")
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
}
