package org.goldenport.schema

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
class IntegerDataTypeSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks {
  "IntegerDataType" should {
    "validate XNonNegativeInteger boundaries" in {
      Given("an XNonNegativeInteger datatype")
      When("checking boundary values")
      XNonNegativeInteger.isValid(BigInt(-1)).shouldBe(false)
      XNonNegativeInteger.isValid(BigInt(0)).shouldBe(true)
      XNonNegativeInteger.isValid(BigInt(1)).shouldBe(true)
      Then("only non-negative values are valid")
    }

    "validate XNonNegativeInteger property-wise" in {
      Given("an XNonNegativeInteger datatype")
      When("checking a range of values")
      forAll(Gen.chooseNum(Long.MinValue, -1L)) { value =>
        XNonNegativeInteger.isValid(BigInt(value)).shouldBe(false)
      }
      forAll(Gen.chooseNum(0L, Long.MaxValue)) { value =>
        XNonNegativeInteger.isValid(BigInt(value)).shouldBe(true)
      }
      Then("negative values are invalid and non-negative values are valid")
    }

    "validate XPositiveInteger boundaries" in {
      Given("an XPositiveInteger datatype")
      When("checking boundary values")
      XPositiveInteger.isValid(BigInt(-1)).shouldBe(false)
      XPositiveInteger.isValid(BigInt(0)).shouldBe(false)
      XPositiveInteger.isValid(BigInt(1)).shouldBe(true)
      Then("only positive values are valid")
    }

    "validate XPositiveInteger property-wise" in {
      Given("an XPositiveInteger datatype")
      When("checking a range of values")
      forAll(Gen.chooseNum(Long.MinValue, 0L)) { value =>
        XPositiveInteger.isValid(BigInt(value)).shouldBe(false)
      }
      forAll(Gen.chooseNum(1L, Long.MaxValue)) { value =>
        XPositiveInteger.isValid(BigInt(value)).shouldBe(true)
      }
      Then("zero and negative values are invalid while positives are valid")
    }
  }
}
