package org.goldenport.consequence

import org.goldenport.observation.Severity
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Dec. 30, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
class SeverityAggregationSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks {

  private def _higher_or_equal(x: Severity, y: Severity): Boolean =
    x.ordinal <= y.ordinal

  "Severity aggregation" should {
    "use the maximum severity as a semantic rule" in {
      Given("ordered severities")
      When("aggregating explicit pairs")
      Then("the max severity is selected")
      Severity.max(Severity.Info, Severity.Warn).shouldBe(Severity.Warn)
      Severity.max(Severity.Warn, Severity.Error).shouldBe(Severity.Error)
      Severity.max(Severity.Error, Severity.Error).shouldBe(Severity.Error)
      Severity.max(Severity.Info, Severity.Info).shouldBe(Severity.Info)
    }

    "be commutative and monotone" in {
      Given("arbitrary severities")
      val gen = Gen.oneOf(Severity.values.toSeq)
      forAll(gen, gen) { (a, b) =>
        When("aggregating in both orders")
        val ab = Severity.max(a, b)
        val ba = Severity.max(b, a)
        Then("order does not matter and max is monotone")
        ab.shouldBe(ba)
        _higher_or_equal(ab, a).shouldBe(true)
        _higher_or_equal(ab, b).shouldBe(true)
      }
    }
  }
}
