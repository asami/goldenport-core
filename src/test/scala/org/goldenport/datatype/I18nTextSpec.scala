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
final class I18nTextSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "I18nText" should {
    "construct one locale-aware body from plain text" in {
      Given("a Japanese execution context and plain body text")
      given ExecutionContext = I18nSpecContext.create(Locale.JAPAN, "i18n-text-spec")

      When("the text is constructed and encoded")
      val text = I18nText("展示内容の本文")
      val encoded = text.encode

      Then("the root entry remains available in the plain representation")
      encoded shouldBe "展示内容の本文"
      text.toI18nString.entries.toVector shouldBe Vector(Locale.ROOT -> "展示内容の本文")
    }

    "round-trip every locale entry through the shared codec" in {
      Given("body text with Japanese and English entries")
      given ExecutionContext = I18nSpecContext.create(Locale.JAPAN, "i18n-text-spec")
      val text = I18nText(I18nString(NonEmptyVector(
        Locale.JAPANESE -> "展示内容の本文",
        Vector(Locale.ENGLISH -> "Exhibition body text")
      )))

      When("the text is encoded and decoded")
      val encoded = text.encode
      val decoded = I18nText.decode(encoded)

      Then("the wrapper preserves every locale-tagged value")
      encoded should startWith("{")
      decoded shouldBe Consequence.success(text)
      decoded.map(_.toI18nString.entries.toVector) shouldBe Consequence.success(
        Vector(Locale.JAPANESE -> "展示内容の本文", Locale.ENGLISH -> "Exhibition body text")
      )
    }
  }
}
