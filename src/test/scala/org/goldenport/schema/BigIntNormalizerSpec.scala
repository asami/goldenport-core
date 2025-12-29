package org.goldenport.schema

import org.goldenport.Consequence
import org.goldenport.observation.Cause
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
class BigIntNormalizerSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks {

  private def assert_success(value: Any, expected: BigInt): Unit = {
    BigIntNormalizer.normalize(value) match {
      case Consequence.Success(result) => result.shouldBe(expected)
      case _ => fail(s"expected Success for value: ${value}")
    }
  }

  private def assert_format_error(value: Any): Unit = {
    BigIntNormalizer.normalize(value) match {
      case Consequence.Failure(conclusion) =>
        conclusion.observation.cause.shouldBe(Some(Cause.FormatError))
      case _ => fail(s"expected Failure for value: ${value}")
    }
  }

  "BigIntNormalizer" should {
    "normalize valid integer inputs" in {
      Given("a BigIntNormalizer")
      When("normalizing supported integer representations")
      assert_success("123", BigInt(123))
      assert_success(1, BigInt(1))
      assert_success(1L, BigInt(1))
      assert_success(1.toShort, BigInt(1))
      assert_success(BigInt(1), BigInt(1))
      Then("all inputs normalize to BigInt successfully")
    }

    "normalize integer inputs property-wise" in {
      Given("a BigIntNormalizer")
      When("normalizing arbitrary integer values")
      forAll(Gen.chooseNum(Long.MinValue, Long.MaxValue)) { value =>
        assert_success(value.toString, BigInt(value))
        assert_success(value, BigInt(value))
        assert_success(BigInt(value), BigInt(value))
      }
      forAll(Gen.chooseNum(Short.MinValue.toInt, Short.MaxValue.toInt)) { value =>
        assert_success(value.toShort, BigInt(value))
      }
      Then("each representation round-trips to BigInt")
    }

    "reject invalid integer inputs as FormatError" in {
      Given("a BigIntNormalizer")
      When("normalizing unsupported or malformed representations")
      assert_format_error("abc")
      assert_format_error("")
      assert_format_error(1.0d)
      assert_format_error(true)
      assert_format_error(null)
      Then("each input fails with FormatError")
    }

    "reject non-numeric strings property-wise" in {
      Given("a BigIntNormalizer")
      When("normalizing alphabetic strings")
      forAll(Gen.alphaStr.suchThat(_.nonEmpty)) { value =>
        assert_format_error(value)
      }
      Then("each string fails with FormatError")
    }

    "reject non-canonical whitespace strings as FormatError" in {
      Given("a BigIntNormalizer")
      When("normalizing a whitespace-prefixed string")
      assert_format_error(" 1")
      Then("the input fails with FormatError")
    }
  }
}
