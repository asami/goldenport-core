package org.goldenport.conclusion.presentation

import org.goldenport.Conclusion
import org.goldenport.observation.Severity
import org.goldenport.conclusion.Disposition.UserAction
import org.goldenport.conclusion.Disposition.Responsibility
import org.goldenport.error.DetailCode

/*
 * @since   Jan. 25, 2026
 *  version Jan. 29, 2026
 * @version May. 11, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class ConclusionPresenter {
  final def present(conclusion: Conclusion, ctx: PresentationContext): ConclusionPresentation =
    build(conclusion, ctx)

  protected def build(conclusion: Conclusion, ctx: PresentationContext): ConclusionPresentation
}

case class MessageRef(key: String, args: List[String] = Nil)

case class PresentationContext(locale: String)

case class ConclusionPresentation(
  title: MessageRef,
  summary: MessageRef,
  severity: Severity,
  responsibility: Responsibility,
  userAction: Option[UserAction],
  status: Int,
  statusText: String,
  detailCode: Option[DetailCode],
  appCode: Option[Long],
  appStatus: Option[String]
)
