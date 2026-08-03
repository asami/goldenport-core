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
final class ConfigurationBindingResolverSpec
  extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckDrivenPropertyChecks {
  private val _e1 = afterWord(
    "in spec:phase-55-binding-resolution, example:E1, rules:GCF05-R1,R2,R3, phase:55, slice:GCF-05"
  )
  private val _e2 = afterWord(
    "in spec:phase-55-binding-resolution, example:E2, rules:GCF05-R4,R5,R6, phase:55, slice:GCF-05"
  )
  private val _e3 = afterWord(
    "in spec:phase-55-binding-resolution, example:E3, rules:GCF05-R3,R7, phase:55, slice:GCF-05"
  )
  private val _e4 = afterWord(
    "in spec:phase-55-binding-resolution, example:E4, rules:GCF05-R1,R6, phase:55, slice:GCF-05"
  )
  private val _e5 = afterWord(
    "in spec:phase-55-binding-resolution, example:E5, rules:GCF05-R1,R6, phase:55, slice:GCF-05"
  )
  private val _e6 = afterWord(
    "in spec:phase-55-binding-resolution, example:E6, rules:GCF05-R6, phase:55, slice:GCF-05"
  )
  private val _e7 = afterWord(
    "in spec:phase-55-binding-resolution, example:E7, rules:GCF05-R4,R6, phase:55, slice:GCF-05"
  )
  private val _e8 = afterWord(
    "in spec:phase-55-binding-resolution, example:E8, rules:GCF05-R3, phase:55, slice:GCF-05"
  )
  private val _e9 = afterWord(
    "in spec:phase-55-binding-resolution, example:E9, rules:GCF05-R1, phase:55, slice:GCF-05"
  )
  private val _e10 = afterWord(
    "in spec:phase-55-binding-resolution, example:E10, rules:GCF05-R4,R6, phase:55, slice:GCF-05"
  )
  private val _e11 = afterWord(
    "in spec:phase-55-binding-resolution, example:E11, rules:GCF05-R3,R6, phase:55, slice:GCF-05"
  )

  "ConfigurationBindingResolver" should {
    "resolve only the supplied ordered target context" which {
      "E1 select the most specific candidate from one physical source" must _e1 {
        "when one source defines every eligible target" in {
          Given("one parameter witness, four ordered targets, and one physical source")
          val parameter = _parameter
          val context = _context(Vector("global", "class", "subsystem", "instance"))
          val candidates = _candidates(Vector(
            _candidate(parameter, "instance", "instance", 10, 0),
            _candidate(parameter, "global", "global", 10, 0),
            _candidate(parameter, "subsystem", "subsystem", 10, 0),
            _candidate(parameter, "class", "class", 10, 0)
          ))

          When("resolution selects one winner per physical source")
          val binding = _take(ConfigurationBindingResolver.resolve(candidates, context)).binding(parameter)

          Then("the most specific target wins without preserving same-source losers")
          _take(binding).map(_.value) shouldBe Some("instance")
          _take(binding).flatMap(_.overridden) shouldBe None
        }
      }

      "E2 apply only direct per-source winners in source ordering" must _e2 {
        "when higher-ranked Global input follows a lower-ranked ComponentInstance input" in {
          Given("two physical sources with otherwise opposed target specificity")
          val parameter = _parameter
          val context = _context(Vector("global", "class", "subsystem", "instance"))
          val candidates = _candidates(Vector(
            _candidate(parameter, "instance", "lower-instance", 10, 0),
            _candidate(parameter, "global", "higher-global", 20, 0),
            _candidate(parameter, "class", "same-source-loser", 10, 0)
          ))

          When("the sources are resolved from lower to higher rank")
          val binding = _take(_take(ConfigurationBindingResolver.resolve(candidates, context)).binding(parameter)).get

          Then("the higher source overrides the earlier source winner directly")
          binding.value shouldBe "higher-global"
          binding.overridden.map(_.value) shouldBe Some("lower-instance")
          binding.overridden.flatMap(_.overridden) shouldBe None
        }
      }

    }

    "preserve deterministic boundaries" which {
      "E3 ignore ineligible targets for independent contexts" must _e3 {
        "when two contexts share candidate input" in {
          Given("two eligible targets and one foreign target in an immutable candidate collection")
          val parameter = _parameter
          val candidates = _candidates(Vector(
            _candidate(parameter, "global", "global", 10, 0),
            _candidate(parameter, "instance-a", "first", 20, 0),
            _candidate(parameter, "instance-b", "second", 20, 1),
            _candidate(parameter, "foreign", "ignored", 40, 0)
          ))

          When("two independent contexts resolve the same candidate collection")
          val first = _take(ConfigurationBindingResolver.resolve(candidates, _context(Vector("global", "instance-a"))))
          val second = _take(ConfigurationBindingResolver.resolve(candidates, _context(Vector("global", "instance-b"))))

          Then("each context obtains only its own eligible winner")
          _take(first.binding(parameter)).get.value shouldBe "first"
          _take(second.binding(parameter)).get.value shouldBe "second"
        }
      }

      "E4 retain an empty collection" must _e4 {
        "when no candidate is eligible" in {
          Given("one Global-only context, one parameter witness, and an empty candidate collection")
          val parameter = _parameter
          val context = _context(Vector("global"))
          val empty = ConfigurationBindingCandidates.empty[String]

          When("the resolver processes the empty collection")
          val emptyresult = _take(ConfigurationBindingResolver.resolve(empty, context))

          Then("the empty collection remains empty")
          _take(emptyresult.binding(parameter)) shouldBe None
        }
      }

      "E5 resolve a Global-only candidate" must _e5 {
        "when exactly one Global candidate is eligible" in {
          Given("one Global-only context, one parameter witness, and one Global candidate")
          val parameter = _parameter
          val context = _context(Vector("global"))
          val candidates = _candidates(Vector(_candidate(parameter, "global", "production", 10, 0)))

          When("the resolver processes the Global candidate")
          val result = _take(ConfigurationBindingResolver.resolve(candidates, context))

          Then("Global is the effective binding")
          _take(result.binding(parameter)).map(_.value) shouldBe Some("production")
        }
      }

      "E6 reject a same-source specificity tie" must _e6 {
        "when one source contributes two equally specific candidates" in {
          Given("one Global-only context, one parameter witness, and one conflicting source")
          val parameter = _parameter
          val context = _context(Vector("global"))
          val conflict = _candidates(Vector(
            _candidate(parameter, "global", "first", 10, 0),
            _candidate(parameter, "global", "second", 10, 0)
          ))

          When("the resolver sees the specificity tie")
          val conflictresult = ConfigurationBindingResolver.resolve(conflict, context)

          Then("the tie is a structured configuration failure")
          conflictresult.isSuccess shouldBe false
          _is_configuration_invalid(conflictresult) shouldBe true
        }
      }

      "E7 preserve ordinal result and override history for either candidate-vector order" must _e7 {
        "when arbitrary distinct source ordinals are presented forward and reverse" in {
          Given("a Global-only context, one parameter witness, and arbitrary ordered source ordinals")
          val parameter = _parameter
          val context = _context(Vector("global"))

          When("the resolver receives both candidate-vector orders")
          forAll(org.scalacheck.Gen.choose(0, 1000), org.scalacheck.Gen.choose(1, 1000)) { (base, offset) =>
            val earlierordinal = base
            val laterordinal = base + offset
            val forward = Vector(
              _candidate(parameter, "global", "earlier", 10, earlierordinal),
              _candidate(parameter, "global", "later", 10, laterordinal)
            )
            val reverse = forward.reverse
            val forwardresult = _take(ConfigurationBindingResolver.resolve(_candidates(forward), context))
            val reverseresult = _take(ConfigurationBindingResolver.resolve(_candidates(reverse), context))

            Then("ordinal determines identical winner history")
            _take(forwardresult.binding(parameter)).map(_.value) shouldBe Some("later")
            _take(reverseresult.binding(parameter)).map(_.value) shouldBe Some("later")
            _take(forwardresult.binding(parameter)).flatMap(_.overridden).map(_.value) shouldBe Some("earlier")
            _take(reverseresult.binding(parameter)).flatMap(_.overridden).map(_.value) shouldBe Some("earlier")
          }
        }
      }

      "E8 reject a mismatched parameter witness" must _e8 {
        "when two source winners share a canonical id but not its parameter instance" in {
          Given("two parameter witnesses with one canonical id and two Global candidates")
          val parameter = _parameter
          val other = _parameter
          val candidates = _candidates(Vector(
            _candidate(parameter, "global", "first", 10, 0),
            _candidate(other, "global", "second", 20, 0)
          ))

          When("the resolver joins the two source winners")
          val result = ConfigurationBindingResolver.resolve(candidates, _context(Vector("global")))

          Then("the witness mismatch is a structured configuration failure")
          result.isSuccess shouldBe false
          _is_configuration_invalid(result) shouldBe true
        }
      }

      "E9 reject invalid ordered target contexts" must _e9 {
        "when the target vector is null, empty, or contains duplicates" in {
          Given("three invalid context target vectors")
          val nullinput = null.asInstanceOf[Vector[String]]
          val emptyinput = Vector.empty[String]
          val duplicateinput = Vector("global", "global")

          When("context admission validates the target vectors")
          val nulltargets = ConfigurationBindingResolutionContext.create(nullinput)
          val emptytargets = ConfigurationBindingResolutionContext.create(emptyinput)
          val duplicatetargets = ConfigurationBindingResolutionContext.create(duplicateinput)

          Then("every invalid vector returns a structured configuration failure")
          nulltargets.isSuccess shouldBe false
          emptytargets.isSuccess shouldBe false
          duplicatetargets.isSuccess shouldBe false
          _is_configuration_invalid(nulltargets) shouldBe true
          _is_configuration_invalid(emptytargets) shouldBe true
          _is_configuration_invalid(duplicatetargets) shouldBe true
        }
      }

      "E10 preserve rank result and override history for either candidate-vector order" must _e10 {
        "when arbitrary higher-ranked sources carry lower ordinals" in {
          Given("a Global-only context, one parameter witness, and arbitrary source ranks")
          val parameter = _parameter
          val context = _context(Vector("global"))

          When("the resolver receives both candidate-vector orders")
          forAll(org.scalacheck.Gen.choose(0, 1000), org.scalacheck.Gen.choose(1, 1000), org.scalacheck.Gen.choose(0, 1000)) { (base, offset, ordinal) =>
            val lowrank = base
            val highrank = base + offset
            val forward = Vector(
              _candidate(parameter, "global", "lower", lowrank, ordinal + offset),
              _candidate(parameter, "global", "higher", highrank, ordinal)
            )
            val reverse = forward.reverse
            val forwardresult = _take(ConfigurationBindingResolver.resolve(_candidates(forward), context))
            val reverseresult = _take(ConfigurationBindingResolver.resolve(_candidates(reverse), context))

            Then("rank determines identical winner history before ordinal")
            _take(forwardresult.binding(parameter)).map(_.value) shouldBe Some("higher")
            _take(reverseresult.binding(parameter)).map(_.value) shouldBe Some("higher")
            _take(forwardresult.binding(parameter)).flatMap(_.overridden).map(_.value) shouldBe Some("lower")
            _take(reverseresult.binding(parameter)).flatMap(_.overridden).map(_.value) shouldBe Some("lower")
          }
        }
      }

      "E11 reject a mismatched witness before same-source specificity selection" must _e11 {
        "when one physical source has differently specific candidates with one canonical id" in {
          Given("two parameter witnesses, one canonical id, two targets, and one physical source")
          val parameter = _parameter
          val other = _parameter
          val candidates = _candidates(Vector(
            _candidate(parameter, "global", "global", 10, 0),
            _candidate(other, "instance", "instance", 10, 0)
          ))

          When("the resolver validates the complete eligible parameter group")
          val result = ConfigurationBindingResolver.resolve(candidates, _context(Vector("global", "instance")))

          Then("it returns a structured configuration failure before choosing specificity")
          result.isSuccess shouldBe false
          _is_configuration_invalid(result) shouldBe true
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

  private def _context(targets: Vector[String]): ConfigurationBindingResolutionContext[String] =
    _take(ConfigurationBindingResolutionContext.create(targets))

  private def _candidates(
    candidates: Vector[ConfigurationBindingCandidate[?, String]]
  ): ConfigurationBindingCandidates[String] =
    _take(ConfigurationBindingCandidates.from(candidates))

  private def _candidate(
    parameter: ConfigurationParameter[String],
    target: String,
    value: String,
    rank: Int,
    ordinal: Int
  ): ConfigurationBindingCandidate[String, String] =
    _take(
      for {
        provenance <- ConfigurationProvenance.create(
          ConfigurationOrigin.Home,
          "home",
          s"source-$rank-$ordinal",
          None,
          Some("textus.application.mode"),
          rank,
          ordinal,
          Vector.empty,
          isConfidential = false
        )
        candidate <- ConfigurationBindingCandidate.create(parameter, target, value, provenance)
      } yield candidate
    )

  private def _take[A](result: Consequence[A]): A =
    result.getOrElse(fail(result.display))

  private def _is_configuration_invalid(result: Consequence[?]): Boolean =
    result match {
      case Consequence.Failure(conclusion) => conclusion.observation.taxonomy.category.name == "configuration"
      case _ => false
    }
}
