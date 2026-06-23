package org.goldenport.value

import java.util.Locale

import org.goldenport.datatype.I18nBrief
import org.goldenport.datatype.I18nDescription
import org.goldenport.datatype.I18nLabel
import org.goldenport.datatype.I18nString
import org.goldenport.datatype.I18nSummary

/*
 * @since   Aug.  1, 2025
 *  version Aug.  2, 2025
 *  version Oct.  7, 2025
 *  version Dec. 30, 2025
 *  version Jan. 21, 2026
 *  version Mar. 29, 2026
 *  version May.  3, 2026
 * @version Jun. 23, 2026
 * @author  ASAMI, Tomoharu
 */
case class DescriptiveAttributes(
  headline: Option[I18nBrief] = None,
  brief: Option[I18nBrief] = None,
  summary: Option[I18nSummary] = None,
  description: Option[I18nDescription] = None,
  lead: Option[I18nSummary] = None,
  `abstract`: Option[I18nSummary] = None,
  remarks: Option[I18nSummary] = None,
  tooltip: Option[I18nLabel] = None
) {
  def effectiveHeadline: Option[I18nString] =
    headline.map(_.toI18nString).
      orElse(brief.map(_.toI18nString)).
      orElse(tooltip.map(_.toI18nString))

  def effectiveBrief: Option[I18nString] =
    brief.map(_.toI18nString).
      orElse(summary.map(_.toI18nString)).
      orElse(lead.map(_.toI18nString)).
      orElse(`abstract`.map(_.toI18nString)).
      orElse(headline.map(_.toI18nString))

  def effectiveSummary: Option[I18nString] =
    summary.map(_.toI18nString).
      orElse(lead.map(_.toI18nString)).
      orElse(`abstract`.map(_.toI18nString)).
      orElse(description.map(_.toI18nString)).
      orElse(brief.map(_.toI18nString))

  def effectiveDescription: Option[I18nString] =
    description.map(_.toI18nString).
      orElse(`abstract`.map(_.toI18nString)).
      orElse(lead.map(_.toI18nString)).
      orElse(summary.map(_.toI18nString))

  def effectiveTooltip: Option[I18nString] =
    tooltip.map(_.toI18nString).
      orElse(brief.map(_.toI18nString)).
      orElse(headline.map(_.toI18nString)).
      orElse(summary.map(_.toI18nString)).
      orElse(`abstract`.map(_.toI18nString))

  def effectiveHeadlineString(locale: Locale): Option[String] =
    effectiveHeadline.map(_.displayMessage(locale))

  def effectiveBriefString(locale: Locale): Option[String] =
    effectiveBrief.map(_.displayMessage(locale))

  def effectiveSummaryString(locale: Locale): Option[String] =
    effectiveSummary.map(_.displayMessage(locale))

  def effectiveDescriptionString(locale: Locale): Option[String] =
    effectiveDescription.map(_.displayMessage(locale))

  def effectiveTooltipString(locale: Locale): Option[String] =
    effectiveTooltip.map(_.displayMessage(locale))
}

object DescriptiveAttributes {
  trait Holder {
    protected def descriptive_Attributes: DescriptiveAttributes

    def headline: Option[I18nBrief] = descriptive_Attributes.headline
    def brief: Option[I18nBrief] = descriptive_Attributes.brief
    def summary: Option[I18nSummary] = descriptive_Attributes.summary
    def description: Option[I18nDescription] = descriptive_Attributes.description
    def lead: Option[I18nSummary] = descriptive_Attributes.lead
    def `abstract`: Option[I18nSummary] = descriptive_Attributes.`abstract`
    def remarks: Option[I18nSummary] = descriptive_Attributes.remarks
    def tooltip: Option[I18nLabel] = descriptive_Attributes.tooltip
    def effectiveHeadline: Option[I18nString] = descriptive_Attributes.effectiveHeadline
    def effectiveBrief: Option[I18nString] = descriptive_Attributes.effectiveBrief
    def effectiveSummary: Option[I18nString] = descriptive_Attributes.effectiveSummary
    def effectiveDescription: Option[I18nString] = descriptive_Attributes.effectiveDescription
    def effectiveTooltip: Option[I18nString] = descriptive_Attributes.effectiveTooltip
  }

  trait BareHolder {
    protected def descriptive_Attributes: DescriptiveAttributes

    def headline: Option[I18nString] = descriptive_Attributes.headline.map(_.toI18nString)
    def brief: Option[I18nString] = descriptive_Attributes.brief.map(_.toI18nString)
    def summary: Option[I18nString] = descriptive_Attributes.summary.map(_.toI18nString)
    def description: Option[I18nString] = descriptive_Attributes.description.map(_.toI18nString)
    def lead: Option[I18nString] = descriptive_Attributes.lead.map(_.toI18nString)
    def `abstract`: Option[I18nString] = descriptive_Attributes.`abstract`.map(_.toI18nString)
    def remarks: Option[I18nString] = descriptive_Attributes.remarks.map(_.toI18nString)
    def tooltip: Option[I18nString] = descriptive_Attributes.tooltip.map(_.toI18nString)
    def effectiveHeadline: Option[I18nString] = descriptive_Attributes.effectiveHeadline
    def effectiveBrief: Option[I18nString] = descriptive_Attributes.effectiveBrief
    def effectiveSummary: Option[I18nString] = descriptive_Attributes.effectiveSummary
    def effectiveDescription: Option[I18nString] = descriptive_Attributes.effectiveDescription
    def effectiveTooltip: Option[I18nString] = descriptive_Attributes.effectiveTooltip
  }

  val empty: DescriptiveAttributes =
    DescriptiveAttributes(None, None, None, None, None, None, None, None)

  case class Builder(
    bsummary: Option[I18nSummary] = None,
    bdescription: Option[I18nDescription] = None
  ) {
    def build(): DescriptiveAttributes = DescriptiveAttributes(
      summary = bsummary,
      description = bdescription
    )

    def summary(p: String) = copy(bsummary = Some(I18nSummary(p)))
    def description(p: String) = copy(bdescription = Some(I18nDescription(p)))
  }
}
