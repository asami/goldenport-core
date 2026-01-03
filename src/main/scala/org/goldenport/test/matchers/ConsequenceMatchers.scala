package org.goldenport.test.matchers

import org.scalatest.matchers.{MatchResult, Matcher}
import org.goldenport.Consequence
import org.goldenport.Conclusion
import org.goldenport.observation.Cause
import org.goldenport.observation.Cause.Reason
import org.goldenport.observation.Descriptor

/*
 * @since   May. 11, 2025
 *  version Jul.  1, 2025
 *  version Dec. 28, 2025
 * @version Jan.  3, 2026
 * @author  ASAMI, Tomoharu
 */
trait ConsequenceMatchers {
  // ---- success ----
  def be_success[A]: Matcher[Consequence[A]] =
    Matcher { c =>
      c match {
        case Consequence.Success(_) =>
          MatchResult(
            true,
            "Expected success but was failure",
            "Expected failure but was success"
          )
        case Consequence.Failure(conclusion) =>
          MatchResult(
            false,
            s"Expected success but was failure with cause: ${_cause(conclusion)}",
            "Expected failure but was success"
          )
      }
    }

  def be_success(expected: String): Matcher[Consequence[String]] =
    Matcher { c =>
      c match {
        case Consequence.Success(actual) =>
          MatchResult(
            actual == expected,
            s"Expected success with value [$expected] but was [$actual]",
            s"Success value matched [$expected]"
          )
        case Consequence.Failure(conclusion) =>
          MatchResult(
            false,
            s"Expected success with value [$expected] but was failure with cause: ${_cause(conclusion)}",
            s"Expected failure but was success with value [$expected]"
          )
      }
    }

  // ---- failure (any) ----
  def be_failure[A]: Matcher[Consequence[A]] =
    Matcher { c =>
      c match {
        case Consequence.Success(_) =>
          MatchResult(
            false,
            "Expected failure but was success",
            "Expected success but was failure"
          )
        case Consequence.Failure(conclusion) =>
          MatchResult(
            true,
            "Expected failure but was success",
            s"Expected success but was failure with cause: ${_cause(conclusion)}"
          )
      }
    }

  // ---- failure with specific cause (exact match) ----

  def fail_with(expected: Cause): Matcher[Consequence[?]] =
    Matcher { c =>
      c match {
        case Consequence.Failure(conclusion) =>
          val actual = _cause(conclusion)
          MatchResult(
            actual.contains(expected),
            s"Expected failure with cause [$expected] but was [$actual]",
            s"Failure cause matched [$expected]"
          )
        case Consequence.Success(_) =>
          MatchResult(
            false,
            s"Expected failure with cause [$expected] but was success",
            s"Failure cause matched [$expected]"
          )
      }
    }

  def fail_argument_missing_with(
    operation: String,
    input: String
  ): Matcher[Consequence[?]] =
    Matcher { c =>
      c match {
        case Consequence.Failure(conclusion) =>
          val observation = conclusion.observation
          val expectedcause = Cause.Argument(Reason.Missing)
          val causeok = observation.cause.contains(expectedcause)
          val aspects = observation.descriptor.aspects
          val operationok = aspects.contains(Descriptor.Aspect.Operation(operation))
          val inputok = aspects.contains(
            Descriptor.Aspect.Input(name = Some(input), value = None)
          )
          val ok = causeok && operationok && inputok
          val reason =
            if (!causeok) s"cause mismatch: ${observation.cause}"
            else if (!operationok) s"missing operation aspect: ${Descriptor.Aspect.Operation(operation)}"
            else s"missing input aspect: ${Descriptor.Aspect.Input(name = Some(input), value = None)}"
          MatchResult(
            ok,
            s"Expected failure with argument-missing cause and aspects, but $reason",
            "Failure cause and aspects matched"
          )
        case Consequence.Success(_) =>
          MatchResult(
            false,
            "Expected failure but was success",
            "Expected success but was failure"
          )
      }
    }

  def fail_argument_redundant_with(
    operation: String,
    input: Option[String] = None
  ): Matcher[Consequence[?]] =
    Matcher { c =>
      c match {
        case Consequence.Failure(conclusion) =>
          val observation = conclusion.observation
          val expectedcause = Cause.Argument(Reason.Redundant)
          val causeok = observation.cause.contains(expectedcause)
          val aspects = observation.descriptor.aspects
          val operationaspect = Descriptor.Aspect.Operation(operation)
          val operationok = aspects.contains(operationaspect)
          val inputaspect = input.map(name => Descriptor.Aspect.Input(name = Some(name), value = None))
          val inputok = inputaspect.forall(aspect => aspects.contains(aspect))
          val ok = causeok && operationok && inputok
          val reason =
            if (!causeok) s"cause mismatch: ${observation.cause}"
            else if (!operationok) s"missing operation aspect: ${operationaspect}"
            else inputaspect.map(a => s"missing input aspect: ${a}").getOrElse("input aspect unexpected")
          MatchResult(
            ok,
            s"Expected failure with argument-redundant cause and aspects, but $reason",
            "Failure cause and aspects matched"
          )
        case Consequence.Success(_) =>
          MatchResult(
            false,
            "Expected failure but was success",
            "Expected success but was failure"
          )
      }
    }

  def fail_syntax_error_with(
    operation: Option[String] = None
  ): Matcher[Consequence[?]] =
    Matcher { c =>
      c match {
        case Consequence.Failure(conclusion) =>
          val observation = conclusion.observation
          val expectedcause = Cause.SyntaxError
          val causeok = observation.cause.contains(expectedcause)
          val aspects = observation.descriptor.aspects
          val operationaspect = operation.map(name => Descriptor.Aspect.Operation(name))
          val operationok = operationaspect.forall(aspect => aspects.contains(aspect))
          val ok = causeok && operationok
          val reason =
            if (!causeok) s"cause mismatch: ${observation.cause}"
            else operationaspect.map(a => s"missing operation aspect: ${a}").getOrElse("operation aspect unexpected")
          MatchResult(
            ok,
            s"Expected failure with syntax-error cause and aspects, but $reason",
            "Failure cause and aspects matched"
          )
        case Consequence.Success(_) =>
          MatchResult(
            false,
            "Expected failure but was success",
            "Expected success but was failure"
          )
      }
    }

  def fail_argument_validation_error: Matcher[Consequence[?]] =
    Matcher { c =>
      c match {
        case Consequence.Failure(conclusion) =>
          val observation = conclusion.observation
          val expectedcause = Cause.Argument(Reason.ValidationError)
          val ok = observation.cause.contains(expectedcause)
          MatchResult(
            ok,
            s"Expected failure with cause [$expectedcause] but was [${observation.cause}]",
            "Failure cause matched"
          )
        case Consequence.Success(_) =>
          MatchResult(
            false,
            "Expected failure but was success",
            "Expected success but was failure"
          )
      }
    }

  private def _cause(conclusion: Conclusion): Option[Cause] =
    conclusion.observation.cause
}
