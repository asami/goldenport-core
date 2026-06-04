package org.goldenport.datatype

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Dec. 22, 2025
 * @version Jun.  4, 2026
 * @author  ASAMI, Tomoharu
 */
class TokenSpec extends AnyWordSpec
  with ScalaCheckDrivenPropertyChecks
  with Matchers {

  "Token" should {
    "accept generic tokens longer than cookie session identifiers" in {
      val value = "t" * 128

      Token(value).value shouldBe value
    }

    "reject values beyond the generic token limit" in {
      an [IllegalArgumentException] should be thrownBy Token("t" * 257)
    }
  }
}
