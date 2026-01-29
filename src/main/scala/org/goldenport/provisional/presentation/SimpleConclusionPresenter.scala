package org.goldenport.provisional.presentation

import org.goldenport.Conclusion
import org.goldenport.observation.Severity
import org.goldenport.provisional.conclusion.Disposition.Responsibility
import org.goldenport.provisional.conclusion.Interpretation.Kind

/*
 * @since   Jan. 25, 2026
 * @version Jan. 29, 2026
 * @author  ASAMI, Tomoharu
 */
class SimpleConclusionPresenter extends ConclusionPresenter {
  protected def build(conclusion: Conclusion, ctx: PresentationContext): ConclusionPresentation = {
    val severity =
      conclusion.interpretation.kind match {
        case Kind.DomainFailure => Severity.Warn
        case Kind.Defect        => Severity.Error
      }
    val title = MessageRef(conclusion.observation.taxonomy.print)
    val summaryKey = conclusion.observation.cause.show
    val summary = MessageRef(summaryKey)
    val responsibility = conclusion.disposition.responsibility.getOrElse(Responsibility.Developer)
    ConclusionPresentation(
      title = title,
      summary = summary,
      severity = severity,
      responsibility = responsibility,
      userAction = conclusion.disposition.userAction
    )
  }
}
