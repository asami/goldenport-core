package org.goldenport.record

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Dec. 22, 2025
 *  version Dec. 22, 2025
 * @version Mar.  4, 2026
 * @author  ASAMI, Tomoharu
 */
class RecordSpec extends AnyWordSpec
  with ScalaCheckDrivenPropertyChecks
  with Matchers {

  "Record" should {  "satisfy basic properties" in {
    pending
  }

  "preserve invariants" in {
    pending
  }

  "render nested records as block-style YAML without float tag for integers" in {
    val rec = Record.data(
      "status" -> Record.data("code" -> 400),
      "observation" -> Record.data(
        "taxonomy" -> Record.data(
          "category" -> "argument",
          "symptom" -> "domain-value"
        )
      ),
      "interpretation" -> Record.data(
        "kind" -> "domain-failure"
      )
    )

    val yaml = rec.toYamlString

    yaml should include ("status:")
    yaml should include ("  code: 400")
    yaml should include ("observation:")
    yaml should include ("  taxonomy:")
    yaml should include ("    category: argument")
    yaml should include ("    symptom: domain-value")
    yaml should include ("interpretation:")
    yaml should include ("  kind: domain-failure")
    yaml should not include ("status: {")
    yaml should not include ("taxonomy: {")
    yaml should not include ("interpretation: {")
    yaml should not include ("!!float")
  }
  }
}
