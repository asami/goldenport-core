package org.goldenport.error

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.goldenport.Conclusion
import org.goldenport.conclusion.{Disposition, Interpretation}
import org.goldenport.observation.{Cause, Taxonomy}

/*
 * @since   May. 11, 2026
 * @version May. 11, 2026
 * @author  ASAMI, Tomoharu
 */
class DetailCodeSpec extends AnyWordSpec with Matchers {
  "DetailCode" should {
    "be stored on Conclusion.Status after materialization" in {
      val conclusion = _conclusion(
        taxonomy = Taxonomy.argumentMissing,
        cause = Cause.Kind.Format,
        interpretation = Interpretation.domainFailure,
        disposition = Disposition.argumentMissing
      )

      conclusion.status.detailCode shouldBe Some(DetailCode(10301020100L))
    }

    "generate a stable numeric code from structured Conclusion" in {
      val conclusion = _conclusion(
        taxonomy = Taxonomy.argumentMissing,
        cause = Cause.Kind.Format,
        interpretation = Interpretation.domainFailure,
        disposition = Disposition.argumentMissing
      )

      DetailCode.generated(conclusion).code shouldBe 10301020100L
      DetailCode.dimensions(conclusion) shouldBe DetailCode.Dimensions(
        category = 1,
        symptom = 3,
        cause = 1,
        interpretation = 2,
        userAction = 1,
        responsibility = 0
      )
    }

    "change when structured classification changes" in {
      val missing = _conclusion(Taxonomy.argumentMissing, Cause.Kind.Format)
      val invalid = _conclusion(Taxonomy.argumentInvalid, Cause.Kind.Policy)

      DetailCode.generated(missing).code should not be DetailCode.generated(invalid).code
      DetailCode.generated(invalid).code shouldBe 10803020100L
    }

    "not change when only the message changes" in {
      val a = Conclusion.simple("first message")
      val b = Conclusion.simple("second message")

      DetailCode.generated(a).code shouldBe DetailCode.generated(b).code
    }

    "store web code generated from Conclusion taxonomy" in {
      val conclusion = Conclusion.simple("coded").copy(
        observation = Conclusion.simple("coded").observation.copy(
          taxonomy = Taxonomy.argumentInvalid
        )
      )

      conclusion.status.webCode shouldBe Conclusion.WebCode.BadRequest
    }

    "derive not-found web code from symptom before category defaults" in {
      val conclusion = Conclusion.simple("missing").copy(
        observation = Conclusion.simple("missing").observation.copy(
          taxonomy = Taxonomy.resourceNotFound
        )
      )

      conclusion.status.webCode shouldBe Conclusion.WebCode.NotFound
    }

    "derive invalid state as an internal server error" in {
      val previous = Conclusion.simple("payload").copy(
        observation = Conclusion.simple("payload").observation.copy(
          taxonomy = Taxonomy.resourceNotFound
        )
      )
      val conclusion = Conclusion.stateInvalid(
        "metadata points at a missing payload",
        previous = Some(previous)
      )

      conclusion.observation.taxonomy shouldBe Taxonomy.stateInvalid
      conclusion.observation.cause.kind shouldBe Some(Cause.Kind.Inconsistency)
      conclusion.status.webCode shouldBe Conclusion.WebCode.InternalServerError
      conclusion.status.toRecord.getString("statusText") shouldBe Some("Internal Server Error")
      conclusion.previous shouldBe Some(previous)
    }

    "preserve application status metadata without overriding generated detail code" in {
      val base = Conclusion.simple("coded")
      val conclusion = base.copy(
        status = base.status.copy(
          appCode = Some(2001001L),
          appStatus = Some("quota-exceeded")
        )
      )

      conclusion.status.appCode shouldBe Some(2001001L)
      conclusion.status.appStatus shouldBe Some("quota-exceeded")
      conclusion.status.detailCode shouldBe Some(DetailCode.generated(conclusion))
    }
  }

  private def _conclusion(
    taxonomy: Taxonomy,
    cause: Cause.Kind,
    interpretation: Interpretation = Interpretation.domainFailure,
    disposition: Disposition = Disposition.argumentMissing
  ): Conclusion = {
    val base = Conclusion.simple("structured")
    base.copy(
      observation = base.observation.copy(
        taxonomy = taxonomy,
        cause = Cause(kind = Some(cause))
      ),
      interpretation = interpretation,
      disposition = disposition
    )
  }
}
