package org.goldenport.datatype

import cats.data.NonEmptyVector
import java.util.Locale
import org.goldenport.Consequence
import org.goldenport.convert.ValueReader
import org.goldenport.record.Record
import io.circe.parser.decode
import io.circe.syntax.*
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 15, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class I18nValueReaderSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "I18n value readers" should {
    "preserve every locale in structured text" in {
      Given("an encoded Japanese and English title")
      val source = I18nString(NonEmptyVector(
        Locale.JAPANESE -> "通知",
        Vector(Locale.ENGLISH -> "Notification")
      ))
      val encoded = {
        given org.goldenport.context.ExecutionContext =
          I18nSpecContext.create(Locale.JAPAN, "i18n-value-reader-spec")
        I18nTitle(source).encode
      }

      When("the title ValueReader reads the structured text")
      val result = summon[ValueReader[I18nTitle]].readC(encoded)

      Then("the ordered locale entries survive")
      result.map(_.toI18nString.entries.toVector) shouldBe Consequence.success(
        Vector(
          Locale.JAPANESE -> "通知",
          Locale.ENGLISH -> "Notification"
        )
      )
    }

    "read the canonical structured Record representation" in {
      Given("a Record containing the canonical entries array")
      val record = Record.data(
        "entries" -> Vector(
          Vector("ja", "通知"),
          Vector("en", "Notification")
        )
      )

      When("the summary ValueReader reads the Record")
      val result = summon[ValueReader[I18nSummary]].readC(record)

      Then("the Record is decoded without collapsing locales")
      result.map(_.toI18nString.entries.toVector) shouldBe Consequence.success(
        Vector(
          Locale.JAPANESE -> "通知",
          Locale.ENGLISH -> "Notification"
        )
      )
    }

    "round-trip the locale map used at API Record boundaries" in {
      Given("a Japanese and English text value")
      val source = I18nString(NonEmptyVector(
        Locale.JAPANESE -> "通知",
        Vector(Locale.ENGLISH -> "Notification")
      ))

      When("the value is projected to and read from its API Record")
      val record = source.toRecord
      val result = summon[ValueReader[I18nString]].readC(record)

      Then("the locale keys and values survive without display selection")
      record.getString("ja") shouldBe Some("通知")
      record.getString("en") shouldBe Some("Notification")
      result shouldBe Consequence.success(source)
    }

    "provide readers for every shared semantic I18n wrapper" in {
      Given("plain text at a generated Record boundary")

      When("each semantic wrapper reader is summoned and applied")
      val results = Vector(
        summon[ValueReader[I18nTitle]].readC("Title").map(_.toI18nString),
        summon[ValueReader[I18nLabel]].readC("Label").map(_.toI18nString),
        summon[ValueReader[I18nBrief]].readC("Brief").map(_.toI18nString),
        summon[ValueReader[I18nSummary]].readC("Summary").map(_.toI18nString),
        summon[ValueReader[I18nDescription]].readC("Description").map(_.toI18nString),
        summon[ValueReader[I18nText]].readC("Text").map(_.toI18nString)
      )

      Then("all wrappers retain one root-locale value")
      results.foreach(_.isSuccess shouldBe true)
      results.map(_.toOption.get.entries.head._1) should contain only Locale.ROOT
    }

    "retain an already typed wrapper unchanged" in {
      Given("an existing title value")
      val title = I18nTitle("Existing")

      When("the title ValueReader reads it")
      val result = summon[ValueReader[I18nTitle]].readC(title)

      Then("the same semantic value is returned")
      result shouldBe Consequence.success(title)
    }

    "round-trip a semantic wrapper through its structured JSON codec" in {
      Given("a multilingual description")
      val description = I18nDescription(I18nString(NonEmptyVector(
        Locale.JAPANESE -> "説明",
        Vector(Locale.ENGLISH -> "Description")
      )))

      When("Circe encodes and decodes the wrapper")
      val result = decode[I18nDescription](description.asJson.noSpaces)

      Then("both locale entries remain structured")
      result shouldBe Right(description)
    }
  }
}
