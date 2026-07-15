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
class RandomContextSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks {
  private val _non_empty_text = Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
  private val _sample_count = Gen.choose(1, 32)

  "RandomContext" should {
    "reproduce a named stream for the same seed and purpose" in {
      forAll(_non_empty_text, _non_empty_text, _sample_count) { (seed, purpose, count) =>
        Given("two seeded contexts with the same seed and named purpose")
        val left = RandomContext.seeded(seed).stream(purpose)
        val right = RandomContext.seeded(seed).stream(purpose)

        When("both streams advance by the same number of calls")
        val leftvalues = Vector.fill(count)(left.nextLong())
        val rightvalues = Vector.fill(count)(right.nextLong())

        Then("the generated sequences are identical")
        leftvalues shouldBe rightvalues
      }
    }

    "isolate calls made through different purpose streams" in {
      forAll(_non_empty_text, _sample_count) { (seed, count) =>
        Given("two equivalent roots and an unrelated purpose stream")
        val undisturbed = RandomContext.seeded(seed)
        val disturbed = RandomContext.seeded(seed)

        When("only the second root consumes values from another purpose")
        Vector.fill(count)(disturbed.stream("domain.recommendation").nextLong())
        val expected = Vector.fill(count)(undisturbed.stream("domain.pricing").nextLong())
        val actual = Vector.fill(count)(disturbed.stream("domain.pricing").nextLong())

        Then("the target purpose sequence is unchanged")
        actual shouldBe expected
      }
    }

    "retain stream state across repeated lookup" in {
      forAll(_non_empty_text) { seed =>
        Given("a seeded root and a control root")
        val root = RandomContext.seeded(seed)
        val control = RandomContext.seeded(seed).stream("domain.pricing")

        When("the same named stream is obtained for each call")
        val actual = Vector(root.stream("domain.pricing").nextLong(), root.stream("domain.pricing").nextLong())
        val expected = Vector(control.nextLong(), control.nextLong())

        Then("lookup continues the existing stream rather than restarting it")
        actual shouldBe expected
      }
    }

    "canonicalize equivalent purpose paths to one advancing stream" in {
      forAll(_non_empty_text) { seed =>
        Given("a seeded root and equivalent flat, nested, and normalized purpose paths")
        val root = RandomContext.seeded(seed)

        When("the equivalent paths are resolved")
        val flat = root.stream("domain.pricing")
        val nested = root.stream("domain").stream("pricing")
        val normalized = root.stream(" Domain.Pricing ")

        Then("all paths resolve to the same stateful stream")
        nested should be theSameInstanceAs flat
        normalized should be theSameInstanceAs flat
      }
    }

    "respect positive integer bounds" in {
      forAll(Gen.choose(1, Int.MaxValue), _sample_count) { (bound, count) =>
        Given("a seeded random stream and a positive bound")
        val random = RandomContext.seeded("bounds").stream("domain.sample")

        When("bounded values are generated")
        val values = Vector.fill(count)(random.nextInt(bound))

        Then("every value is within the requested interval")
        all(values) should (be >= 0 and be < bound)
      }
    }

    "reject blank purposes and non-positive integer bounds" in {
      forAll(Gen.oneOf("", " ", "\t"), Gen.choose(Int.MinValue, 0)) { (purpose, bound) =>
        Given("a seeded context with invalid purpose and bound inputs")
        val random = RandomContext.seeded("invalid-input")

        When("a blank stream purpose or non-positive bound is used")
        val purposefailure = intercept[IllegalArgumentException](random.stream(purpose))
        val boundfailure = intercept[IllegalArgumentException](random.nextInt(bound))

        Then("both invalid arguments are rejected deterministically")
        purposefailure.getMessage should include("purpose")
        boundfailure.getMessage should include("bound")
      }
    }

    "preserve fixed context compatibility without exposing seed material" in {
      forAll(_non_empty_text) { seed =>
        Given("the legacy deterministic context and a seeded context")
        val fixed = RandomContext.from("deterministic")
        val seeded = RandomContext.seeded(seed)

        When("their public representations and values are inspected")
        val values = Vector(fixed.nextInt(), fixed.nextLong().toInt, fixed.nextInt(1))

        Then("the legacy context remains fixed")
        values shouldBe Vector(0, 0, 0)

        And("the seeded representation does not reveal its seed")
        seeded.toString shouldBe "RandomContext.Seeded"
      }
    }
  }
}
