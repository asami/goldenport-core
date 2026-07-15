package org.goldenport.datatype

import io.circe.parser.decode
import io.circe.syntax.*
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class PasswordSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Password" should {
    "preserve bounded printable secret text through its codec" in {
      Given("a printable password within the canonical request boundary")
      val source = "correct horse battery staple"

      When("the password is constructed and round-tripped as JSON")
      val password = Password(source)
      val decoded = decode[Password](password.asJson.noSpaces)

      Then("the exact nonlocalized secret value is preserved")
      password.value shouldBe source
      decoded.map(_.value) shouldBe Right(source)
    }

    "reject empty, oversized, and control-character values" in {
      Given("values outside the canonical password boundary")
      val oversized = "p" * (Password.LENGTH_MAX + 1)

      When("each value is constructed")
      Then("the datatype rejects it deterministically")
      an[IllegalArgumentException] should be thrownBy Password("")
      an[IllegalArgumentException] should be thrownBy Password(oversized)
      an[IllegalArgumentException] should be thrownBy Password("line1\nline2")
    }
  }
}
