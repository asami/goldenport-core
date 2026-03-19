package org.goldenport.model.statemachine

import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Dec. 22, 2025
 * @version Mar. 19, 2026
 * @author  ASAMI, Tomoharu
 */
class PostStatusSpec
  extends AnyWordSpec
    with ScalaCheckDrivenPropertyChecks
    with Matchers
    with GivenWhenThen {

  "PostStatus" should {
    "use Draft as default status" in {
      Given("the PostStatus default")
      val state = PostStatus.default

      When("evaluating publication flag")
      val ispublic = state.isPublic

      Then("default is Draft and not public")
      state shouldBe PostStatus.Draft
      ispublic shouldBe false
    }

    "round-trip enum values through textual representation" in {
      Given("an arbitrary PostStatus state")
      val stategen = Gen.oneOf(PostStatus.values.toSeq)

      forAll(stategen) { state =>
        When("converting to string and parsing via from")
        val parsed = PostStatus.from(state.value)

        Then("the same state is restored")
        parsed shouldBe Some(state)
      }
    }

    "keep lowercase state name and db value mapping" in {
      Given("an arbitrary PostStatus state")
      val stategen = Gen.oneOf(PostStatus.values.toSeq)

      forAll(stategen) { state =>
        When("reading stateName and resolving from db value")
        val name = state.stateName
        val parsed = PostStatus.fromDbValue(state.dbValue)

        Then("state name is lowercase and db value round-trips")
        name shouldBe name.toLowerCase
        parsed shouldBe Some(state)
      }
    }

    "reject unknown textual state" in {
      Given("an unknown textual state")
      val unknown = "scheduled"

      When("parsing through from")
      val parsed = PostStatus.from(unknown)

      Then("no state is resolved")
      parsed shouldBe None
    }

    "follow canonical transition semantics" in {
      Given("Draft state and publish event")
      val current = PostStatus.Draft

      When("transition is selected via deterministic planner")
      val next = PostStatus.transition(current, PostStatus.Event.Publish)

      Then("state moves to Published")
      next shouldBe Right(Some(PostStatus.Published))
    }
  }
}
