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
final class I18nTitleSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "I18nTitle" should {
    "construct one locale-aware title from a plain string" in {
      Given("a Japanese execution context and a plain title")
      given ExecutionContext = I18nSpecContext.create(Locale.JAPAN, "i18n-title-spec")

      When("the title is constructed and encoded")
      val title = I18nTitle("展示会")
      val encoded = title.encode

      Then("the single entry remains available without an artificial locale container")
      encoded shouldBe "展示会"
      title.toI18nString.entries.toVector shouldBe Vector(Locale.ROOT -> "展示会")
      title.displayMessage(Locale.JAPANESE) shouldBe "展示会"
    }

    "round-trip every locale entry through the string codec" in {
      Given("a title with Japanese and English locale entries")
      given ExecutionContext = I18nSpecContext.create(Locale.JAPAN, "i18n-title-spec")
      val title = I18nTitle(I18nString(NonEmptyVector(
        Locale.JAPANESE -> "展示会",
        Vector(Locale.ENGLISH -> "Exhibition")
      )))

      When("the title is encoded and decoded")
      val encoded = title.encode
      val decoded = I18nTitle.decode(encoded)

      Then("the structured codec preserves every locale-tagged value")
      encoded should startWith("{")
      decoded shouldBe Consequence.success(title)
      decoded.map(_.toI18nString.entries.toVector) shouldBe Consequence.success(
        Vector(Locale.JAPANESE -> "展示会", Locale.ENGLISH -> "Exhibition")
      )
    }

    "select an effective display value without collapsing stored locales" in {
      Given("a multilingual title with Japanese and English entries")
      val title = I18nTitle(I18nString(NonEmptyVector(
        Locale.JAPANESE -> "展示会",
        Vector(Locale.ENGLISH -> "Exhibition")
      )))

      When("display values are requested for available and unavailable locales")
      val japanese = title.displayMessage(Locale.JAPAN)
      val fallback = title.displayMessage(Locale.CANADA_FRENCH)

      Then("language fallback selects an effective value and leaves source entries intact")
      japanese shouldBe "展示会"
      fallback shouldBe "Exhibition"
      title.toI18nString.entries.toVector shouldBe Vector(
        Locale.JAPANESE -> "展示会",
        Locale.ENGLISH -> "Exhibition"
      )
    }
  }

}
