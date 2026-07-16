package org.goldenport.datatype

import cats.data.NonEmptyVector
import java.util.Locale
import org.goldenport.Consequence
import org.goldenport.context.ExecutionContext
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class SemanticTextNormalizationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Semantic text runtime values" should {
    "preserve the exact authored representation instead of normalizing identity or display text" in {
      Given("mixed-case nonlocalized text and locale-aware text with significant surrounding spaces")
      val source = "  Mixed CASE Cafe\u0301 日本語  "

      When("the accepted semantic runtime families construct their values")
      val name = Name(source)
      val localized = Vector(
        "label" -> I18nLabel(source).toI18nString,
        "title" -> I18nTitle(source).toI18nString,
        "brief" -> I18nBrief(source).toI18nString,
        "summary" -> I18nSummary(source).toI18nString,
        "description" -> I18nDescription(source).toI18nString,
        "text" -> I18nText(source).toI18nString
      )

      Then("the stable name keeps the exact nonlocalized identity")
      name.value shouldBe source

      And("every locale-aware family keeps the exact root-locale text")
      localized.map { case (role, value) => role -> value.entries.toVector } shouldBe Vector(
        "label" -> Vector(Locale.ROOT -> source),
        "title" -> Vector(Locale.ROOT -> source),
        "brief" -> Vector(Locale.ROOT -> source),
        "summary" -> Vector(Locale.ROOT -> source),
        "description" -> Vector(Locale.ROOT -> source),
        "text" -> Vector(Locale.ROOT -> source)
      )
    }

    "apply preservation independently to every authored locale entry" in {
      Given("Japanese and English entries whose authored whitespace and case differ")
      given ExecutionContext = I18nSpecContext.create(Locale.JAPAN, "semantic-text-normalization-spec")
      val source = I18nString(NonEmptyVector(
        Locale.JAPANESE -> "  展示の概要  ",
        Vector(Locale.ENGLISH -> "  Exhibition SUMMARY  ")
      ))

      When("each locale-aware semantic wrapper receives the same multilingual value")
      val localized = Vector(
        I18nLabel(source).toI18nString,
        I18nTitle(source).toI18nString,
        I18nBrief(source).toI18nString,
        I18nSummary(source).toI18nString,
        I18nDescription(source).toI18nString,
        I18nText(source).toI18nString
      )
      val decoded = I18nString.decode(source.encode)

      Then("no wrapper trims, folds case, or collapses either locale entry")
      localized.map(_.entries.toVector).distinct shouldBe Vector(source.entries.toVector)

      And("the shared storage codec preserves the same authored representation")
      decoded.map(_.entries.toVector) shouldBe Consequence.success(source.entries.toVector)
    }
  }
}
