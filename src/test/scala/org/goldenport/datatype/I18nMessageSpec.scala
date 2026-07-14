package org.goldenport.datatype

import cats.data.NonEmptyVector
import java.util.Locale
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Dec. 22, 2025
 * @version Jul. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class I18nMessageSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "I18nMessage" should {
    "construct plain text as one root-locale entry" in {
      Given("plain message text")

      When("the message is constructed")
      val message = I18nMessage("通知があります")

      Then("the root entry and display value preserve the source text")
      message.entries.toVector shouldBe Vector(Locale.ROOT -> "通知があります")
      message.displayMessage shouldBe "通知があります"
    }

    "select the fixed root, English, and Japanese preference order" in {
      Given("messages whose declaration order differs from the fixed preference order")
      val withroot = I18nMessage(NonEmptyVector(
        Locale.JAPANESE -> "日本語",
        Vector(Locale.ENGLISH -> "English", Locale.ROOT -> "Root")
      ))
      val withoutroot = I18nMessage(NonEmptyVector(
        Locale.JAPANESE -> "日本語",
        Vector(Locale.ENGLISH -> "English")
      ))
      val japaneseonly = I18nMessage(NonEmptyVector(
        Locale.FRENCH -> "Francais",
        Vector(Locale.JAPANESE -> "日本語")
      ))

      When("display values are selected")
      val rootpreferred = withroot.displayMessage
      val englishpreferred = withoutroot.displayMessage
      val japanesepreferred = japaneseonly.displayMessage

      Then("root, English, and Japanese are selected in order when available")
      rootpreferred shouldBe "Root"
      englishpreferred shouldBe "English"
      japanesepreferred shouldBe "日本語"
    }

    "fall back to the first entry when no preferred locale exists" in {
      Given("French and German entries without root, English, or Japanese")
      val message = I18nMessage(NonEmptyVector(
        Locale.FRENCH -> "Francais",
        Vector(Locale.GERMAN -> "Deutsch")
      ))

      When("the display value is selected")
      val display = message.displayMessage

      Then("the first declared value is returned")
      display shouldBe "Francais"
      message.entries.toVector shouldBe Vector(
        Locale.FRENCH -> "Francais",
        Locale.GERMAN -> "Deutsch"
      )
    }
  }
}
