package org.goldenport.configuration

import org.goldenport.Consequence
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Aug.  3, 2026
 * @version Aug.  3, 2026
 * @author  ASAMI, Tomoharu
 */
final class ConfigurationBindingStringCodecSpec
  extends AnyWordSpec
    with Matchers
    with GivenWhenThen {
  private val _e1 = afterWord(
    "in spec:phase-55-canonical-binding-string-codec, example:E1, rules:GCF08A-R1,R2, phase:55, slice:GCF-08A"
  )
  private val _e2 = afterWord(
    "in spec:phase-55-canonical-binding-string-codec, example:E2, rules:GCF08A-R3,R4, phase:55, slice:GCF-08A"
  )
  private val _e3 = afterWord(
    "in spec:phase-55-canonical-binding-string-codec, example:E3, rules:GCF08A-R5, phase:55, slice:GCF-08A"
  )
  private sealed trait TestTarget
  private case object GlobalTarget extends TestTarget
  private final case class QualifiedTarget(value: String) extends TestTarget

  private val _codec = _take(ConfigurationBindingStringCodec.create(new ConfigurationBindingQualifierCodec[TestTarget] {
    def decode(value: Option[String]): Consequence[TestTarget] =
      value match {
        case None => Consequence.success(GlobalTarget)
        case Some(text) if text.startsWith("q/") && text.length > 2 => Consequence.success(QualifiedTarget(text.drop(2)))
        case _ => Consequence.configurationInvalid("test qualifier is invalid")
      }

    def encode(value: TestTarget): Consequence[Option[String]] =
      value match {
        case `GlobalTarget` => Consequence.success(None)
        case QualifiedTarget(text) if text.nonEmpty => Consequence.success(Some(s"q/$text"))
        case _ => Consequence.configurationInvalid("test target is invalid")
      }
  }))

  "Configuration binding string codec" should {
    "round-trip bare and qualified canonical references" which {
      "E1 preserve target-parametrized outer grammar" must _e1 {
        "when Global and a qualified target are encoded and decoded" in {
          Given("one canonical parameter and a qualifier codec with no CNCF semantics")
          val parameter = _take(CanonicalParameterId.parse("textus.application.mode"))
          val bare = _take(ConfigurationBindingReference.create[TestTarget](parameter, GlobalTarget))
          val qualified = _take(ConfigurationBindingReference.create[TestTarget](parameter, QualifiedTarget("blue")))

          When("the generic outer codec round-trips both references")
          val encodedbare = _take(_codec.encode(bare))
          val decodedbare = _take(_codec.decode(encodedbare))
          val encodedqualified = _take(_codec.encode(qualified))
          val decodedqualified = _take(_codec.decode(encodedqualified))

          Then("bare and qualified forms retain only the target codec's target meaning")
          encodedbare shouldBe "textus.application.mode"
          decodedbare.target shouldBe GlobalTarget
          encodedqualified shouldBe "@q/blue:textus.application.mode"
          decodedqualified.target shouldBe QualifiedTarget("blue")
        }
      }
    }

    "reject malformed outer grammar and non-canonical parameter identities" which {
      "E2 fail structurally before target interpretation" must _e2 {
        "when malformed or non-canonical external strings are decoded" in {
          Given("the generic codec's canonical parameter-id boundary")
          val spellings = Vector(
            null,
            "",
            "@q/blue",
            "@q/blue:",
            "@q/blue:textus.application.mode:extra",
            "textus.application.mode:extra",
            "Textus.application.mode",
            "textus.application.mode@"
          )

          When("each external spelling is decoded")
          val results = spellings.map(_codec.decode)

          Then("none becomes a typed binding reference")
          results.foreach(_.isSuccess shouldBe false)
        }
      }
    }

    "propagate target codec rejection without adding target semantics" which {
      "E3 retain target-codec authority" must _e3 {
        "when a qualifier or target is rejected by the supplied codec" in {
          Given("a generic codec that does not know target semantics")
          val emptyqualified = _take(ConfigurationBindingReference.create[TestTarget](
            _take(CanonicalParameterId.parse("textus.application.mode")),
            QualifiedTarget("")
          ))

          When("the codec delegates to the target qualifier codec")
          val invalidqualifier = _codec.decode("@other/blue:textus.application.mode")
          val invalidtarget = _codec.encode(emptyqualified)

          Then("the qualifier codec's structured failures are preserved")
          invalidqualifier.isSuccess shouldBe false
          invalidtarget.isSuccess shouldBe false
        }
      }
    }
  }

  private def _take[A](value: Consequence[A]): A =
    value.getOrElse(fail(value.display))
}
