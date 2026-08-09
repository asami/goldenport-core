package org.goldenport

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalacheck.Gen
import org.goldenport.conclusion.{Disposition, Interpretation}
import org.goldenport.observation.{Cause, Taxonomy}

/*
 * @since   May. 10, 2026
 *  version May. 11, 2026
 * @version Aug. 10, 2026
 * @author  ASAMI, Tomoharu
 */
class ErrorVocabularySpec extends AnyWordSpec with GivenWhenThen with Matchers with ScalaCheckDrivenPropertyChecks {
  "formal error vocabulary" should {
    "use canonical Taxonomy.Category names and values" in {
      Given("the closed Taxonomy.Category vocabulary")
      When("its declared identifiers are projected")
      val entries = Taxonomy.Category.values.map(x => x.name -> x.value).toVector

      Then("the stable names and numeric identities are preserved")
      entries shouldBe Vector(
        "argument" -> 1,
        "property" -> 2,
        "value" -> 3,
        "record" -> 4,
        "entity" -> 5,
        "resource" -> 6,
        "reference" -> 7,
        "state" -> 8,
        "security" -> 9,
        "operation" -> 10,
        "service" -> 11,
        "service-provider" -> 12,
        "component" -> 13,
        "subsystem" -> 14,
        "system" -> 15,
        "configuration" -> 16,
        "datastore" -> 17,
        "network" -> 18,
        "out-of-control" -> 99
      )
      entries.map(_._1).distinct shouldBe entries.map(_._1)
      entries.map(_._2).distinct shouldBe entries.map(_._2)
    }

    "use canonical Taxonomy.Symptom names and values" in {
      Given("the closed Taxonomy.Symptom vocabulary")
      When("its declared identifiers are projected")
      val entries = Taxonomy.Symptom.values.map(x => x.name -> x.value).toVector

      Then("the stable names and numeric identities are preserved")
      entries shouldBe Vector(
        "syntax-error" -> 1,
        "format-error" -> 2,
        "missing" -> 3,
        "redundant" -> 4,
        "unexpected" -> 5,
        "unsupported" -> 6,
        "domain-value" -> 7,
        "invalid" -> 8,
        "illegal" -> 9,
        "not-found" -> 10,
        "unavailable" -> 11,
        "conflict" -> 12,
        "invalid-reference" -> 13,
        "duplicate" -> 14,
        "authentication-required" -> 15,
        "permission-denied" -> 16,
        "corrupted" -> 17,
        "null-pointer" -> 90,
        "unreachable-reached" -> 91,
        "uninitialized-state" -> 92,
        "invariant-violation" -> 93,
        "precondition-violation" -> 94,
        "postcondition-violation" -> 95,
        "not-implemented" -> 96,
        "impossible-state" -> 97
      )
      entries.map(_._1).distinct shouldBe entries.map(_._1)
      entries.map(_._2).distinct shouldBe entries.map(_._2)
    }

    "render taxonomy using canonical names" in {
      Given("canonical resource and argument taxonomy values")

      When("they are rendered")

      Then("their names remain the external vocabulary")
      Taxonomy.resourceNotFound.print shouldBe "resource.not-found"
      Taxonomy.argumentSyntaxError.print shouldBe "argument.syntax-error"
      Taxonomy.argumentSyntaxError.symptom shouldBe Taxonomy.Symptom.SyntaxError
    }

    "use canonical Cause.Kind names and values" in {
      Given("the closed Cause.Kind vocabulary")
      When("its declared identifiers are projected")
      val entries = Cause.Kind.values.map(x => x.name -> x.value).toVector

      Then("new availability mechanisms append without changing existing identities")
      entries shouldBe Vector(
        "format" -> 1,
        "limit" -> 2,
        "policy" -> 3,
        "capability" -> 4,
        "permission" -> 5,
        "guard" -> 6,
        "relation" -> 7,
        "conflict" -> 8,
        "inconsistency" -> 9,
        "exhaustion" -> 10,
        "timeout" -> 11,
        "corruption" -> 12,
        "not-running" -> 13,
        "connection-refused" -> 14,
        "unreachable" -> 15,
        "unknown" -> 99
      )
      entries.map(_._1).distinct shouldBe entries.map(_._1)
      entries.map(_._2).distinct shouldBe entries.map(_._2)
    }

    "round-trip every generated Cause.Kind selection without renumbering it" in {
      Given("the closed Cause.Kind vocabulary")
      val kindgen = Gen.oneOf(Cause.Kind.values.toSeq)

      When("generated vocabulary entries are projected and resolved by name")
      forAll(kindgen) { kind =>
        val resolved = Cause.Kind.values.find(_.name == kind.name)

        Then("each identity is unique and preserves its declared numeric value")
        resolved shouldBe Some(kind)
        resolved.map(_.value) shouldBe Some(kind.value)
        Cause.Kind.values.count(_.value == kind.value) shouldBe 1
      }
    }

    "use canonical conclusion-side classification values" in {
      Given("the closed conclusion interpretation and disposition vocabularies")

      When("their identifiers are projected")

      Then("their pre-existing numeric values remain stable")
      Interpretation.Kind.values.map(x => x.name -> x.value).toVector shouldBe Vector(
        "success" -> 1,
        "domain-failure" -> 2,
        "defect" -> 3,
        "configuration-failure" -> 4,
        "system-failure" -> 5,
        "network-failure" -> 6,
        "external-service-failure" -> 7
      )
      Disposition.UserAction.values.map(x => x.name -> x.value).toVector shouldBe Vector(
        "fix-input" -> 1,
        "retry-now" -> 2,
        "retry-later" -> 3,
        "escalation" -> 4
      )
      Disposition.Responsibility.values.map(x => x.name -> x.value).toVector shouldBe Vector(
        "user" -> 1,
        "application-admin" -> 2,
        "system-admin" -> 3,
        "developer" -> 4
      )
    }
  }
}
