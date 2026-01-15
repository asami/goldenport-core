package org.goldenport.test.matchers

import org.goldenport.Consequence
import org.goldenport.Conclusion
import org.goldenport.text.Presentable
import org.scalatest.matchers.{MatchResult, Matcher}

/*
 * @since   Jan. 15, 2026
 * @version Jan. 15, 2026
 * @author  ASAMI, Tomoharu
 */
object MatcherSupport {
  def matchResult(expected: Any, actual: Any): MatchResult =
    matchResult(expected == actual, expected, actual)

  def matchResult(condition: Boolean, expected: Any, actual: Any): MatchResult =
    MatchResult(
      condition,
      s"Expected success with value [${_show(expected)}] but was [${_show(actual)}]",
      s"Value unexpectedly matched [${_show(expected)}]"
    )

  def matchResultNotFound(c: Conclusion): MatchResult =
    c match {
      case _ => MatchResult(
        true, // TODO
        s"Expected not found failure: ${c.message}",
        s"Unexpeced success"
      )
    }

  def matchFailure(c: Conclusion): MatchResult =
    MatchResult(
      false,
      s"Expected success but was failure with cause: ${c.message}",
      s"Expected failure but was success: ${c.message}"
    )

  def matchFailureUnexpectedSuccess(actual: Any): MatchResult =
    MatchResult(
      false,
      s"Expected failure",
      s"Expected failure but was success: ${_show(actual)}}"
    )

  private inline def _show(p: Any) = Presentable.show(p)
}
