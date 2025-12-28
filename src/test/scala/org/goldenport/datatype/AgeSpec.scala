package org.goldenport.datatype

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalacheck.Gen

/*
 * @since   Dec. 22, 2025
 * @version Dec. 26, 2025
 * @author  ASAMI, Tomoharu
 */
class AgeSpec extends AnyWordSpec
  with ScalaCheckDrivenPropertyChecks
  with Matchers {

  "Age" should {

    "satisfy basic properties" in {
      // forAll(Gen.chooseNum(0, Age.MAX_DEFAULT)) { (n: Int) =>
      //   noException should be thrownBy {
      //     Age(n)
      //   }
      // }
      pending
    }

    "preserve invariants" in {
      val negative = Gen.chooseNum(Int.MinValue, -1)
      val overMax = Gen.chooseNum(Age.MAX_DEFAULT + 1, Age.MAX_DEFAULT + 1000)

      // forAll(Gen.oneOf(negative, overMax)) { (n: Int) =>
      //   an[IllegalArgumentException] should be thrownBy {
      //     Age(n)
      //   }
      // }
      pending
    }
  }
}
