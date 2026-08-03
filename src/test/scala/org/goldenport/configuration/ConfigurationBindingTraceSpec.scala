package org.goldenport.configuration

import org.goldenport.Consequence
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Aug.  3, 2026
 * @version Aug.  3, 2026
 * @author  ASAMI, Tomoharu
 */
final class ConfigurationBindingTraceSpec
  extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckDrivenPropertyChecks {
  private val _e1 = afterWord(
    "in spec:phase-55-binding-trace, example:E1, rules:GCF06-R1,R2,R3, phase:55, slice:GCF-06-I"
  )
  private val _e2 = afterWord(
    "in spec:phase-55-binding-trace, example:E2, rules:GCF06-R3,R4,R5, phase:55, slice:GCF-06-I"
  )
  private val _e3 = afterWord(
    "in spec:phase-55-binding-trace, example:E3, rules:GCF06-R6, phase:55, slice:GCF-06-I"
  )
  private val _e4 = afterWord(
    "in spec:phase-55-binding-trace, example:E4, rules:GCF06-R7, phase:55, slice:GCF-06-I"
  )
  private val _e5 = afterWord(
    "in spec:phase-55-binding-trace, example:E5, rules:GCF06-R2,R7, phase:55, slice:GCF-06-I"
  )
  private val _e6 = afterWord(
    "in spec:phase-55-binding-trace, example:E6, rules:GCF06-R1,R3, phase:55, slice:GCF-06-I"
  )

  "ConfigurationBindingTrace" should {
    "derive deterministic typed explanations from resolved bindings alone" which {
      "E1 retain the canonical key, typed target, encoded value, and winning provenance" must _e1 {
        "when one public effective binding is projected" in {
          Given("one String parameter witness and one public effective binding")
          val parameter = _parameter("textus.application.mode")
          val binding = _take(ConfigurationBinding.initial(_candidate(parameter, "global", "production", 10, 0)))
          val collection = _take(ConfigurationBindingCollection.from(Vector(binding)))

          When("the resolved collection creates its trace")
          val trace = _take(ConfigurationBindingTrace.from(collection))
          val explanation = _take(trace.explain(parameter)).get

          Then("every explanation field derives from the one effective binding")
          explanation.parameterId.value shouldBe "textus.application.mode"
          explanation.effective.target shouldBe "global"
          explanation.effective.value shouldBe ConfigurationBindingTraceValue.Visible(ConfigurationValue.StringValue("production"))
          explanation.effective.provenance.sourceRank shouldBe 10
          explanation.effective.provenance.sourceOrdinal shouldBe 0
          explanation.entries.size shouldBe 1
          explanation.omittedEntryCount shouldBe 0
          val substituted = trace.explain(_parameter("textus.application.mode"))
          substituted.isSuccess shouldBe false
          _is_configuration_invalid(substituted) shouldBe true
        }
      }

      "E2 retain only direct override history in newest-first order" must _e2 {
        "when two source winners form one effective binding chain" in {
          Given("one parameter witness and two source winners with different ranks")
          val parameter = _parameter("textus.application.mode")
          val earlier = _take(ConfigurationBinding.initial(_candidate(parameter, "global", "development", 10, 0)))
          val winner = _take(ConfigurationBinding.overrideWith(_candidate(parameter, "instance", "production", 20, 0), earlier))

          When("the winner collection is projected")
          val explanation = _take(_take(ConfigurationBindingTrace.from(_take(ConfigurationBindingCollection.from(Vector(winner))))).explain(parameter)).get

          Then("the current winner precedes only its direct predecessor")
          explanation.entries.map(_.target) shouldBe Vector("instance", "global")
          explanation.entries.map(_.value) shouldBe Vector(
            ConfigurationBindingTraceValue.Visible(ConfigurationValue.StringValue("production")),
            ConfigurationBindingTraceValue.Visible(ConfigurationValue.StringValue("development"))
          )
        }
      }

      "E6 sort every explanation by canonical parameter identity independently of collection order" must _e6 {
        "when arbitrary permutations construct one resolved multi-parameter collection" in {
          Given("three exact parameter witnesses with canonical ids in non-canonical insertion order")
          val parameters = Vector(
            _parameter("textus.application.zeta"),
            _parameter("textus.application.alpha"),
            _parameter("textus.application.middle")
          )
          val bindings = parameters.map { parameter =>
            _take(ConfigurationBinding.initial(_candidate(parameter, "global", parameter.id.value, 10, 0)))
          }
          val expected = Vector(
            "textus.application.alpha",
            "textus.application.middle",
            "textus.application.zeta"
          )

          When("every candidate vector permutation becomes a resolved collection")
          forAll(org.scalacheck.Gen.oneOf(
            Vector(0, 1, 2), Vector(0, 2, 1), Vector(1, 0, 2),
            Vector(1, 2, 0), Vector(2, 0, 1), Vector(2, 1, 0)
          )) { order =>
            val trace = _take(ConfigurationBindingTrace.from(_take(ConfigurationBindingCollection.from(order.map(bindings)))))

            Then("explanations and exact-witness lookup remain canonical and independent of insertion order")
            trace.entries.map(_.parameterId.value) shouldBe expected
            parameters.foreach { parameter =>
              _take(trace.explain(parameter)).map(_.effective.value) shouldBe Some(
                ConfigurationBindingTraceValue.Visible(ConfigurationValue.StringValue(parameter.id.value))
              )
            }
          }
        }
      }
    }

    "sanitize bounded diagnostic history" which {
      "E3 retain sixteen newest entries and record the omitted count" must _e3 {
        "when a runtime override chain has seventeen entries" in {
          Given("one complete immutable seventeen-entry runtime chain")
          val parameter = _parameter("textus.application.mode")
          val chain = (0 to 16).foldLeft(Option.empty[ConfigurationBinding[String, String]]) { (previous, index) =>
            val candidate = _candidate(parameter, s"target-$index", s"value-$index", index, 0)
            Some(previous.fold(_take(ConfigurationBinding.initial(candidate)))(x => _take(ConfigurationBinding.overrideWith(candidate, x))))
          }.get

          When("the chain is projected for diagnostics")
          val explanation = _take(_take(ConfigurationBindingTrace.from(_take(ConfigurationBindingCollection.from(Vector(chain))))).explain(parameter)).get

          Then("the projection is bounded without truncating the runtime chain")
          explanation.entries.size shouldBe 16
          explanation.entries.head.target shouldBe "target-16"
          explanation.entries.last.target shouldBe "target-1"
          explanation.omittedEntryCount shouldBe 1
          _chain_size(chain) shouldBe 17
        }
      }

      "E4 structurally redact confidential current and historical values" must _e4 {
        "when both bindings are marked confidential" in {
          Given("a confidential winner and confidential direct predecessor")
          val parameter = _parameter("textus.application.secret")
          val previous = _take(ConfigurationBinding.initial(_candidate(parameter, "global", "old-secret", 10, 0, isconfidential = true)))
          val winner = _take(ConfigurationBinding.overrideWith(_candidate(parameter, "instance", "new-secret", 20, 0, isconfidential = true), previous))

          When("the confidential chain is projected")
          val explanation = _take(_take(ConfigurationBindingTrace.from(_take(ConfigurationBindingCollection.from(Vector(winner))))).explain(parameter)).get

          Then("neither projected entry retains an encoded secret value")
          explanation.entries.map(_.value) shouldBe Vector(
            ConfigurationBindingTraceValue.Redacted,
            ConfigurationBindingTraceValue.Redacted
          )
          explanation.toString.contains("new-secret") shouldBe false
          explanation.toString.contains("old-secret") shouldBe false
        }
      }

      "E5 bound source identity without losing public/confidential history order" must _e5 {
        "when one public winner overrides one confidential predecessor with a long source identity" in {
          Given("a mixed-confidentiality chain and a source identity longer than 256 code units")
          val parameter = _parameter("textus.application.mode")
          val previous = _take(ConfigurationBinding.initial(_candidate(parameter, "global", "secret", 10, 0, isconfidential = true)))
          val winner = _take(ConfigurationBinding.overrideWith(_candidate(parameter, "instance", "visible", 20, 0, sourceidentity = "x" * 255 + "😀"), previous))

          When("the collection projects its diagnostic explanation")
          val explanation = _take(_take(ConfigurationBindingTrace.from(_take(ConfigurationBindingCollection.from(Vector(winner))))).explain(parameter)).get

          Then("public data remains visible while confidential history remains redacted and source text is bounded")
          explanation.entries.map(_.value) shouldBe Vector(
            ConfigurationBindingTraceValue.Visible(ConfigurationValue.StringValue("visible")),
            ConfigurationBindingTraceValue.Redacted
          )
          explanation.effective.provenance.sourceIdentity.length shouldBe 255
          explanation.effective.provenance.sourceIdentityTruncatedCount shouldBe 2
          Character.isHighSurrogate(explanation.effective.provenance.sourceIdentity.last) shouldBe false
        }
      }
    }
  }

  private def _parameter(id: String): ConfigurationParameter[String] =
    _take(ConfigurationParameter.create(_take(CanonicalParameterId.parse(id)), ConfigurationValueCodec.string))

  private def _candidate(
    parameter: ConfigurationParameter[String],
    target: String,
    value: String,
    rank: Int,
    ordinal: Int,
    isconfidential: Boolean = false,
    sourceidentity: String = "source"
  ): ConfigurationBindingCandidate[String, String] =
    _take(
      for {
        provenance <- ConfigurationProvenance.create(
          ConfigurationOrigin.Home,
          "home",
          sourceidentity,
          Some("config.yaml"),
          Some(parameter.id.value),
          rank,
          ordinal,
          Vector("evidence"),
          isconfidential
        )
        candidate <- ConfigurationBindingCandidate.create(parameter, target, value, provenance)
      } yield candidate
    )

  private def _chain_size(binding: ConfigurationBinding[String, String]): Int =
    1 + binding.overridden.map(_chain_size).getOrElse(0)

  private def _take[A](result: Consequence[A]): A =
    result.getOrElse(fail(result.display))

  private def _is_configuration_invalid(result: Consequence[?]): Boolean =
    result match {
      case Consequence.Failure(conclusion) => conclusion.observation.taxonomy.category.name == "configuration"
      case _ => false
    }
}
