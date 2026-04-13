package org.goldenport.protocol.handler

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import org.goldenport.Consequence
import org.goldenport.observation.Descriptor
import org.goldenport.protocol.Response
import org.goldenport.protocol.handler.egress.{Egress, EgressCollection}
import org.goldenport.protocol.handler.projection.ProjectionCollection
import org.goldenport.protocol.spec.ServiceDefinitionGroup
import org.goldenport.provisional.observation.Taxonomy

/*
 * @since   Apr. 14, 2026
 * @version Apr. 14, 2026
 * @author  ASAMI, Tomoharu
 */
class ProjectionEgressCollectionSpec
  extends AnyWordSpec
    with GivenWhenThen
    with Matchers {

  "ProjectionCollection" should {
    "report a missing projection kind as a structured missing input failure" in {
      Given("an empty projection collection")
      val collection = ProjectionCollection.empty

      When("projecting a known kind without a registered projection")
      val result = collection.projectByName("openapi", ServiceDefinitionGroup.empty)

      Then("it fails as an argument-missing result with the projection name")
      val conclusion = _failure_conclusion(result)
      conclusion.observation.taxonomy shouldBe Taxonomy.argumentMissing
      conclusion.observation.cause.descriptor.facets should contain(
        Descriptor.Facet.Input(name = Some("openapi"))
      )
    }
  }

  "EgressCollection" should {
    "report a missing egress kind as a structured missing input failure" in {
      Given("an empty egress collection")
      val collection = EgressCollection.empty

      When("resolving an HTTP egress without a registered egress")
      val result = collection.egress(Egress.Kind.Http, Response.Void())

      Then("it fails as an argument-missing result with the egress kind")
      val conclusion = _failure_conclusion(result)
      conclusion.observation.taxonomy shouldBe Taxonomy.argumentMissing
      conclusion.observation.cause.descriptor.facets should contain(
        Descriptor.Facet.Input(name = Some("http"))
      )
    }
  }

  private def _failure_conclusion[A](p: Consequence[A]) =
    p match {
      case Consequence.Failure(conclusion) => conclusion
      case Consequence.Success(_) => fail("expected failure")
    }
}
