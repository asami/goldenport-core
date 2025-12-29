package org.goldenport.schema

import java.time.YearMonth
import org.scalacheck.Gen
import org.goldenport.Consequence
import org.goldenport.observation.Cause
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Dec. 30, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
class YearMonthNormalizerSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks {

  private def assert_success(value: Any, expected: YearMonth): Unit = {
    YearMonthNormalizer.normalize(value) match {
      case Consequence.Success(result) => result.shouldBe(expected)
      case _ => fail(s"expected Success for value: ${value}")
    }
  }

  private def assert_format_error(value: Any): Unit = {
    YearMonthNormalizer.normalize(value) match {
      case Consequence.Failure(conclusion) =>
        conclusion.observation.cause.shouldBe(Some(Cause.FormatError))
      case _ => fail(s"expected Failure for value: ${value}")
    }
  }

  "YearMonthNormalizer" should {
    "normalize valid year-month inputs" in {
      Given("a YearMonthNormalizer")
      val ym = YearMonth.parse("2025-01")
      When("normalizing year-month values and strings")
      assert_success(ym, ym)
      assert_success("2025-01", ym)
      Then("inputs normalize to YearMonth")
    }

    "normalize year-month inputs property-wise" in {
      Given("a YearMonthNormalizer")
      When("normalizing arbitrary year-month values")
      forAll(_genYearMonth) { ym =>
        YearMonthNormalizer.normalize(ym.toString) match {
          case Consequence.Success(result) => result.shouldBe(ym)
          case _ => fail("expected Success for year-month string")
        }
      }
      Then("string forms round-trip to YearMonth")
    }

    "reject invalid year-month inputs as FormatError" in {
      Given("a YearMonthNormalizer")
      When("normalizing invalid inputs")
      assert_format_error("not-a-year-month")
      assert_format_error(true)
      assert_format_error(1.0d)
      assert_format_error(null)
      Then("each input fails with FormatError")
    }
  }

  private def _genYearMonth: Gen[YearMonth] =
    for {
      year <- Gen.chooseNum(2000, 2100)
      month <- Gen.chooseNum(1, 12)
    } yield YearMonth.of(year, month)
}
