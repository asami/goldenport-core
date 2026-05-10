package org.goldenport.conclusion

import java.time.Instant
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.goldenport.Conclusion
import org.goldenport.observation.Phenomenon
import org.goldenport.observation.Severity
import org.goldenport.observation.Taxonomy
import org.goldenport.observation.{Observation, Origin}
import org.goldenport.observation.Cause
import org.goldenport.observation.Occurrence
import org.goldenport.observation.{Source, Channel, Substrate}
import org.goldenport.conclusion.{Interpretation, Disposition}
import org.goldenport.conclusion.Disposition.Responsibility
import org.goldenport.conclusion.presentation.{PresentationContext, SimpleConclusionPresenter}
import org.goldenport.conclusion.cli.CliConclusionRenderer

/*
 * @since   Jan. 25, 2026
 *  version Feb.  5, 2026
 *  version Mar.  4, 2026
 * @version May. 11, 2026
 * @author  ASAMI, Tomoharu
 */
class ConclusionPresenterSpec extends AnyWordSpec with Matchers {
  "SimpleConclusionPresenter + CLI renderer" should {
    "produce a valid exit code and message" in {
      val observation = Observation(
        Phenomenon.Rejection,
        taxonomy = Taxonomy.resourceNotFound,
        cause = Cause.empty,
        Instant.now(),
        occurrence = Some(
          Occurrence(
            source = Source.inMemory,
            channel = Channel.command,
            substrate = Substrate.jvm
          )
        ),
        origin = Some(Origin.Opaque("SpecSmoke"))
      )
      val interpretation = Interpretation(
        kind = Interpretation.Kind.DomainFailure,
        responsibility = Some(Responsibility.User)
      )
      val disposition = Disposition(
        userAction = Some(Disposition.UserAction.FixInput),
        responsibility = Some(Responsibility.User)
      )
      val status = Conclusion.Status()
      val conclusion = Conclusion(status, observation, interpretation, disposition)

      val presenter = new SimpleConclusionPresenter
      val presented = presenter.present(conclusion, PresentationContext("en"))
      presented.severity shouldBe Severity.Warn
      presented.userAction should contain(Disposition.UserAction.FixInput)

      val (code, message) = CliConclusionRenderer.render(presented)
      code shouldBe 1
      val expectedSummaryKey = presented.summary.key
      message should include("resource.not-found")
      message shouldBe s"[${presented.severity}]resource.not-found-${expectedSummaryKey}"
    }
  }
}
