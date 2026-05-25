package org.goldenport.record

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.goldenport.Consequence

/*
 * @since   Dec. 22, 2025
 *  version Dec. 22, 2025
 *  version Mar.  4, 2026
 * @version May. 26, 2026
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

  "normalize boundary keys to canonical camel names" in {
    RecordKeyNaming.toCanonicalCamelName("loginName") shouldEqual "loginName"
    RecordKeyNaming.toCanonicalCamelName("login_name") shouldEqual "loginName"
    RecordKeyNaming.toCanonicalCamelName("login-name") shouldEqual "loginName"
    RecordKeyNaming.toCanonicalCamelName("login.name") shouldEqual "loginName"
    RecordKeyNaming.toSnakeColumnName("loginName") shouldEqual "login_name"
  }

  "normalize known field aliases and preserve unknown fields" in {
    val record = Record.data(
      "login-name" -> "alice",
      "display_label" -> "Alice",
      "unknown-field" -> "kept"
    )

    RecordKeyNaming.normalizeKnownKeys(record, Set("loginName", "displayLabel")) match {
      case Consequence.Success(normalized) =>
        normalized.getString("loginName") shouldEqual Some("alice")
        normalized.getString("displayLabel") shouldEqual Some("Alice")
        normalized.getString("unknown-field") shouldEqual Some("kept")
      case Consequence.Failure(conclusion) => fail(conclusion.toString)
    }
  }

  "reject duplicate aliases for the same known field" in {
    val record = Record.data(
      "loginName" -> "alice",
      "login_name" -> "bob"
    )

    RecordKeyNaming.normalizeKnownKeys(record, Set("loginName")) shouldBe a[Consequence.Failure[?]]
  }
  }
}
