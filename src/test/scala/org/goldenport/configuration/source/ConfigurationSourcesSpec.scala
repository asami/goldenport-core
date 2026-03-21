package org.goldenport.configuration

import java.nio.file.{Files, Path}

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import org.goldenport.configuration.source.ProjectRootFinder

/*
 * @since   Mar. 21, 2026
 * @version Mar. 21, 2026
 * @author  ASAMI, Tomoharu
 */
class ConfigurationSourcesSpec
  extends AnyWordSpec
    with Matchers
    with GivenWhenThen {

  "ProjectRootFinder" should {
    "detect project root via .cncf ancestor" in {
      Given("a nested cwd under a .cncf project root")
      val root = Files.createTempDirectory("sm-root-cncf-")
      Files.createDirectories(root.resolve(".cncf"))
      val cwd = Files.createDirectories(root.resolve("a").resolve("b"))

      When("searching upward")
      val found = ProjectRootFinder.find(cwd, "cncf")

      Then("the .cncf ancestor is returned")
      found.map(_.normalize()) shouldBe Some(root.normalize())
    }

    "detect project root via .git ancestor" in {
      Given("a nested cwd under a .git project root")
      val root = Files.createTempDirectory("sm-root-git-")
      Files.createDirectories(root.resolve(".git"))
      val cwd = Files.createDirectories(root.resolve("a").resolve("b"))

      When("searching upward")
      val found = ProjectRootFinder.find(cwd, "cncf")

      Then("the .git ancestor is returned")
      found.map(_.normalize()) shouldBe Some(root.normalize())
    }

    "prefer nearest ancestor on upward search" in {
      Given("a higher .cncf and nearer .git")
      val top = Files.createTempDirectory("sm-root-nearest-")
      Files.createDirectories(top.resolve(".cncf"))
      val nearest = Files.createDirectories(top.resolve("work"))
      Files.createDirectories(nearest.resolve(".git"))
      val cwd = Files.createDirectories(nearest.resolve("child"))

      When("searching upward")
      val found = ProjectRootFinder.find(cwd, "cncf")

      Then("the first matched ancestor (nearer .git) wins")
      found.map(_.normalize()) shouldBe Some(nearest.normalize())
    }
  }

  "ConfigurationSources.standard" should {
    "skip project source when no root is found" in {
      Given("a cwd with no .cncf/.git ancestors")
      val cwd = Files.createTempDirectory("sm-no-root-")

      When("assembling standard sources")
      val sources =
        ConfigurationSources.standard(
          cwd = cwd,
          applicationname = "cncf",
          args = Map("a" -> "1"),
          env = Map("E" -> "x")
        )

      Then("project origin is absent")
      sources.sources.exists(_.origin == ConfigurationOrigin.Project) shouldBe false
      _compress_origins(sources.sources.map(_.origin).toVector) shouldBe Vector(
        ConfigurationOrigin.Home,
        ConfigurationOrigin.Cwd,
        ConfigurationOrigin.Environment,
        ConfigurationOrigin.Arguments
      )
    }

    "keep stable source order home-project?-cwd-env-args" in {
      Given("a cwd under detected project root")
      val root = Files.createTempDirectory("sm-order-")
      Files.createDirectories(root.resolve(".cncf"))
      val cwd = Files.createDirectories(root.resolve("app").resolve("work"))

      When("assembling standard sources")
      val sources =
        ConfigurationSources.standard(
          cwd = cwd,
          applicationname = "cncf",
          args = Map("a" -> "1"),
          env = Map("E" -> "x")
        )

      val grouped = _compress_origins(sources.sources.map(_.origin).toVector)

      Then("origin groups are ordered deterministically")
      grouped shouldBe Vector(
        ConfigurationOrigin.Home,
        ConfigurationOrigin.Project,
        ConfigurationOrigin.Cwd,
        ConfigurationOrigin.Environment,
        ConfigurationOrigin.Arguments
      )
    }
  }

  private def _compress_origins(origins: Vector[ConfigurationOrigin]): Vector[ConfigurationOrigin] =
    origins.foldLeft(Vector.empty[ConfigurationOrigin]) { (z, o) =>
      z.lastOption match {
        case Some(last) if last == o => z
        case _ => z :+ o
      }
    }
}
