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
final class I18nDescriptionSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "I18nDescription" should {
    "construct one locale-aware description from plain text" in {
      Given("a Japanese execution context and a plain description")
      given ExecutionContext = I18nSpecContext.create(Locale.JAPAN, "i18n-description-spec")

      When("the description is constructed and encoded")
      val description = I18nDescription("展示内容の説明")
      val encoded = description.encode

      Then("the root entry remains available in the plain representation")
      encoded shouldBe "展示内容の説明"
      description.toI18nString.entries.toVector shouldBe Vector(
        Locale.ROOT -> "展示内容の説明"
      )
    }

    "round-trip every locale entry through the shared codec" in {
      Given("a description with Japanese and English entries")
      given ExecutionContext = I18nSpecContext.create(Locale.JAPAN, "i18n-description-spec")
      val description = I18nDescription(I18nString(NonEmptyVector(
        Locale.JAPANESE -> "展示内容の説明",
        Vector(Locale.ENGLISH -> "Exhibition description")
      )))

      When("the description is encoded and decoded")
      val encoded = description.encode
      val decoded = I18nDescription.decode(encoded)

      Then("the wrapper preserves every locale-tagged value")
      encoded should startWith("{")
      decoded shouldBe Consequence.success(description)
      decoded.map(_.toI18nString.entries.toVector) shouldBe Consequence.success(
        Vector(
          Locale.JAPANESE -> "展示内容の説明",
          Locale.ENGLISH -> "Exhibition description"
        )
      )
    }

  }
}
