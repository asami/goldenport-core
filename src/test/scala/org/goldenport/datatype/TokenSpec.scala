package org.goldenport.datatype

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.GivenWhenThen

/*
 * @since   Dec. 22, 2025
 *  version Jun.  4, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class TokenSpec extends AnyWordSpec
  with Matchers
  with GivenWhenThen {

  "Token" should {
    "accept authentication tokens within the canonical request boundary" in {
      Given("an authentication token within the canonical request boundary")
      val value = "t" * 1024

      When("the token value is constructed")
      val token = Token(value)

      Then("the exact token value is retained")
      token.value shouldBe value
    }

    "reject values beyond the canonical request boundary" in {
      Given("an authentication token longer than the canonical request boundary")
      val value = "t" * (Token.LENGTH_MAX + 1)

      When("the token value is constructed")
      Then("construction fails deterministically")
      an [IllegalArgumentException] should be thrownBy Token(value)
    }
  }
}
