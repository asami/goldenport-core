package org.goldenport

import cats.Id
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

/*
 * @since   Jan. 10, 2026
 * @version Jan. 10, 2026
 * @author  ASAMI, Tomoharu
 */
class ConsequenceTSpec
    extends AnyWordSpec
    with Matchers {

  "ConsequenceT.pure" should {
    "wrap Success in the target context" in {
      val result = ConsequenceT.pure[Id, Int](1)
      result.value shouldBe Consequence.Success(1)
    }
  }

  "ConsequenceT.liftF" should {
    "lift a value into Success in the target context" in {
      val result = ConsequenceT.liftF[Option, Int](Some(1))
      result.value shouldBe Some(Consequence.Success(1))
    }
  }

  "ConsequenceT.fromConsequence" should {
    "lift a Consequence into the target context" in {
      val source = Consequence.failure[Int]("boom")
      val result = ConsequenceT.fromConsequence[Option, Int](source)
      result.value shouldBe Some(source)
    }
  }

  "ConsequenceT.map" should {
    "apply only to Success" in {
      val success = ConsequenceT.fromConsequence[Id, Int](Consequence.success(1))
      success.map(_ + 1).value shouldBe Consequence.Success(2)

      val failure = ConsequenceT.fromConsequence[Id, Int](Consequence.failure("nope"))
      failure.map(_ + 1).value shouldBe failure.value
    }
  }

  "ConsequenceT.flatMap" should {
    "not evaluate on Failure" in {
      var called = 0
      val failure = ConsequenceT.fromConsequence[Option, Int](Consequence.failure("nope"))
      val result =
        failure.flatMap { _ =>
          called += 1
          ConsequenceT.pure[Option, Int](1)
        }

      called shouldBe 0
      result.value shouldBe failure.value
    }
  }
}
