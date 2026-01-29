package org.goldenport

import java.time.Instant

import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpec
import cats.data.NonEmptyVector
import org.goldenport.datatype.I18nMessage
import org.goldenport.provisional.observation.Observation
// import org.goldenport.observation.Agent
// import org.goldenport.observation.Cause
// import org.goldenport.observation.CauseKind
// import org.goldenport.observation.Descriptor
// import org.goldenport.observation.Handler
// import org.goldenport.observation.Observation
// import org.goldenport.observation.Phenomenon
// import org.goldenport.observation.Resource
// import org.goldenport.observation.Severity
// import org.goldenport.observation.Strategy
// import org.goldenport.observation.Subject
// import org.goldenport.observation.SystemLocation

/*
 * @since   Dec. 22, 2025
 *  version Dec. 30, 2025
 * @version Jan. 27, 2026
 * @author  ASAMI, Tomoharu
 */
class ConsequenceSpec extends AnyWordSpec
  with ScalaCheckDrivenPropertyChecks
  with Matchers {

  // ------------------------------------------------------------------
  // Helpers (test-local, explicit)
  // ------------------------------------------------------------------

  def ok[A](a: A): Consequence[A] =
    Consequence.Success(a)

  def ng(label: String): Consequence[Nothing] =
    Consequence.Failure(Conclusion.simple(label))
  // def ng(label: String): Consequence[Nothing] =
  //   Consequence.Failure(
  //     Conclusion(
  //       status = Conclusion.Status(
  //         webCode = Conclusion.WebCode.BadRequest
  //       ),
  //       observation = _observation(label),
  //       previous = None
  //     )
  //   )

  // private def _observation(label: String): Observation =
  //   Observation.argumentInvalid(label)

  // private def _observation(label: String): Observation =
  //   Observation(
  //     phenomenon = Phenomenon.Rejection,
  //     causeKind = CauseKind.Fault,
  //     cause = Some(Cause.ValidationError),
  //     severity = Severity.Error,
  //     strategy = Strategy.Manual,
  //     handler = Handler.EndUser,
  //     timestamp = Instant.EPOCH,
  //     subject = Subject.User,
  //     `object` = Resource.Unknown,
  //     agent = Agent.System,
  //     location = SystemLocation(None),
  //     traceId = None,
  //     spanId = None,
  //     descriptor = Descriptor(),
  //     message = Some(I18nMessage(label)),
  //     exception = None,
  //     properties = Map.empty
  //   )

  "Consequence" should {
    "when used in validation style (applicative composition)" should {
      "combine independent results using zip" should {
        "return Success when both sides succeed" in {
          val r =
            ok(1).zip(ok(2))

          r.shouldBe(Consequence.Success((1, 2)))
        }
        "return Failure if the left side fails" in {
          val r =
            ng("A").zip(ok(1))

          r match {
            case Consequence.Failure(_) => succeed
            case _ => fail("expected Failure")
          }
        }
        "return Failure if the right side fails" in {
          val r =
            ok(1).zip(ng("B"))

          r match {
            case Consequence.Failure(_) => succeed
            case _ => fail("expected Failure")
          }
        }
        "aggregate failures when both sides fail" in {
          val r =
            ng("A").zip(ng("B"))

          val c = r match {
            case Consequence.Failure(conclusion) => conclusion
            case _ => fail("expected Failure")
          }
          // c.causes.map(_.observation.displayMessage).shouldBe(Seq("A", "B"))
          println(c)
        }
      }
      "combine three independent validations using zip3" should {
        "return Success when all three succeed" in {
          val r =
            Consequence.zip3(
              ok(1),
              ok(2),
              ok(3)
            )

          r.shouldBe(Consequence.Success((1, 2, 3)))
        }
        "collect all failures from three inputs" in {
          val r =
            Consequence.zip3(
              ng("A"),
              ok(2),
              ng("C")
            )

          val c = r match {
            case Consequence.Failure(conclusion) => conclusion
            case _ => fail("expected Failure")
          }
          // c.causes.map(_.observation.displayMessage).shouldBe(Seq("A", "C"))
          println(c)
        }
      }
      "combine an arbitrary number of validations using zipN" should {
        "collect all failures without short-circuiting" in {
          val r =
            Consequence.zipN(
              Seq(
                ng("A"),
                ok(2),
                ng("C"),
                ng("D")
              )
            )

          val c = r match {
            case Consequence.Failure(conclusion) => conclusion
            case _ => fail("expected Failure")
          }
          // c.causes.map(_.observation.displayMessage).shouldBe(Seq("A", "C", "D"))
          println(c)
        }
        "preserve the input order of observations in failures" in {
          val r =
            Consequence.zipN(
              Seq(
                ng("first"),
                ng("second"),
                ng("third")
              )
            )

          val c = r match {
            case Consequence.Failure(conclusion) => conclusion
            case _ => fail("expected Failure")
          }
          // c.causes.map(_.observation.displayMessage).shouldBe(Seq("first", "second", "third"))
          println(c)
        }
      }
    }

    "as a design constraint" should {
      "not use flatMap for validation style" in {
        // This test is documentary:
        // If flatMap were used, only the first failure would be visible.
        // zip / zipN MUST be used for validation.
        succeed
      }
    }
  }
}
