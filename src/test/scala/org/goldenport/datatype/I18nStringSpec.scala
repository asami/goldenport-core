package org.goldenport.datatype

import cats.data.NonEmptyVector
import io.circe.parser.decode
import java.util.Locale
import org.goldenport.Consequence
import org.goldenport.context.ExecutionContext
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Dec. 22, 2025
 * @version Jul. 16, 2026
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

    "canonicalize valid BCP 47 locale tags at structured input boundaries" in {
      Given("a structured value whose locale tag uses noncanonical case")
      val source = """{"entries":[["EN-us","Localized title"]]}"""

      When("the structured value is decoded")
      val result = decode[I18nString](source)

      Then("the locale identity uses its canonical language tag")
      result.map(_.entries.toVector.map { case (locale, value) => locale.toLanguageTag -> value }) shouldBe
        Right(Vector("en-US" -> "Localized title"))
    }

    "reject malformed and duplicate locale identities" in {
      Given("one malformed tag and one structured value that repeats the same canonical locale")
      val malformed = """{"entries":[["en_US","Invalid"]]}"""
      val duplicate = """{"entries":[["en","First"],["EN","Second"]]}"""

      When("the structured values are decoded")
      val malformedresult = decode[I18nString](malformed)
      val duplicateresult = decode[I18nString](duplicate)

      Then("neither invalid identity enters the runtime value")
      malformedresult.isLeft shouldBe true
      duplicateresult.isLeft shouldBe true

      And("direct validated construction rejects duplicate locale objects")
      val entries = NonEmptyVector(
        Locale.ENGLISH -> "First",
        Vector(Locale.ENGLISH -> "Second")
      )
      I18nString.create(entries).isSuccess shouldBe false
      an[IllegalArgumentException] should be thrownBy I18nString(entries)
    }

    "enforce an explicitly configured allowed locale set without restricting unrestricted values" in {
      Given("a Japanese and English value plus a Japanese-only deployment locale set")
      val source = I18nString(NonEmptyVector(
        Locale.JAPANESE -> "通知",
        Vector(Locale.ENGLISH -> "Notification")
      ))

      When("the same value is validated with and without the explicit restriction")
      val unrestricted = I18nString.create(source.entries)
      val restricted = source.validateAllowedLocales(Set(Locale.JAPANESE))
      val allowed = source.validateAllowedLocales(Set(Locale.JAPANESE, Locale.ENGLISH))

      Then("absence of a restriction accepts all valid locales while an explicit set is exact")
      unrestricted shouldBe Consequence.success(source)
      restricted.isSuccess shouldBe false
      allowed shouldBe Consequence.success(source)
    }

    "apply the ExecutionContext allowed locale policy during plain and structured decoding" in {
      Given("a Japanese execution context restricted to the exact Japanese regional locale")
      given ExecutionContext = I18nSpecContext.create(
        Locale.JAPAN,
        "i18n-string-allowed-locale-spec",
        Some(Set(Locale.JAPAN))
      )
      val japanese = """{"entries":[["ja-JP","通知"]]}"""
      val english = """{"entries":[["en","Notification"]]}"""

      When("plain Japanese text and structured locale entries are decoded")
      val plainresult = I18nString.decode("通知")
      val japaneseresult = I18nString.decode(japanese)
      val englishresult = I18nString.decode(english)

      Then("the execution locale and matching structured locale are accepted")
      plainresult.map(_.entries.toVector) shouldBe Consequence.success(Vector(Locale.JAPAN -> "通知"))
      japaneseresult.map(_.entries.toVector) shouldBe Consequence.success(Vector(Locale.JAPAN -> "通知"))

      And("a well-formed but unsupported locale is rejected")
      englishresult.isSuccess shouldBe false
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

    "apply the canonical exact language-root neutral English Japanese and first-entry fallback order" in {
      Given("values that isolate each fallback level")
      val exactandlanguage = I18nString(NonEmptyVector(
        Locale.JAPAN -> "Exact Japanese region",
        Vector(Locale.JAPANESE -> "Japanese language")
      ))
      val languageonly = I18nString(NonEmptyVector.one(
        Locale.JAPANESE -> "Japanese language"
      ))
      val neutral = I18nString(NonEmptyVector(
        Locale.ROOT -> "Neutral",
        Vector(Locale.ENGLISH -> "English")
      ))
      val english = I18nString(NonEmptyVector(
        Locale.ENGLISH -> "English",
        Vector(Locale.JAPANESE -> "Japanese")
      ))
      val japanese = I18nString(NonEmptyVector(
        Locale.JAPANESE -> "Japanese",
        Vector(Locale.FRENCH -> "French")
      ))
      val first = I18nString(NonEmptyVector(
        Locale.FRENCH -> "French",
        Vector(Locale.GERMAN -> "German")
      ))

      When("display values are requested")
      val actual = Vector(
        exactandlanguage.displayMessage(Locale.JAPAN),
        languageonly.displayMessage(Locale.JAPAN),
        neutral.displayMessage(Locale.ITALIAN),
        english.displayMessage(Locale.ITALIAN),
        japanese.displayMessage(Locale.ITALIAN),
        first.displayMessage(Locale.ITALIAN)
      )

      Then("each fallback level is selected before the next one")
      actual shouldBe Vector(
        "Exact Japanese region",
        "Japanese language",
        "Neutral",
        "English",
        "Japanese",
        "French"
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
