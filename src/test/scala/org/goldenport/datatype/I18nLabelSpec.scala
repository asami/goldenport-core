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
final class I18nLabelSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "I18nLabel" should {
    "construct one locale-aware label from plain text" in {
      Given("a Japanese execution context and a plain label")
      given ExecutionContext = I18nSpecContext.create(Locale.JAPAN, "i18n-label-spec")

      When("the label is constructed and encoded")
      val label = I18nLabel("送信")
      val encoded = label.encode

      Then("the root entry remains available in the plain representation")
      encoded shouldBe "送信"
      label.toI18nString.entries.toVector shouldBe Vector(Locale.ROOT -> "送信")
    }

    "round-trip every locale entry through the shared codec" in {
      Given("a label with Japanese and English entries")
      given ExecutionContext = I18nSpecContext.create(Locale.JAPAN, "i18n-label-spec")
      val label = I18nLabel(I18nString(NonEmptyVector(
        Locale.JAPANESE -> "送信",
        Vector(Locale.ENGLISH -> "Send")
      )))

      When("the label is encoded and decoded")
      val encoded = label.encode
      val decoded = I18nLabel.decode(encoded)

      Then("the wrapper preserves the shared locale-tagged value")
      encoded should startWith("{")
      decoded shouldBe Consequence.success(label)
      decoded.map(_.toI18nString.entries.toVector) shouldBe Consequence.success(
        Vector(Locale.JAPANESE -> "送信", Locale.ENGLISH -> "Send")
      )
    }
  }
}
