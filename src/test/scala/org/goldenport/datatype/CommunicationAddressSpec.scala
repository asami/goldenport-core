package org.goldenport.datatype

import org.goldenport.Consequence
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 15, 2026
 * @version Jul. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class CommunicationAddressSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "EmailAddress" should {
    "preserve the local part and normalize the domain" in {
      Given("an email address with surrounding whitespace and an uppercase domain")
      val source = " User.Name@EXAMPLE.COM "

      When("the address is parsed")
      val result = EmailAddress.parse(source)

      Then("the domain is canonical and the local identity is preserved")
      result.map(_.value) shouldBe Consequence.success("User.Name@example.com")
    }

    "reject incomplete addresses" in {
      Given("an address without a domain suffix")
      val source = "user@localhost"

      When("the address is parsed")
      val result = EmailAddress.parse(source)

      Then("the incomplete address is rejected")
      result shouldBe a[Consequence.Failure[?]]
    }
  }

  "PhoneNumber" should {
    "normalize international dialing prefixes and visual separators" in {
      Given("an international number with a 00 prefix and separators")
      val source = "00 81-(90)-1234-5678"

      When("the phone number is parsed")
      val result = PhoneNumber.parse(source)

      Then("the canonical E.164 representation is retained")
      result.map(_.value) shouldBe Consequence.success("+819012345678")
    }

    "reject a local number without a country code" in {
      Given("a domestic-only number")
      val source = "090-1234-5678"

      When("the phone number is parsed")
      val result = PhoneNumber.parse(source)

      Then("the number is rejected until its international identity is explicit")
      result shouldBe a[Consequence.Failure[?]]
    }
  }
}
