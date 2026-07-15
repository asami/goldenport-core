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
final class I18nBriefSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "I18nBrief" should {
    "construct one locale-aware brief from plain text" in {
      Given("a Japanese execution context and plain brief text")
      given ExecutionContext = I18nSpecContext.create(Locale.JAPAN, "i18n-brief-spec")

      When("the brief is constructed and encoded")
      val brief = I18nBrief("展示の概要")
      val encoded = brief.encode

      Then("the root entry remains available in the plain representation")
      encoded shouldBe "展示の概要"
      brief.toI18nString.entries.toVector shouldBe Vector(Locale.ROOT -> "展示の概要")
    }

    "round-trip every locale entry through the shared codec" in {
      Given("a brief with Japanese and English entries")
      given ExecutionContext = I18nSpecContext.create(Locale.JAPAN, "i18n-brief-spec")
      val brief = I18nBrief(I18nString(NonEmptyVector(
        Locale.JAPANESE -> "展示の概要",
        Vector(Locale.ENGLISH -> "Exhibition brief")
      )))

      When("the brief is encoded and decoded")
      val encoded = brief.encode
      val decoded = I18nBrief.decode(encoded)

      Then("the wrapper preserves every locale-tagged value")
      encoded should startWith("{")
      decoded shouldBe Consequence.success(brief)
      decoded.map(_.toI18nString.entries.toVector) shouldBe Consequence.success(
        Vector(Locale.JAPANESE -> "展示の概要", Locale.ENGLISH -> "Exhibition brief")
      )
    }
  }
}
