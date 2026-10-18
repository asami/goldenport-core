package org.goldenport

import org.goldenport.observation.Descriptor
import org.goldenport.schema.XSemiStructuredData
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Oct. 18, 2026
 * @version Oct. 18, 2026
 */
final class ConclusionRecordPolicySpec extends AnyWordSpec
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks
  with Matchers {

  "A simple Conclusion" should {
    "preserve one original message through canonical Record encoding" in {
      Given("arbitrary nonempty textual diagnostic messages")
      val messages = Gen.nonEmptyListOf(Gen.choose(0, 0xffff).map(_.toChar)).map(_.mkString)
      forAll(messages) { message =>
        When("a simple Conclusion is projected to its canonical JSON Record")
        val conclusion = Conclusion.simple(message)
        val encoded = XSemiStructuredData.toJson(conclusion.toRecord)

        Then("exactly one Message facet retains the original text and JSON conversion succeeds")
        conclusion.observation.cause.descriptor.facets.collect {
          case Descriptor.Facet.Message(value) => value
        } shouldBe Vector(message)
        encoded match {
          case Consequence.Success(json) =>
            json.hcursor.downField("observation").downField("cause").get[String]("message") shouldBe Right(message)
          case _ => fail("A simple Conclusion must have a canonical JSON Record")
        }
      }
    }
  }
}
