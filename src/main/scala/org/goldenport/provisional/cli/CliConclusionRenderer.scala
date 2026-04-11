package org.goldenport.provisional.cli

import org.goldenport.observation.Severity
import org.goldenport.provisional.presentation.ConclusionPresentation

/*
 * @since   Jan. 25, 2026
 * @version Apr. 11, 2026
 * @author  ASAMI, Tomoharu
 */
object CliConclusionRenderer {
  def render(conclusion: ConclusionPresentation): (Int, String) = {
    val exitCode = conclusion.severity match {
      case Severity.Fatal => 2
      case Severity.Info  => 0
      case Severity.Warn  => 1
      case Severity.Error => 2
      case Severity.Debug => 0
      case Severity.Trace => 0
    }
    val message = s"[${conclusion.severity}]${conclusion.title.key}-${conclusion.summary.key}"
    (exitCode, message)
  }
}
