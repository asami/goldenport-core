package org.goldenport.datatype

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Dec. 22, 2025
 * @version Jun.  4, 2026
 * @author  ASAMI, Tomoharu
 */
class NameSpec extends AnyWordSpec
  with ScalaCheckDrivenPropertyChecks
  with Matchers {

  "Name" should {
    "accept generic names longer than authentication identifiers" in {
      val value = "n" * 128

      Name(value).value shouldBe value
    }

    "reject values beyond the generic name limit" in {
      an [IllegalArgumentException] should be thrownBy Name("n" * 257)
    }
  }
}
