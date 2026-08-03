package org.goldenport.configuration

import java.nio.file.Paths

import org.goldenport.Consequence
import org.goldenport.configuration.source.ConfigurationSource
import org.goldenport.configuration.source.file.FileConfigLoader
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Aug.  3, 2026
 * @version Aug.  3, 2026
 * @author  ASAMI, Tomoharu
 */
final class ConfigurationResolutionSnapshotSpec
  extends AnyWordSpec
    with Matchers
    with GivenWhenThen {
  private val _e1 = afterWord(
    "in spec:phase-55-gcf07c-runtime-source-snapshot, example:E1, rules:GCF07C-R1,R2, phase:55, slice:GCF-07C"
  )
  private val _e2 = afterWord(
    "in spec:phase-55-gcf08l-raw-yaml-duplicate-member-admission, example:E2, rules:GCF08L-R1,R2, phase:55, slice:GCF-08L"
  )

  "Configuration resolution snapshots" should {
    "E1 load each physical source exactly once while deriving legacy resolution" must _e1 {
      "when one file source supplies an effective configuration value" in {
        Given("a counting file source whose loader returns one configuration")
        var count = 0
        val source = ConfigurationSource.File(
          ConfigurationOrigin.Home,
          Paths.get("target", "gcf07c-runtime.conf"),
          ConfigurationSource.Rank.Home,
          new FileConfigLoader {
            override def load(path: java.nio.file.Path): Consequence[Configuration] = {
              count += 1
              Consequence.success(Configuration(Map(
                "textus.subsystem.user-mode" -> ConfigurationValue.StringValue("standalone")
              )))
            }
          }
        )

        When("the generic resolver captures its immutable runtime snapshot")
        val snapshot = _take(ConfigurationResolver.default.resolveSnapshot(Vector(source)))

        Then("one loaded value supplies both the source snapshot and legacy configuration trace")
        count shouldBe 1
        snapshot.sources.size shouldBe 1
        snapshot.sources.head.value shouldBe snapshot.resolved.configuration
        snapshot.sources.head.sourceOrdinal shouldBe 0
        snapshot.resolved.trace.get("textus.subsystem.user-mode").flatMap(_.sourceId) shouldBe Some(source.path.toString)
      }
    }

    "E2 retain a standard file source raw YAML document without a second source load" must _e2 {
      "when a YAML file repeats one top-level member" in {
        Given("a real temporary YAML file")
        val path = java.nio.file.Files.createTempFile("gcf08l-runtime-", ".yaml")
        java.nio.file.Files.writeString(path, "setting: one\nsetting: two\n")
        val source = ConfigurationSource.File(
          ConfigurationOrigin.Home,
          path,
          ConfigurationSource.Rank.Home,
          new org.goldenport.configuration.source.file.SimpleFileConfigLoader
        )

        When("the generic resolver derives its compatibility resolution and source snapshot")
        val snapshot = _take(ConfigurationResolver.default.resolveSnapshot(Vector(source)))

        Then("the raw document preserves both members and the legacy configuration remains usable")
        snapshot.sources.head.value.values.keySet shouldBe Set("setting")
        snapshot.sources.head.rawDocument.map(_.fields.map(_.name)) shouldBe Some(Vector("setting", "setting"))
      }
    }
  }

  private def _take[A](result: Consequence[A]): A =
    result.getOrElse(fail(result.display))
}
