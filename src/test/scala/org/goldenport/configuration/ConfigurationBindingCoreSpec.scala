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
final class ConfigurationBindingCoreSpec
  extends AnyWordSpec
    with Matchers
    with GivenWhenThen {
  private val _e1 = afterWord(
    "in spec:phase-55-typed-configuration-binding, example:E1, rules:GCF03-R1,R2,R3, phase:55, slice:GCF-03"
  )
  private val _e2 = afterWord(
    "in spec:phase-55-typed-configuration-binding, example:E2, rules:GCF03-R4,R5,R6, phase:55, slice:GCF-03"
  )
  private val _e3 = afterWord(
    "in spec:phase-55-typed-configuration-binding, example:E3, rules:GCF03-R7,R8,R9, phase:55, slice:GCF-03"
  )
  private val _e4 = afterWord(
    "in spec:phase-55-typed-configuration-binding, example:E4, rules:GCF03-R10,R11, phase:55, slice:GCF-03"
  )
  private val _e5 = afterWord(
    "in spec:phase-55-typed-configuration-binding, example:E5, rules:GCF03-R12, phase:55, slice:GCF-03"
  )

  "Configuration binding core" should {
    "validate typed parameter and provenance construction" which {
      "E1 accept only canonical identity grammar and strict codecs" must _e1 {
        "when a String parameter is created" in {
          Given("a canonical generic parameter id and string codec")
          val id = _take(CanonicalParameterId.parse("textus.application.mode"))

          When("the parameter codec round-trips a StringValue")
          val parameter = _take(ConfigurationParameter.create(id, ConfigurationValueCodec.string))
          val encoded = _take(parameter.codec.encode("production"))

          Then("the value remains typed without coercion")
          encoded shouldBe ConfigurationValue.StringValue("production")
          _take(parameter.codec.decode(encoded)) shouldBe "production"
          _take(ConfigurationValueCodec.boolean.decode(ConfigurationValue.BooleanValue(true))) shouldBe true
          _take(ConfigurationValueCodec.bigDecimal.decode(ConfigurationValue.NumberValue(BigDecimal(42)))) shouldBe BigDecimal(42)
          ConfigurationValueCodec.string.decode(ConfigurationValue.BooleanValue(true)).isSuccess shouldBe false
          ConfigurationValueCodec.boolean.decode(ConfigurationValue.StringValue("true")).isSuccess shouldBe false
          ConfigurationValueCodec.bigDecimal.decode(ConfigurationValue.StringValue("42")).isSuccess shouldBe false
          CanonicalParameterId.parse("Textus.application.mode").isSuccess shouldBe false
          CanonicalParameterId.parse("textus").isSuccess shouldBe false
        }
      }

      "E2 bound construction-time evidence independently from projection" must _e2 {
        "when provenance receives seventeen ordered evidence entries" in {
          Given("valid source identity and the accepted 16/256 construction bound")
          val evidence = Vector.fill(16)("evidence") :+ "omitted"

          When("provenance is constructed")
          val provenance = _take(_provenance(evidence))

          Then("it retains stable first entries and records the omission")
          provenance.evidence.size shouldBe 16
          provenance.omittedEvidenceCount shouldBe 1
        }

        "when evidence would split a UTF-16 surrogate pair at the bound" in {
          Given("a 255-code-unit prefix followed by one surrogate pair")
          val value = "x" * 255 + "😀"

          When("provenance is constructed")
          val provenance = _take(_provenance(Vector(value)))

          Then("the retained evidence has no dangling surrogate")
          provenance.evidence.head.length shouldBe 255
          Character.isHighSurrogate(provenance.evidence.head.last) shouldBe false
        }
      }
    }

    "separate candidates from effective bindings" which {
      "E3 retain multiplicity while enforcing same-witness direct history" must _e3 {
        "when two candidates share one typed parameter and different targets" in {
          Given("a parameter, provenance, and two target-specific candidates")
          val parameter = _parameter
          val firstcandidate = _candidate(parameter, "global", "first")
          val secondcandidate = _candidate(parameter, "component", "second")

          When("candidates and a direct override are constructed")
          val candidates = _take(ConfigurationBindingCandidates.from(Vector(firstcandidate, secondcandidate)))
          val first = _take(ConfigurationBinding.initial(firstcandidate))
          val second = _take(ConfigurationBinding.overrideWith(secondcandidate, first))

          Then("candidate multiplicity and one immutable direct history link are preserved")
          candidates.bindings.size shouldBe 2
          second.overridden shouldBe Some(first)
          second.target shouldBe "component"
        }

        "when a matching id uses another parameter witness" in {
          Given("two separately constructed parameters with the same canonical id")
          val original = _parameter
          val other = _parameter
          val first = _take(ConfigurationBinding.initial(_candidate(original, "global", "first")))

          When("the other witness attempts an override")
          val result = ConfigurationBinding.overrideWith(_candidate(other, "component", "second"), first)

          Then("construction fails with the configuration taxonomy")
          result.isSuccess shouldBe false
          _is_configuration_invalid(result) shouldBe true
        }

        "when a candidate value is rejected by its parameter codec" in {
          Given("a parameter whose codec rejects every encoded value")
          val rejecting = new ConfigurationValueCodec[String] {
            def decode(value: ConfigurationValue): Consequence[String] =
              Consequence.configurationInvalid("not admitted")

            def encode(value: String): Consequence[ConfigurationValue] =
              Consequence.configurationInvalid("not admitted")
          }
          val parameter = _take(
            ConfigurationParameter.create(
              _take(CanonicalParameterId.parse("textus.application.mode")),
              rejecting
            )
          )

          When("candidate construction validates its typed value")
          val result = ConfigurationBindingCandidate.create(
            parameter,
            "global",
            "value",
            _take(_provenance(Vector("evidence")))
          )

          Then("the codec failure is retained as a structured configuration failure")
          result.isSuccess shouldBe false
          _is_configuration_invalid(result) shouldBe true
        }
      }
    }

    "publish one exact-witness typed lookup authority" which {
      "E4 reject duplicate effective ids and String-free witness substitution" must _e4 {
        "when a resolved collection receives duplicate canonical ids" in {
          Given("two bindings from distinct parameter witnesses with one id")
          val first = _take(ConfigurationBinding.initial(_candidate(_parameter, "global", "first")))
          val second = _take(ConfigurationBinding.initial(_candidate(_parameter, "component", "second")))

          When("the collection is constructed")
          val result = ConfigurationBindingCollection.from(Vector(first, second))

          Then("it fails structurally")
          result.isSuccess shouldBe false
          _is_configuration_invalid(result) shouldBe true
        }

        "when the original parameter witness performs lookup" in {
          Given("one effective String binding")
          val parameter = _parameter
          val binding = _take(ConfigurationBinding.initial(_candidate(parameter, "global", "value")))
          val collection = _take(ConfigurationBindingCollection.from(Vector(binding)))

          When("the original and another same-id witness look up the value")
          val actual = collection.value(parameter)
          val substituted = collection.value(_parameter)

          Then("only the original witness receives the typed value")
          _take(actual) shouldBe Some("value")
          substituted.isSuccess shouldBe false
          _is_configuration_invalid(substituted) shouldBe true
        }
      }

      "E5 provide canonical typed empty collections" must _e5 {
        "when candidates and resolved bindings are constructed without entries" in {
          Given("the Collection idiom canonical empty APIs")

          When("each zero-argument factory is evaluated")
          val candidates = ConfigurationBindingCandidates[String]()
          val collection = ConfigurationBindingCollection[String]()

          Then("both return their typed canonical empty values")
          candidates shouldBe ConfigurationBindingCandidates.empty[String]
          candidates.bindings shouldBe Vector.empty
          _take(collection.binding(_parameter)) shouldBe None
          collection shouldBe ConfigurationBindingCollection.empty[String]
        }
      }
    }
  }

  private def _parameter: ConfigurationParameter[String] =
    _take(
      ConfigurationParameter.create(
        _take(CanonicalParameterId.parse("textus.application.mode")),
        ConfigurationValueCodec.string
      )
    )

  private def _candidate(
    parameter: ConfigurationParameter[String],
    target: String,
    value: String
  ): ConfigurationBindingCandidate[String, String] =
    _take(ConfigurationBindingCandidate.create(parameter, target, value, _take(_provenance(Vector("evidence")))))

  private def _provenance(
    evidence: Vector[String]
  ): Consequence[ConfigurationProvenance] =
    ConfigurationProvenance.create(
      ConfigurationOrigin.Home,
      "home",
      "file:/home/user/.textus/config.yaml",
      Some("config.yaml"),
      Some("textus.application.mode"),
      1,
      0,
      evidence,
      isConfidential = false
    )

  private def _take[A](result: Consequence[A]): A =
    result.getOrElse(fail(result.display))

  private def _is_configuration_invalid(result: Consequence[?]): Boolean =
    result match {
      case Consequence.Failure(conclusion) => conclusion.observation.taxonomy.category.name == "configuration"
      case _ => false
    }
}
