package org.goldenport.provisional.presentation

import org.goldenport.provisional.conclusion._

class SimpleConclusionPresenter extends ConclusionPresenter {
  protected def build(conclusion: Conclusion, ctx: PresentationContext): PresentedConclusion = {
    // val severity =
    //   conclusion.interpretation.kind match {
    //     case Kind.domain_failure => Severity.WARN
    //     case Kind.defect        => Severity.ERROR
    //   }
    // val title = MessageRef(conclusion.observation.taxonomy)
    // val summary = MessageRef(conclusion.observation.cause)
    // PresentedConclusion(
    //   title = title,
    //   summary = summary,
    //   severity = severity,
    //   responsibility = conclusion.interpretation.responsibility,
    //   userAction = conclusion.disposition.userAction
    // )
    ???
  }
}
