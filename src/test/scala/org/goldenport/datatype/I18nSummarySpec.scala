package org.goldenport.datatype

import cats.data.NonEmptyVector
import java.util.Locale
import org.goldenport.Consequence
import org.goldenport.context.ExecutionContext
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Dec. 22, 2025
 * @version Jul. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class I18nSummarySpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "I18nSummary" should {
    "construct one locale-aware summary from plain text" in {
      Given("a Japanese execution context and plain summary text")
      given ExecutionContext = I18nSpecContext.create(Locale.JAPAN, "i18n-summary-spec")

      When("the summary is constructed and encoded")
      val summary = I18nSummary("展示内容の要約")
      val encoded = summary.encode

      Then("the root entry remains available in the plain representation")
      encoded shouldBe "展示内容の要約"
      summary.toI18nString.entries.toVector shouldBe Vector(Locale.ROOT -> "展示内容の要約")
    }

    "round-trip every locale entry through the shared codec" in {
      Given("a summary with Japanese and English entries")
      given ExecutionContext = I18nSpecContext.create(Locale.JAPAN, "i18n-summary-spec")
      val summary = I18nSummary(I18nString(NonEmptyVector(
        Locale.JAPANESE -> "展示内容の要約",
        Vector(Locale.ENGLISH -> "Exhibition summary")
      )))

      When("the summary is encoded and decoded")
      val encoded = summary.encode
      val decoded = I18nSummary.decode(encoded)

      Then("the wrapper preserves every locale-tagged value")
      encoded should startWith("{")
      decoded shouldBe Consequence.success(summary)
      decoded.map(_.toI18nString.entries.toVector) shouldBe Consequence.success(
        Vector(Locale.JAPANESE -> "展示内容の要約", Locale.ENGLISH -> "Exhibition summary")
      )
    }
  }
}
