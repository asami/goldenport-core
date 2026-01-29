package org.goldenport.test.matchers

import org.scalatest.matchers.{MatchResult, Matcher}
import org.goldenport.Conclusion
import org.goldenport.provisional.observation.Taxonomy
import org.goldenport.provisional.observation.Taxonomy.Symptom
import org.goldenport.test.matchers.MatcherSupport

/*
 * @since   Jan. 28, 2026
 * @version Jan. 28, 2026
 * @author  ASAMI, Tomoharu
 */
trait ConclusionMatchers {
  import ConclusionMatchers._

  final protected def be_equal_conclusion(expected: Conclusion): Matcher[Conclusion] =
    Matcher { c =>
      MatcherSupport.matchResult(expected.isMatch(c), c, expected)
    }

  final protected def be_invalid_failure_conclusion: Matcher[Conclusion] =
    Matcher { c =>
      matchResult(c, Symptom.Invalid)
    }

  final protected def be_format_error_failure_conclusion: Matcher[Conclusion] =
    Matcher { c =>
      matchResult(c, Symptom.FormatError)
    }
}

object ConclusionMatchers {
  def isMatch(c: Conclusion, s: Symptom): Boolean =
    c.observation.taxonomy.symptom == s

  def matchResult(c: Conclusion, s: Symptom): MatchResult =
    MatcherSupport.matchResult(isMatch(c, s), s, c.observation.taxonomy.symptom)
}
