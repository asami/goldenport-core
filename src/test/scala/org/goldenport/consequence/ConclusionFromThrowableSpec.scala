package org.goldenport.consequence

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.goldenport.Conclusion
import org.goldenport.observation.Phenomenon
import org.goldenport.observation.{CauseKind, Severity}
import org.goldenport.test.matchers.ConclusionMatchers

/*
 * @since   Dec. 30, 2025
 * @version Jan. 29, 2026
 * @author  ASAMI, Tomoharu
 */
class ConclusionFromThrowableSpec extends AnyWordSpec with Matchers
  with ConclusionMatchers {

  "Conclusion.fromThrowable(Throwable)" should {

    "always produce a failure Conclusion representing a Defect" in {
      val ex = new RuntimeException("boom")

      val c = Conclusion.fromThrowable(ex)

      c.observation.exception shouldBe Some(ex)
      c.observation.phenomenon shouldBe Phenomenon.Failure
    }

    "use InternalError as WebCode" in {
      val ex = new IllegalStateException("illegal")

      val c = Conclusion.fromThrowable(ex)

      c.status.webCode shouldBe Conclusion.WebCode.InternalError
    }

    "use Error severity" in {
      val ex = new RuntimeException("error")

      val c = Conclusion.fromThrowable(ex)

      c.observation.severity shouldBe Some(Severity.Error)
    }

    "not encode domain meaning" in {
      val ex = new RuntimeException("domain-like message")

      val c = Conclusion.fromThrowable(ex)

      c.observation.getMessage shouldBe None
    }

    "be deterministic for the same Throwable instance without timestamp" in {
      val ex = new RuntimeException("same")

      val c1 = Conclusion.fromThrowable(ex)
      val c2 = Conclusion.fromThrowable(ex)

      c1 should be_equal_conclusion(c2)
    }

    "treat null Throwable as a Defect" in {
      val c = Conclusion.fromThrowable(null)

      c.observation.exception shouldBe None
      // c.observation.causeKind shouldBe CauseKind.Defect
      c.observation.phenomenon shouldBe Phenomenon.Failure
      c.status.webCode shouldBe Conclusion.WebCode.InternalError
      c.observation.severity shouldBe Some(Severity.Error)
    }
  }
}
