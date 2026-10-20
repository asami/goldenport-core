package org.goldenport

import org.goldenport.conclusion.{Disposition, Interpretation}
import org.goldenport.observation.{Cause, Observation, Taxonomy}
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Oct. 19, 2026
 * @version Oct. 19, 2026
 */
final class ConclusionHttpStatusSpec extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with TableDrivenPropertyChecks
  with ScalaCheckDrivenPropertyChecks {

  "Conclusion HTTP status" should {
    "interpret conflict symptoms" which {
      "assigns 409 to every taxonomy category" in {
        forAll(Table("category", Taxonomy.Category.values.toIndexedSeq*)) { category =>
          Given(s"a Conflict symptom in the ${category.name} category")
          val taxonomy = Taxonomy(category, Taxonomy.Symptom.Conflict)

          When("the taxonomy is interpreted as an HTTP status")
          val status = Conclusion.Status.webCodeOf(taxonomy)

          Then("the conflict symptom takes precedence over the category fallback")
          status shouldBe Conclusion.WebCode.Conflict
          status.code shouldBe 409
        }
      }

      "materializes 409 independently of category and diagnostic message" in {
        forAll(Gen.oneOf(Taxonomy.Category.values.toIndexedSeq), Gen.alphaStr) { (category, message) =>
          Given(s"a Conflict observation in the ${category.name} category with an arbitrary message")
          val taxonomy = Taxonomy(category, Taxonomy.Symptom.Conflict)
          val observation = Observation.failure(taxonomy, Cause.message(message), None)

          When("a failure Conclusion materializes its generated status")
          val conclusion = Conclusion(
            Conclusion.Status(),
            observation,
            Interpretation.domainFailure,
            Disposition.none
          )

          Then("the stored status expresses the conflict as HTTP 409")
          conclusion.status.webCode shouldBe Conclusion.WebCode.Conflict
          conclusion.status.webCode.code shouldBe 409
        }
      }
    }

    "materialize public conflict failures" which {
      "assigns 409 to a state conflict" in {
        Given("an existing state that conflicts with a requested transition")
        val message = "The editing session has already advanced"

        When("the public stateConflict helper describes the failure")
        val result = Consequence.stateConflict[Unit](message)

        Then("the failure carries a State Conflict taxonomy and a stored HTTP 409 status")
        val conclusion = _failure(result)
        conclusion.observation.taxonomy shouldBe Taxonomy.stateConflict
        conclusion.status.webCode shouldBe Conclusion.WebCode.Conflict
        conclusion.status.webCode.code shouldBe 409
      }

      "assigns 409 to an operation conflict" in {
        Given("an operation that conflicts with an earlier request")
        val operation = "applyEditingChange"

        When("the public operationConflict helper describes the failure")
        val result = Consequence.operationConflict[Unit](operation, Nil)

        Then("the failure carries an Operation Conflict taxonomy and a stored HTTP 409 status")
        val conclusion = _failure(result)
        conclusion.observation.taxonomy shouldBe Taxonomy(
          Taxonomy.Category.Operation,
          Taxonomy.Symptom.Conflict
        )
        conclusion.status.webCode shouldBe Conclusion.WebCode.Conflict
        conclusion.status.webCode.code shouldBe 409
      }
    }

    "preserve other generated statuses" which {
      "retains 500 for an ordinary invalid state" in {
        Given("an invalid state without a conflict symptom")
        val message = "The editing session is uninitialized"

        When("the public stateInvalid helper describes the failure")
        val result = Consequence.stateInvalid[Unit](message)

        Then("the State Invalid failure retains HTTP 500")
        val conclusion = _failure(result)
        conclusion.observation.taxonomy shouldBe Taxonomy(
          Taxonomy.Category.State,
          Taxonomy.Symptom.Invalid
        )
        conclusion.status.webCode.code shouldBe 500
      }

      "retains 400 for invalid arguments and values" in {
        forAll(Table("category", "argument", "value")) { category =>
          Given(s"an invalid $category without a conflict symptom")
          val message = "The supplied revision is invalid"

          When("the corresponding public validation helper describes the failure")
          val result = category match {
            case "argument" => Consequence.argumentInvalid[Unit](message)
            case _ => Consequence.valueInvalid[Unit](message)
          }

          Then("the validation failure retains HTTP 400")
          _failure(result).status.webCode.code shouldBe 400
        }
      }

      "retains 401 and 403 for authentication and permission failures" in {
        forAll(Table("status", 401, 403)) { status =>
          Given("a security failure requiring authentication or permission")
          val message = "The editing operation requires access"

          When("the corresponding public security helper describes the failure")
          val result = status match {
            case 401 => Consequence.securityAuthenticationRequired[Unit](message)
            case _ => Consequence.securityPermissionDenied[Unit](message)
          }

          Then(s"the security failure retains HTTP $status")
          _failure(result).status.webCode.code shouldBe status
        }
      }

      "retains 404 for a missing resource" in {
        Given("a requested editing resource that does not exist")
        val message = "The editing session was not found"

        When("the public resourceNotFound helper describes the failure")
        val result = Consequence.resourceNotFound[Unit](message)

        Then("the missing resource failure retains HTTP 404")
        _failure(result).status.webCode.code shouldBe 404
      }

      "retains 200 for a successful interpretation even with a Conflict observation" in {
        Given("a Conflict observation with a successful interpretation")
        val observation = Observation.failure(
          Taxonomy.stateConflict,
          Cause.message("The observation does not override success"),
          None
        )

        When("the successful Conclusion materializes its generated status")
        val conclusion = Conclusion(
          Conclusion.Status(),
          observation,
          Interpretation.success,
          Disposition.none
        )

        Then("success takes precedence and retains HTTP 200")
        conclusion.status.webCode shouldBe Conclusion.WebCode.Ok
        conclusion.status.webCode.code shouldBe 200
      }
    }
  }

  private def _failure[A](result: Consequence[A]): Conclusion = result match {
    case Consequence.Failure(conclusion) => conclusion
    case _ => fail("The semantic failure helper must return Consequence.Failure")
  }
}
