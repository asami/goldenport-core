package org.goldenport.provisional

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalacheck.Gen
import org.goldenport.observation.Descriptor
import org.goldenport.provisional.observation.{Observation, Taxonomy, Cause}
import org.goldenport.provisional.observation.{ObservationRender, ObservationProject}

/*
 * @since   Mar.  4, 2026
 * @version Mar.  4, 2026
 * @author  ASAMI, Tomoharu
 */
class ObservationWarnBridgeSpec extends AnyWordSpec
  with ScalaCheckDrivenPropertyChecks
  with Matchers {

  "Observation warn bridge" should {
    "project canonical warning attributes for component class-load failures" in {
      // Given
      val cause = Cause.create(Seq(
        Descriptor.Facet.Component("example-plugin"),
        Descriptor.Facet.ClassName("com.example.Plugin"),
        Descriptor.Facet.Artifact("plugin-a.car"),
        Descriptor.Facet.RepositoryType("component-dir"),
        Descriptor.Facet.Exception(new ClassNotFoundException("com.example.Plugin"))
      ))
      val observation = Observation.failure(
        Taxonomy.componentUnavailable,
        cause
      )

      // When
      val attrs = ObservationProject.warnAttributes(observation)

      // Then
      attrs("taxonomy.category") shouldBe "component"
      attrs("taxonomy.symptom") shouldBe "unavailable"
      attrs("component") shouldBe "example-plugin"
      attrs("class.name") shouldBe "com.example.Plugin"
      attrs("artifact.id") shouldBe "plugin-a.car"
      attrs("repository.type") shouldBe "component-dir"
      attrs("exception.class") shouldBe "java.lang.ClassNotFoundException"
    }

    "render a deterministic compact warning message" in {
      // Given
      val cause = Cause.create(Seq(
        Descriptor.Facet.Component("example-plugin"),
        Descriptor.Facet.ClassName("com.example.Plugin"),
        Descriptor.Facet.Artifact("plugin-a.car"),
        Descriptor.Facet.RepositoryType("component-dir"),
        Descriptor.Facet.Message("dependency not found")
      ))
      val observation = Observation.failure(
        Taxonomy.componentUnavailable,
        cause
      )

      // When
      val message = ObservationRender.warnMessage(observation)

      // Then
      message should include ("component/unavailable")
      message should include ("component=example-plugin")
      message should include ("class.name=com.example.Plugin")
      message should include ("artifact.id=plugin-a.car")
      message should include ("repository.type=component-dir")
      message should include ("dependency not found")
    }

    "be deterministic property-wise for stable observations" in {
      // Given / When / Then
      val nonEmpty = Gen.alphaNumStr.suchThat(_.nonEmpty)
      forAll(nonEmpty, nonEmpty, nonEmpty) { (cls, artifact, repo) =>
        val cause = Cause.create(Seq(
          Descriptor.Facet.ClassName(cls),
          Descriptor.Facet.Artifact(artifact),
          Descriptor.Facet.RepositoryType(repo)
        ))
        val observation = Observation.failure(Taxonomy.componentInvalid, cause)
        val m1 = ObservationRender.warnMessage(observation)
        val m2 = ObservationRender.warnMessage(observation)
        val a1 = ObservationProject.warnAttributes(observation)
        val a2 = ObservationProject.warnAttributes(observation)
        m1 shouldBe m2
        a1 shouldBe a2
      }
    }
  }
}
