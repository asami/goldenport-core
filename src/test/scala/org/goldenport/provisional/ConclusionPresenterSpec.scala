package org.goldenport.provisional

import java.time.Instant
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.goldenport.Conclusion
import org.goldenport.observation.Phenomenon
import org.goldenport.observation.Severity
import org.goldenport.provisional.observation.Taxonomy
import org.goldenport.provisional.observation.{Observation, Origin}
import org.goldenport.provisional.observation.Cause
import org.goldenport.provisional.observation.Occurrence
import org.goldenport.provisional.observation.{Source, Channel, Substrate}
import org.goldenport.provisional.conclusion.{Interpretation, Disposition}
import org.goldenport.provisional.conclusion.Disposition.Responsibility
import org.goldenport.provisional.presentation.{PresentationContext, SimpleConclusionPresenter}
import org.goldenport.provisional.cli.CliConclusionRenderer

/*
 * @since   Jan. 25, 2026
 * @version Jan. 29, 2026
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
      val status = Conclusion.Status.badRequest
      val conclusion = Conclusion(status, observation, interpretation, disposition)

      val presenter = new SimpleConclusionPresenter
      val presented = presenter.present(conclusion, PresentationContext("en"))
      presented.severity shouldBe Severity.Warn
      presented.userAction should contain(Disposition.UserAction.FixInput)

      val (code, message) = CliConclusionRenderer.render(presented)
      code shouldBe 1
      println(message)
      message should include("Resource.NotFound")
      message should include("Cause()")
    }
  }
}
