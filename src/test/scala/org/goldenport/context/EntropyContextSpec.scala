package org.goldenport.context

import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Jul. 15, 2026
 * @version Jul. 15, 2026
 * @author  ASAMI, Tomoharu
 */
class EntropyContextSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks {
  private val _non_empty_text = Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
  private val _byte_size = Gen.choose(0, 128)

  "EntropyContext" should {
    "reproduce deterministic entropy for the same seed and purpose" in {
      forAll(_non_empty_text, _non_empty_text, _byte_size) { (seed, purpose, size) =>
        Given("two deterministic entropy contexts with the same seed")
        val left = EntropyContext.deterministic(seed)
        val right = EntropyContext.deterministic(seed)

        When("both contexts generate two values for the same purpose")
        val leftvalues = Vector(left.bytes(purpose, size).toSeq, left.bytes(purpose, size).toSeq)
        val rightvalues = Vector(right.bytes(purpose, size).toSeq, right.bytes(purpose, size).toSeq)

        Then("both advancing sequences are identical")
        leftvalues shouldBe rightvalues
      }
    }

    "isolate entropy purposes and domain-random consumption" in {
      forAll(_non_empty_text, _byte_size) { (seed, size) =>
        Given("equivalent domain streams and a separate entropy context")
        val expectedrandom = RandomContext.seeded(seed).stream("domain.pricing")
        val actualrandom = RandomContext.seeded(seed).stream("domain.pricing")
        val entropy = EntropyContext.deterministic(seed)

        When("ID entropy and an unrelated entropy purpose are consumed")
        entropy.bytes("id.entity", size)
        entropy.bytes("retry.jitter", size)
        val expected = expectedrandom.nextLong()
        val actual = actualrandom.nextLong()

        Then("domain-random values remain unchanged")
        actual shouldBe expected
      }
    }

    "generate URL-safe tokens with the requested entropy size" in {
      forAll(_non_empty_text, Gen.choose(0, 64)) { (seed, size) =>
        Given("a deterministic entropy context")
        val entropy = EntropyContext.deterministic(seed)

        When("a token is generated")
        val token = entropy.token("security.test-token", size)

        Then("the token contains only unpadded URL-safe Base64 characters")
        token should fullyMatch regex "[A-Za-z0-9_-]*"

        And("the source seed is not exposed by diagnostics")
        entropy.toString shouldBe "EntropyContext.Deterministic"
      }
    }

    "reject blank purposes" in {
      forAll(Gen.oneOf("", " ", "\t"), _byte_size) { (purpose, size) =>
        Given("an entropy context and a blank purpose")
        val entropy = EntropyContext.secure()

        When("entropy bytes are requested")
        val result = intercept[IllegalArgumentException](entropy.bytes(purpose, size))

        Then("the invalid purpose is rejected before generation")
        result.getMessage should include("purpose")
        entropy.toString shouldBe "EntropyContext.Secure"
      }
    }

    "reject negative byte sizes" in {
      forAll(Gen.choose(Int.MinValue, -1)) { size =>
        Given("an entropy context and a negative byte size")
        val entropy = EntropyContext.deterministic("invalid-size")

        When("entropy bytes are requested")
        val result = intercept[IllegalArgumentException](entropy.bytes("id.entity", size))

        Then("the invalid bound is rejected before generation")
        result.getMessage should include("size")
      }
    }
  }
}
