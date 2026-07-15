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
final class I18nStringSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "I18nString" should {
    "decode plain text as one entry in the execution locale" in {
      Given("a Japanese execution context and plain text")
      given ExecutionContext = I18nSpecContext.create(Locale.JAPAN, "i18n-string-spec")

      When("the text is decoded")
      val decoded = I18nString.decode("通知")

      Then("one locale-tagged entry is retained")
      decoded.map(_.entries.toVector) shouldBe Consequence.success(
        Vector(Locale.JAPAN -> "通知")
      )
    }

    "round-trip every locale entry through structured JSON" in {
      Given("Japanese and English entries")
      given ExecutionContext = I18nSpecContext.create(Locale.JAPAN, "i18n-string-spec")
      val source = I18nString(NonEmptyVector(
        Locale.JAPANESE -> "通知",
        Vector(Locale.ENGLISH -> "Notification")
      ))

      When("the value is encoded and decoded")
      val encoded = source.encode
      val decoded = I18nString.decode(encoded)

      Then("the structured codec preserves entry order, locales, and values")
      encoded should startWith("{")
      decoded shouldBe Consequence.success(source)
    }

    "select effective language fallback without changing stored entries" in {
      Given("Japanese and English entries")
      val source = I18nString(NonEmptyVector(
        Locale.JAPANESE -> "通知",
        Vector(Locale.ENGLISH -> "Notification")
      ))

      When("display values are requested for Japanese and unavailable French locales")
      val japanese = source.displayMessage(Locale.JAPAN)
      val fallback = source.displayMessage(Locale.CANADA_FRENCH)

      Then("language fallback selects values and preserves the source vector")
      japanese shouldBe "通知"
      fallback shouldBe "Notification"
      source.entries.toVector shouldBe Vector(
        Locale.JAPANESE -> "通知",
        Locale.ENGLISH -> "Notification"
      )
    }

    "escape a plain leading brace instead of interpreting it as structured JSON" in {
      Given("a Japanese execution context and plain text beginning with a brace")
      given ExecutionContext = I18nSpecContext.create(Locale.JAPAN, "i18n-string-spec")
      val source = I18nString("{draft}")

      When("the value is encoded and decoded")
      val encoded = source.encode
      val decoded = I18nString.decode(encoded)

      Then("the text survives as one plain execution-locale entry")
      encoded shouldBe "\\{draft}"
      decoded.map(_.entries.toVector) shouldBe Consequence.success(
        Vector(Locale.JAPAN -> "{draft}")
      )
    }
  }

}
