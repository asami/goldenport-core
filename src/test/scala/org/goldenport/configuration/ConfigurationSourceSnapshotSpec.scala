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
final class ConfigurationSourceSnapshotSpec
  extends AnyWordSpec
    with Matchers
    with GivenWhenThen {
  private val _e1 = afterWord(
    "in spec:phase-55-source-snapshot, example:E1, rules:GCF04-R2,R3,R4, phase:55, slice:GCF-04"
  )
  private val _e2 = afterWord(
    "in spec:phase-55-source-snapshot, example:E2, rules:GCF04-R2, phase:55, slice:GCF-04"
  )

  "ConfigurationSourceSnapshots" should {
    "capture admitted physical sources once in runtime-owned order" which {
      "E1 load each source once and assign rank and ordinal from admission" must _e1 {
        "when two sources are captured" in {
          Given("two runtime admissions whose loaders count their invocations")
          var firstcount = 0
          var secondcount = 0
          val first = _admission("home", 10, "home-a") { () =>
            firstcount += 1
            Consequence.success(ConfigurationDocument.Scalar(ConfigurationValue.StringValue("first")))
          }
          val second = _admission("arguments", 50, "args-a") { () =>
            secondcount += 1
            Consequence.success(ConfigurationDocument.Scalar(ConfigurationValue.StringValue("second")))
          }

          When("one immutable snapshot is captured")
          val snapshots = _take(ConfigurationSourceSnapshots.load(Vector(first, second)))

          Then("the physical loaders run exactly once with stable runtime ordering")
          firstcount shouldBe 1
          secondcount shouldBe 1
          snapshots.snapshots.map(_.sourceOrdinal) shouldBe Vector(0, 1)
          snapshots.snapshots.map(_.sourceRank) shouldBe Vector(10, 50)
          snapshots.snapshots.map(_.collisionDomain) shouldBe Vector("home", "arguments")
        }
      }

      "E2 retain a structured loader failure without reloading an admitted source" must _e2 {
        "when a loader fails" in {
          Given("one failing source admission")
          var count = 0
          val failing = _admission("home", 10, "home-a") { () =>
            count += 1
            Consequence.configurationInvalid("fixture source failure")
          }

          When("snapshot capture is attempted")
          val result = ConfigurationSourceSnapshots.load(Vector(failing))

          Then("the failure is structured and the source was invoked once")
          result.isSuccess shouldBe false
          count shouldBe 1
        }
      }
    }
  }

  private def _admission(
    layer: String,
    rank: Int,
    source: String
  )(
    load: () => Consequence[ConfigurationDocument]
  ): ConfigurationSourceAdmission[ConfigurationDocument] =
    _take(
      ConfigurationSourceAdmission.create(
        ConfigurationOrigin.Home,
        layer,
        source,
        rank,
        layer,
        load
      )
    )

  private def _take[A](result: Consequence[A]): A =
    result.getOrElse(fail(result.display))
}
