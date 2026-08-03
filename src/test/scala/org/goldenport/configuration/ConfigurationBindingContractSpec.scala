package org.goldenport.configuration

import org.goldenport.configuration.ConfigurationBindingScenarioReport.NotImplemented
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Aug.  2, 2026
 * @version Aug.  2, 2026
 * @author  ASAMI, Tomoharu
 */
final class ConfigurationBindingContractSpec
  extends AnyWordSpec
    with Matchers
    with GivenWhenThen {
  private val _e1 = afterWord(
    "in spec:phase-55-typed-configuration-binding, example:E1, rules:GCF02-R1,R2,R3,R4,R5,R6, phase:55, slice:GCF-02"
  )
  private val _e2 = afterWord(
    "in spec:phase-55-typed-configuration-binding, example:E2, rules:GCF02-R7,R8,R9,R10,R11,R12, phase:55, slice:GCF-02"
  )
  private val _e3 = afterWord(
    "in spec:phase-55-typed-configuration-binding, example:E3, rules:GCF02-R13,R14,R15, phase:55, slice:GCF-02"
  )

  private val _typed_core_scenarios = Vector(
    "parameter-codec-value-type-coupling",
    "validated-parameter-target-provenance-candidate-binding-construction",
    "candidate-effective-separation-without-history",
    "candidate-multiplicity-resolved-uniqueness",
    "same-parameter-type-direct-acyclic-complete-history",
    "exclude-mismatched-duplicate-invalid-context-ineligible-candidates"
  )
  private val _resolution_scenarios = Vector(
    "one-physical-source-load-immutable-snapshot",
    "same-source-target-specificity-ordering",
    "cross-source-rank-dominates-specificity",
    "independent-subsystem-component-resolution-from-immutable-candidates",
    "typed-parameter-lookup-without-winning-target",
    "no-second-authoritative-string-lookup-store"
  )
  private val _diagnostic_scenarios = Vector(
    "trace-derived-from-effective-binding-without-string-authority",
    "confidential-winner-and-history-redaction",
    "newest-sixteen-history-256-character-source-cap-truncation-count"
  )

  "Phase 55 generic configuration binding" should {
    "route typed-core and candidate scenarios through the production SPI" which {
      "E1 preserve the registered invariant identity while behavior is deferred" must _e1 {
        "when parameter, candidate, binding, and history requests are evaluated" in {
          Given("the non-authoritative GCF-02 generic scenario SPI")

          When("each invariant scenario is sent to the production SPI")
          val reports = _typed_core_scenarios.map(_evaluate)

          Then("every report remains attributable to exactly its scenario")
          reports shouldBe _typed_core_scenarios.map(NotImplemented.apply)
        }

        "when GCF-03 and GCF-05 satisfy every typed-core invariant" in {
          Given("the registered typed-core scenarios")

          When("their behavior becomes authoritative")

          Then("none remains a GCF-02 deferred report")
          pendingUntilFixed {
            _typed_core_scenarios.map(_evaluate).exists(_.isInstanceOf[NotImplemented]) shouldBe false
          }
        }
      }
    }

    "route immutable-snapshot and resolution scenarios through the production SPI" which {
      "E2 preserve source, target, and lookup invariants while behavior is deferred" must _e2 {
        "when source and selected-context requests are evaluated" in {
          Given("one generic SPI without a String-keyed binding authority")

          When("each resolution scenario is sent to the production SPI")
          val reports = _resolution_scenarios.map(_evaluate)

          Then("every report retains the request scenario identity")
          reports shouldBe _resolution_scenarios.map(NotImplemented.apply)
        }

        "when GCF-04 through GCF-06 satisfy every resolution invariant" in {
          Given("the registered resolution scenarios")

          When("their behavior becomes authoritative")

          Then("none remains a GCF-02 deferred report")
          pendingUntilFixed {
            _resolution_scenarios.map(_evaluate).exists(_.isInstanceOf[NotImplemented]) shouldBe false
          }
        }
      }
    }

    "route derived-diagnostic scenarios through the production SPI" which {
      "E3 preserve trace and redaction invariants while behavior is deferred" must _e3 {
        "when diagnostic requests are evaluated" in {
          Given("the generic SPI has no trace, redaction, or projection authority")

          When("each diagnostic scenario is sent to the production SPI")
          val reports = _diagnostic_scenarios.map(_evaluate)

          Then("every report retains the request scenario identity")
          reports shouldBe _diagnostic_scenarios.map(NotImplemented.apply)
        }

        "when GCF-06 satisfies every derived diagnostic invariant" in {
          Given("the registered diagnostic scenarios")

          When("their behavior becomes authoritative")

          Then("none remains a GCF-02 deferred report")
          pendingUntilFixed {
            _diagnostic_scenarios.map(_evaluate).exists(_.isInstanceOf[NotImplemented]) shouldBe false
          }
        }
      }
    }
  }

  private def _evaluate(scenarioid: String): ConfigurationBindingScenarioReport =
    ConfigurationBindingScenarioSpi.notImplemented[String].evaluate(
      _Request(scenarioid)
    )

  private final case class _Request(
    scenarioId: String
  ) extends ConfigurationBindingScenarioRequest[String]
}
