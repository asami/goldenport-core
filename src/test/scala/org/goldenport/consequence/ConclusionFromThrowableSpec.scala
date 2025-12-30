package org.goldenport.consequence

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.goldenport.Conclusion
import org.goldenport.observation.{CauseKind, Severity}

/*
 * @since   Dec. 30, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
class ConclusionFromThrowableSpec extends AnyWordSpec with Matchers {

  "Conclusion.fromThrowable(Throwable)" should {

    "always produce a failure Conclusion representing a Defect" in {
      val ex = new RuntimeException("boom")

      val c = Conclusion.fromThrowable(ex)

      c.observation.exception shouldBe Some(ex)
      c.observation.causeKind shouldBe CauseKind.Defect
    }

    "use InternalError as WebCode" in {
      val ex = new IllegalStateException("illegal")

      val c = Conclusion.fromThrowable(ex)

      c.status.webCode shouldBe Conclusion.WebCode.InternalError
    }

    "use Error severity" in {
      val ex = new RuntimeException("error")

      val c = Conclusion.fromThrowable(ex)

      c.observation.severity shouldBe Severity.Error
    }

    "not encode domain meaning" in {
      val ex = new RuntimeException("domain-like message")

      val c = Conclusion.fromThrowable(ex)

      c.observation.message shouldBe None
    }

    "be deterministic for the same Throwable instance" in {
      val ex = new RuntimeException("same")

      val c1 = Conclusion.fromThrowable(ex)
      val c2 = Conclusion.fromThrowable(ex)

      c1 shouldBe c2
    }

    "treat null Throwable as a Defect" in {
      val c = Conclusion.fromThrowable(null)

      c.observation.exception shouldBe None
      c.observation.causeKind shouldBe CauseKind.Defect
      c.status.webCode shouldBe Conclusion.WebCode.InternalError
      c.observation.severity shouldBe Severity.Error
    }
  }
}
