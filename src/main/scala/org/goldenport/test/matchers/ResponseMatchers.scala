package org.goldenport.test.matchers

import org.goldenport.Consequence
import org.goldenport.Conclusion
import org.goldenport.protocol.Response
import org.goldenport.test.matchers.MatcherSupport._
import org.scalatest.matchers.{MatchResult, Matcher}

/*
 * @since   Jan. 15, 2026
 * @version Jan. 15, 2026
 * @author  ASAMI, Tomoharu
 */
trait ResponseMatchers {
  final def be_success_response(expected: String): Matcher[Consequence[Response]] =
    Matcher {
      case Consequence.Success(actual) => actual match {
        case Response.Scalar(value) => matchResult(expected, value)
        case m => matchResult(expected, m)
      }
      case Consequence.Failure(conclusion) => matchFailure(conclusion)
    }

  final def be_not_found_response: Matcher[Consequence[Response]] =
    Matcher {
      case Consequence.Success(actual) => matchFailureUnexpectedSuccess(actual)
      case Consequence.Failure(conclusion) => matchResultNotFound(conclusion)
    }
}
