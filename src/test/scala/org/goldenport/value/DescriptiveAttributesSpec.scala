package org.goldenport.value

import java.util.Locale

import org.goldenport.datatype.I18nBrief
import org.goldenport.datatype.I18nDescription
import org.goldenport.datatype.I18nLabel
import org.goldenport.datatype.I18nSummary
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jun. 23, 2026
 * @version Jun. 23, 2026
 * @author  ASAMI, Tomoharu
 */
class DescriptiveAttributesSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "DescriptiveAttributes" should {
    "resolve effective descriptive text with SmartDox-compatible precedence" in {
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

      Then("effective accessors follow the SmartDox Explanation precedence")
      attrs.effectiveHeadlineString(Locale.ENGLISH) should be(Some("Headline"))
      attrs.effectiveBriefString(Locale.ENGLISH) should be(Some("Brief"))
      attrs.effectiveSummaryString(Locale.ENGLISH) should be(Some("Summary"))
      attrs.effectiveDescriptionString(Locale.ENGLISH) should be(Some("Description"))
      attrs.effectiveTooltipString(Locale.ENGLISH) should be(Some("Tooltip"))

      And("fallback order is stable when primary fields are absent")
      val fallback = DescriptiveAttributes(
        brief = Some(I18nBrief("Brief")),
        lead = Some(I18nSummary("Lead")),
        `abstract` = Some(I18nSummary("Abstract"))
      )
      fallback.effectiveHeadlineString(Locale.ENGLISH) should be(Some("Brief"))
      fallback.effectiveSummaryString(Locale.ENGLISH) should be(Some("Lead"))
      fallback.effectiveDescriptionString(Locale.ENGLISH) should be(Some("Abstract"))
      fallback.effectiveTooltipString(Locale.ENGLISH) should be(Some("Brief"))
    }
  }
}
