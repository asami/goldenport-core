package org.goldenport.test.matchers

import org.scalatest.matchers.{MatchResult, Matcher}
import org.goldenport.Consequence
import org.goldenport.Conclusion
// import org.goldenport.observation.Cause
// import org.goldenport.observation.Cause.Reason
// import org.goldenport.observation.Descriptor
import org.goldenport.provisional.observation.Taxonomy
import org.goldenport.provisional.observation.Cause

/*
 * @since   May. 11, 2025
 *  version Jul.  1, 2025
 *  version Dec. 28, 2025
 *  version Jan.  3, 2026
 * @version Jan. 29, 2026
 * @author  ASAMI, Tomoharu
 */
trait ConsequenceMatchers {
  import ConsequenceMatchers._

  // ---- success ----
  final protected def be_success[A]: Matcher[Consequence[A]] =
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
            s"Expected success but was failure with cause: ${conclusion.cause}",
            "Expected failure but was success"
          )
      }
    }

  final protected def be_success(expected: String): Matcher[Consequence[String]] =
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
            s"Expected success with value [$expected] but was failure with cause: ${conclusion.cause}",
            s"Expected failure but was success with value [$expected]"
          )
      }
    }

  // ---- failure (any) ----
  final protected def be_failure[A]: Matcher[Consequence[A]] =
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
            s"Expected success but was failure with cause: ${conclusion.cause}"
          )
      }
    }

  final protected def be_argument_invalid_failure[A]: Matcher[Consequence[A]] =
    be_failure_with(Taxonomy.argumentInvalid)

  final protected def be_argument_missing_failure[A]: Matcher[Consequence[A]] =
    be_failure_with(Taxonomy.argumentMissing)

  final protected def be_argument_redundant_failure[A]: Matcher[Consequence[A]] =
    be_failure_with(Taxonomy.argumentRedundant)

  final protected def be_argument_format_error_failure[A]: Matcher[Consequence[A]] =
    be_failure_with(Taxonomy.argumentFormatError)

  final protected def be_operation_invalid_failure[A]: Matcher[Consequence[A]] =
    be_failure_with(Taxonomy.operationInvalid)

  final protected def be_failure_with[A](expected: Taxonomy): Matcher[Consequence[A]] =
    Matcher { c =>
      c match {
        case Consequence.Failure(conclusion) =>
          val actual = conclusion.observation.taxonomy
          MatchResult(
            isMatch(actual, expected),
            s"Expected failure with taxonomy [$expected] but was [$actual]",
            s"Failure taxonomy matched [$expected]"
          )
        case Consequence.Success(_) =>
          MatchResult(
            false,
            s"Expected failure with taxonomy [$expected] but was success",
            s"Failure taxonomy matched [$expected]"
          )
      }
    }

  // ---- failure with specific cause (exact match) ----

  def fail_with(expected: Cause): Matcher[Consequence[?]] =
    Matcher { c =>
      c match {
        case Consequence.Failure(conclusion) =>
          val actual = conclusion.cause
          MatchResult(
            isMatch(actual, expected),
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

  final protected def fail_argument_missing_with(
    operation: String,
    input: String
  ): Matcher[Consequence[?]] =
    Matcher { c =>
      c match {
        case Consequence.Failure(conclusion) =>
          val observation = conclusion.observation
//          val expectedcause = Cause.Argument(Reason.Missing)
//          val expectedcause = CauseArgumentMissing
//          val causeok = observation.cause.contains(expectedcause)
          val causeok = isMatch(observation.taxonomy, Taxonomy.argumentMissing)
          // val aspects = observation.descriptor.aspects
          // val operationok = aspects.contains(Descriptor.Aspect.Operation(operation))
          // val inputok = aspects.contains(
          //   Descriptor.Aspect.Input(name = Some(input), value = None)
          // )
          // val ok = causeok && operationok && inputok
          val ok = causeok
          val reason =
            if (!causeok) s"cause mismatch: ${observation.cause}"
            // else if (!operationok) s"missing operation aspect: ${Descriptor.Aspect.Operation(operation)}"
//            else s"missing input aspect: ${Descriptor.Aspect.Input(name = Some(input), value = None)}"
            else "missing input"
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

  final protected def fail_argument_redundant_with(
    operation: String,
    input: Option[String] = None
  ): Matcher[Consequence[?]] =
    Matcher { c =>
      c match {
        case Consequence.Failure(conclusion) =>
          val observation = conclusion.observation
//          val expectedcause = Cause.Argument(Reason.Redundant)
//          val expectedcause = CauseArgumentRedundant
//          val causeok = observation.cause.contains(expectedcause)
          val causeok = isMatch(observation.taxonomy, Taxonomy.argumentRedundant)
          // val aspects = observation.descriptor.aspects
          // val operationaspect = Descriptor.Aspect.Operation(operation)
          // val operationok = aspects.contains(operationaspect)
          // val inputaspect = input.map(name => Descriptor.Aspect.Input(name = Some(name), value = None))
          // val inputok = inputaspect.forall(aspect => aspects.contains(aspect))
          // val ok = causeok && operationok && inputok
          val ok = causeok
          val reason =
            if (!causeok) s"cause mismatch: ${observation.cause}"
            // else if (!operationok) s"missing operation aspect: ${operationaspect}"
            // else inputaspect.map(a => s"missing input aspect: ${a}").getOrElse("input aspect unexpected")
            else "input aspect unexpected"
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
          //val expectedcause = CauseSyntaxError
//          val causeok = observation.cause.contains(expectedcause)
          val causeok = isMatch(observation.taxonomy, Taxonomy.argumentSyntaxError)
          // val aspects = observation.descriptor.aspects
          // val operationaspect = operation.map(name => Descriptor.Aspect.Operation(name))
          // val operationok = operationaspect.forall(aspect => aspects.contains(aspect))
          // val ok = causeok && operationok
          val ok = causeok
          val reason =
            if (!causeok) s"cause mismatch: ${observation.cause}"
//            else operationaspect.map(a => s"missing operation aspect: ${a}").getOrElse("operation aspect unexpected")
            else "operation aspect unexpected"
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
          val expected = Taxonomy.argumentInvalid
//          val expectedcause = Cause.Argument(Reason.ValidationError)
          // val expectedcause = CauseArgumentValidationError
//          val ok = observation.cause.contains(expectedcause)
          val ok = isMatch(observation.taxonomy, expected)
          MatchResult(
            ok,
            s"Expected failure with cause [$expected] but was [${observation.taxonomy}]",
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
}

object ConsequenceMatchers {
  // TestMigration
  def isMatch(lhs: Cause, rhs: Cause): Boolean = lhs == rhs
  // def CauseArgumentMissing: Cause = Cause.argumentMissing
  // def CauseArgumentRedundant: Cause = Cause.argumentRedundant
  // def CauseArgumentValidationError: Cause = Cause.argumentValidationError
  // def CauseSyntaxError: Cause = Cause.syntaxError

  def isMatch(lhs: Taxonomy, rhs: Taxonomy): Boolean = lhs == rhs
}
