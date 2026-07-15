package org.goldenport.value

import cats.data.NonEmptyVector
import java.util.Locale
import org.goldenport.datatype.I18nBrief
import org.goldenport.datatype.I18nDescription
import org.goldenport.datatype.I18nLabel
import org.goldenport.datatype.I18nString
import org.goldenport.datatype.I18nSummary
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jun. 23, 2026
 * @version Jul. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class DescriptiveAttributesSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "DescriptiveAttributes" should {
    "resolve effective precedence" which {
      "selects primary descriptive text with SmartDox-compatible precedence" in {
        Given("descriptive attributes with overlapping metadata fields")
        val attrs = DescriptiveAttributes(
          headline = Some(I18nBrief("Headline")),
          brief = Some(I18nBrief("Brief")),
          summary = Some(I18nSummary("Summary")),
          description = Some(I18nDescription("Description")),
          lead = Some(I18nSummary("Lead")),
          `abstract` = Some(I18nSummary("Abstract")),
          tooltip = Some(I18nLabel("Tooltip"))
        )

        When("effective descriptive strings are requested")
        val actual = Vector(
          attrs.effectiveHeadlineString(Locale.ENGLISH),
          attrs.effectiveBriefString(Locale.ENGLISH),
          attrs.effectiveSummaryString(Locale.ENGLISH),
          attrs.effectiveDescriptionString(Locale.ENGLISH),
          attrs.effectiveTooltipString(Locale.ENGLISH)
        )

        Then("each accessor selects its primary field")
        actual shouldBe Vector(
          Some("Headline"),
          Some("Brief"),
          Some("Summary"),
          Some("Description"),
          Some("Tooltip")
        )
      }

      "retains SmartDox-compatible fallback order when primary fields are absent" in {
        Given("descriptive attributes containing only fallback fields")
        val attrs = DescriptiveAttributes(
          brief = Some(I18nBrief("Brief")),
          lead = Some(I18nSummary("Lead")),
          `abstract` = Some(I18nSummary("Abstract"))
        )

        When("effective descriptive strings are requested")
        val actual = Vector(
          attrs.effectiveHeadlineString(Locale.ENGLISH),
          attrs.effectiveSummaryString(Locale.ENGLISH),
          attrs.effectiveDescriptionString(Locale.ENGLISH),
          attrs.effectiveTooltipString(Locale.ENGLISH)
        )

        Then("each accessor selects the first available fallback")
        actual shouldBe Vector(Some("Brief"), Some("Lead"), Some("Abstract"), Some("Brief"))
      }
    }

    "preserve localized descriptive values" which {
      "selects a locale from I18nDescription without collapsing stored entries" in {
        Given("a multilingual description stored in descriptive attributes")
        val sourceEntries = Vector(
          Locale.JAPANESE -> "展示内容の説明",
          Locale.ENGLISH -> "Exhibition description"
        )
        val attrs = DescriptiveAttributes(
          description = Some(I18nDescription(I18nString(NonEmptyVector(
            sourceEntries.head,
            sourceEntries.tail
          ))))
        )

        When("effective description strings are requested for available and unavailable locales")
        val japanese = attrs.effectiveDescriptionString(Locale.JAPAN)
        val fallback = attrs.effectiveDescriptionString(Locale.CANADA_FRENCH)

        Then("locale fallback returns display text and preserves the structured value")
        japanese shouldBe Some("展示内容の説明")
        fallback shouldBe Some("Exhibition description")
        attrs.effectiveDescription.map(_.entries.toVector) shouldBe Some(sourceEntries)
      }

      "selects headline and brief locales without collapsing stored entries" in {
        Given("multilingual headline and brief values stored in descriptive attributes")
        val headlineEntries = Vector(
          Locale.JAPANESE -> "展示案内",
          Locale.ENGLISH -> "Exhibition guide"
        )
        val briefEntries = Vector(
          Locale.JAPANESE -> "展示の概要",
          Locale.ENGLISH -> "Exhibition brief"
        )
        val attrs = DescriptiveAttributes(
          headline = Some(I18nBrief(I18nString(NonEmptyVector(
            headlineEntries.head,
            headlineEntries.tail
          )))),
          brief = Some(I18nBrief(I18nString(NonEmptyVector(
            briefEntries.head,
            briefEntries.tail
          ))))
        )

        When("effective headline and brief strings are requested in Japanese and French")
        val headline = Vector(
          attrs.effectiveHeadlineString(Locale.JAPAN),
          attrs.effectiveHeadlineString(Locale.CANADA_FRENCH)
        )
        val brief = Vector(
          attrs.effectiveBriefString(Locale.JAPAN),
          attrs.effectiveBriefString(Locale.CANADA_FRENCH)
        )

        Then("locale fallback returns each effective value and preserves both wrappers")
        headline shouldBe Vector(Some("展示案内"), Some("Exhibition guide"))
        brief shouldBe Vector(Some("展示の概要"), Some("Exhibition brief"))
        attrs.effectiveHeadline.map(_.entries.toVector) shouldBe Some(headlineEntries)
        attrs.effectiveBrief.map(_.entries.toVector) shouldBe Some(briefEntries)
      }

      "selects a summary-family fallback without collapsing stored entries" in {
        Given("a multilingual lead stored without a primary summary")
        val leadEntries = Vector(
          Locale.JAPANESE -> "展示への導入",
          Locale.ENGLISH -> "Exhibition lead"
        )
        val attrs = DescriptiveAttributes(
          lead = Some(I18nSummary(I18nString(NonEmptyVector(
            leadEntries.head,
            leadEntries.tail
          ))))
        )

        When("effective summary strings are requested in Japanese and French")
        val summary = Vector(
          attrs.effectiveSummaryString(Locale.JAPAN),
          attrs.effectiveSummaryString(Locale.CANADA_FRENCH)
        )

        Then("the lead supplies display text while its structured value remains intact")
        summary shouldBe Vector(Some("展示への導入"), Some("Exhibition lead"))
        attrs.effectiveSummary.map(_.entries.toVector) shouldBe Some(leadEntries)
        attrs.lead.map(_.toI18nString.entries.toVector) shouldBe Some(leadEntries)
      }
    }
  }
}
