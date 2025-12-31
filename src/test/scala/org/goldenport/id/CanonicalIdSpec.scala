package org.goldenport.id

import java.time.{Clock, Instant, ZoneOffset}

import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class CanonicalIdSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks {

  private val _clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC)

  "CanonicalId" should {
    "generate a deterministic value with fixed clock and entropy" in {
      Given("a fixed entropy source and fixed clock")
      val entropy = new EntropySource {
        def next(): String = "entropy"
      }
      val generator = new DefaultCanonicalIdGenerator(entropy)

      When("generating a canonical id")
      val gen = Gen.const(())
      forAll(gen) { _ =>
        val id = generator.generate("svc", "op", "exec", _clock)
        Then("the generated value matches the expected format")
        id.value.shouldBe("svc-op-exec-20250101T000000Z-entropy")
      }
    }
  }
}
