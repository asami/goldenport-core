package org.goldenport.provisional.cli

import org.goldenport.provisional.conclusion.Severity
import org.goldenport.provisional.presentation.PresentedConclusion

object CliConclusionRenderer {
  def render(conclusion: PresentedConclusion): (Int, String) = {
    val exitCode = conclusion.severity match {
      case Severity.INFO  => 0
      case Severity.WARN  => 1
      case Severity.ERROR => 2
    }
    val message = s"[${conclusion.severity}] ${conclusion.title.key} - ${conclusion.summary.key}"
    (exitCode, message)
  }
}
