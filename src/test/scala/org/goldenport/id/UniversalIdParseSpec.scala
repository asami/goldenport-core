package org.goldenport.id

import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import org.goldenport.Consequence

/*
 * @since   Mar. 17, 2026
 * @version Mar. 17, 2026
 * @author  ASAMI, Tomoharu
 */
class UniversalIdParseSpec
  extends AnyWordSpec
    with GivenWhenThen
    with Matchers
    with ScalaCheckDrivenPropertyChecks {

  "UniversalId.parse" should {
    "parse canonical id with expected kind" in {
      Given("a canonical id with kind trace")
      val value = "svc_mod-op_mod-trace-1700000000000-abc123"

      When("parsing with expected kind")
      val result = UniversalId.parse(value, "trace") { parts =>
        (parts.major, parts.minor, parts.kind, parts.subkind)
      }

      Then("major/minor/kind are extracted")
      result shouldBe Consequence.success(("svc_mod", "op_mod", "trace", None))
    }

    "parse canonical id with subkind" in {
      Given("a canonical id including subkind")
      val value = "svc-op-event-created-1700000000000-abc123"

      When("parsing with expected kind")
      val result = UniversalId.parseParts(value, "event")

      Then("subkind is preserved")
      result.map(_.subkind) shouldBe Consequence.success(Some("created"))
    }

    "fail when kind does not match expected kind" in {
      Given("a canonical id with kind span")
      val value = "svc-op-span-1700000000000-abc123"

      When("parsing as trace")
      val result = UniversalId.parseParts(value, "trace")

      Then("parsing fails")
      result shouldBe a[Consequence.Failure[?]]
    }

    "parse labels property-wise" in {
      Given("label-safe major/minor strings")
      val labelgen = Gen.chooseNum(1, Int.MaxValue).map(i => s"x_$i")

      forAll(labelgen, labelgen) { (major, minor) =>
        val value = s"$major-$minor-trace-1700000000000-abc123"

        When("parsing the canonical id")
        val result = UniversalId.parseParts(value, "trace")

        Then("the same labels are restored")
        result match {
          case Consequence.Success(parts) =>
            parts.major shouldBe major
            parts.minor shouldBe minor
          case f: Consequence.Failure[?] =>
            fail(s"unexpected failure: ${f.conclusion.print}")
        }
      }
    }
  }
}
