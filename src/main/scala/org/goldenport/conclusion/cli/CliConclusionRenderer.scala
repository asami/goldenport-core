package org.goldenport.conclusion.cli

import org.goldenport.observation.Severity
import org.goldenport.conclusion.presentation.ConclusionPresentation

/*
 * @since   Jan. 25, 2026
 *  version Apr. 11, 2026
 * @version May. 11, 2026
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
    val detail = conclusion.detailCode.map(x => s" detailCode=${x.code}").getOrElse("")
    val app = conclusion.appCode.map(x => s" appCode=${x}").getOrElse("") +
      conclusion.appStatus.map(x => s" appStatus=${x}").getOrElse("")
    val message = s"[${conclusion.severity}] status=${conclusion.status} statusText=${conclusion.statusText}${detail}${app} ${conclusion.title.key}-${conclusion.summary.key}"
    (exitCode, message)
  }
}
