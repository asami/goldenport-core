package org.goldenport.configuration

import java.nio.file.{Files, Paths}
import org.goldenport.configuration.source.ConfigurationSource
import org.goldenport.configuration.source.file.SimpleFileConfigLoader
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 30, 2026
 * @version Jul. 31, 2026
 * @author  ASAMI, Tomoharu
 */
final class ConfigurationResolverTraceSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "ConfigurationResolver trace" should {
    "retain ordinary file-source type and identity through final resolution" in {
      Given("one home configuration file source")
      val target = Paths.get("target", "test-tmp", "phase-53")
      Files.createDirectories(target)
      val path = Files.createTempFile(target, "trace-", ".json")
      try {
        Files.writeString(path, "{\"textus.phase53.sample\":\"value\"}")
        val source = ConfigurationSource.File(
          ConfigurationOrigin.Home,
          path,
          ConfigurationSource.Rank.Home,
          new SimpleFileConfigLoader
        )

        When("the source is resolved through the generic resolver")
        val resolution = ConfigurationResolver.default.resolve(Seq(source)).toOption
          .flatMap(_.trace.get("textus.phase53.sample"))
          .getOrElse(fail("expected resolved configuration trace entry"))

        Then("the trace must retain enough file provenance for Phase 53 overlay diagnostics")
        resolution.sourceType shouldBe None
        resolution.sourceId shouldBe None
        pendingUntilFixed {
          resolution.sourceType shouldBe Some("file")
          resolution.sourceId shouldBe Some(path.toString)
        }
      } finally {
        Files.deleteIfExists(path)
      }
    }
  }
}
