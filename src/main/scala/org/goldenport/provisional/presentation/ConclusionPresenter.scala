package org.goldenport.provisional.presentation

import org.goldenport.provisional.conclusion.Conclusion

abstract class ConclusionPresenter {
  final def present(conclusion: Conclusion, ctx: PresentationContext): PresentedConclusion =
    build(conclusion, ctx)

  protected def build(conclusion: Conclusion, ctx: PresentationContext): PresentedConclusion
}
