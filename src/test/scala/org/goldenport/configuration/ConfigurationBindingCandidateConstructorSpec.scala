package org.goldenport.configuration

import org.goldenport.Consequence
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Aug.  2, 2026
 * @version Aug.  2, 2026
 * @author  ASAMI, Tomoharu
 */
final class ConfigurationBindingCandidateConstructorSpec
  extends AnyWordSpec
    with Matchers
    with GivenWhenThen {
  private val _e1 = afterWord(
    "in spec:phase-55-candidate-constructor, example:E1, rules:GCF04-R5,R6,R7, phase:55, slice:GCF-04"
  )
  private val _e2 = afterWord(
    "in spec:phase-55-candidate-constructor, example:E2, rules:GCF04-R6, phase:55, slice:GCF-04"
  )

  "ConfigurationBindingCandidateConstructor" should {
    "decode a witnessed typed value and preserve source provenance" which {
      "E1 construct an unresolved candidate without selecting a winner" must _e1 {
        "when a String raw value is admitted through its parameter witness" in {
          Given("one source snapshot and one exact String parameter witness")
          val parameter = _parameter
          val snapshot = _snapshot("home")
          val input = _take(
            ConfigurationBindingCandidateInput.typed(
              parameter,
              "global",
              ConfigurationValue.StringValue("production"),
              Some("global.config.textus.application.mode"),
              Some("textus.application.mode"),
              Vector("default: production"),
              isConfidential = false
            )
          )

          When("candidate construction runs")
          val candidates = _take(
            ConfigurationBindingCandidateConstructor.construct(
              Vector(_take(ConfigurationBindingCandidateBatch.create(snapshot, Vector(input))))
            )
          )

          Then("the typed candidate carries its original source details without resolution")
          candidates.bindings.size shouldBe 1
          val candidate = candidates.bindings.head
          candidate.value shouldBe "production"
          candidate.provenance.layer shouldBe "home"
          candidate.provenance.inputPath shouldBe Some("global.config.textus.application.mode")
          candidate.provenance.inputSpelling shouldBe Some("textus.application.mode")
          candidate.provenance.evidence shouldBe Vector("default: production")
        }
      }
    }

    "reject same-domain canonical target duplicates" which {
      "E2 reject aliases or repeated fields before they become overrides" must _e2 {
        "when two source batches in one collision domain define one parameter and target" in {
          Given("two same-layer source snapshots and one parameter witness")
          val parameter = _parameter
          val first = _input(parameter, "first")
          val second = _input(parameter, "second")

          When("the constructor receives both candidates")
          val result = ConfigurationBindingCandidateConstructor.construct(
            Vector(
              _take(ConfigurationBindingCandidateBatch.create(_snapshot("home", "one"), Vector(first))),
              _take(ConfigurationBindingCandidateBatch.create(_snapshot("home", "two"), Vector(second)))
            )
          )

          Then("it returns a structured configuration failure instead of an invisible override")
          result.isSuccess shouldBe false
          _is_configuration_invalid(result) shouldBe true
        }
      }
    }
  }

  private def _parameter: ConfigurationParameter[String] =
    _take(ConfigurationParameter.create(_take(CanonicalParameterId.parse("textus.application.mode")), ConfigurationValueCodec.string))

  private def _input(
    parameter: ConfigurationParameter[String],
    value: String
  ): ConfigurationBindingCandidateInput[String] =
    _take(
      ConfigurationBindingCandidateInput.typed(
        parameter,
        "global",
        ConfigurationValue.StringValue(value),
        Some("global.config.textus.application.mode"),
        Some("textus.application.mode"),
        Vector.empty,
        isConfidential = false
      )
    )

  private def _snapshot(
    layer: String,
    source: String = "source"
  ): ConfigurationSourceSnapshot[ConfigurationDocument] =
    _take(
      ConfigurationSourceSnapshots.load(
        Vector(
          _take(
            ConfigurationSourceAdmission.create(
              ConfigurationOrigin.Home,
              layer,
              source,
              10,
              layer,
              () => Consequence.success(ConfigurationDocument.Scalar(ConfigurationValue.StringValue("fixture")))
            )
          )
        )
      )
    ).snapshots.head

  private def _take[A](result: Consequence[A]): A =
    result.getOrElse(fail(result.display))

  private def _is_configuration_invalid(result: Consequence[?]): Boolean =
    result match {
      case Consequence.Failure(conclusion) => conclusion.observation.taxonomy.category.name == "configuration"
      case _ => false
    }
}
