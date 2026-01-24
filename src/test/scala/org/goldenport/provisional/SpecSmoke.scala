package org.goldenport.provisional

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.goldenport.provisional.observation.{Observation, Method, Origin}
import org.goldenport.provisional.conclusion._
import org.goldenport.provisional.presentation.{PresentationContext, SimpleConclusionPresenter}
import org.goldenport.provisional.cli.CliConclusionRenderer

class SpecSmoke extends AnyWordSpec with Matchers {
  "SimpleConclusionPresenter + CLI renderer" should {
    "produce a valid exit code and message" in {
      val observation = Observation(
        taxonomy = "resource.not_found",
        cause = "lookup",
        source = "in_memory",
        channel = "in_memory",
        substrate = "jvm",
        origin = Method("SpecSmoke")
      )
      val interpretation = Interpretation(
        kind = Kind.domain_failure,
        responsibility = Responsibility.user
      )
      val disposition = Disposition(
        userAction = Some(UserAction.fix_input),
        responsibility = Some(Responsibility.user)
      )
      val conclusion = Conclusion(observation, interpretation, disposition)

      val presenter = new SimpleConclusionPresenter
      val presented = presenter.present(conclusion, PresentationContext("en"))
      presented.severity shouldBe Severity.WARN
      presented.userAction should contain(UserAction.fix_input)

      val (code, message) = CliConclusionRenderer.render(presented)
      code shouldBe 1
      message should include("resource.not_found")
      message should include("lookup")
    }
  }
}
