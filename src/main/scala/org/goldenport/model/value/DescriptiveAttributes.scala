package org.goldenport.model.value

import org.goldenport.datatype.I18nLabel
import org.goldenport.datatype.I18nBrief
import org.goldenport.datatype.I18nSummary
import org.goldenport.datatype.I18nDescription
import org.goldenport.datatype.I18nText
import org.goldenport.datatype.I18nString

/*
 * @since   Aug.  1, 2025
 *  version Aug.  2, 2025
 *  version Oct.  7, 2025
 *  version Dec. 30, 2025
 * @version Jan. 21, 2026
 * @author  ASAMI, Tomoharu
 */
case class DescriptiveAttributes(
  headline: Option[I18nBrief] = None,
  brief: Option[I18nBrief] = None,
  summary: Option[I18nSummary] = None,
  description: Option[I18nDescription] = None,
  lead: Option[I18nSummary] = None,
  content: Option[I18nText] = None,
  `abstract`: Option[I18nSummary] = None,
  remarks: Option[I18nSummary] = None,
  tooltip: Option[I18nLabel] = None
)

object DescriptiveAttributes {
  trait Holder {
    protected def descriptive_Attributes: DescriptiveAttributes

    def headline: Option[I18nBrief] = descriptive_Attributes.headline
    def brief: Option[I18nBrief] = descriptive_Attributes.brief
    def summary: Option[I18nSummary] = descriptive_Attributes.summary
    def description: Option[I18nDescription] = descriptive_Attributes.description
    def lead: Option[I18nSummary] = descriptive_Attributes.lead
    def content: Option[I18nText] = descriptive_Attributes.content
    def `abstract`: Option[I18nSummary] = descriptive_Attributes.`abstract`
    def remarks: Option[I18nSummary] = descriptive_Attributes.remarks
    def tooltip: Option[I18nLabel] = descriptive_Attributes.tooltip
  }

  trait BareHolder {
    protected def descriptive_Attributes: DescriptiveAttributes

    def headline: Option[I18nString] = descriptive_Attributes.headline.map(_.toI18nString)
    def brief: Option[I18nString] = descriptive_Attributes.brief.map(_.toI18nString)
    def summary: Option[I18nString] = descriptive_Attributes.summary.map(_.toI18nString)
    def description: Option[I18nString] = descriptive_Attributes.description.map(_.toI18nString)
    def lead: Option[I18nString] = descriptive_Attributes.lead.map(_.toI18nString)
    def contentText: Option[I18nString] = descriptive_Attributes.content.map(_.toI18nString)
    def `abstract`: Option[I18nString] = descriptive_Attributes.`abstract`.map(_.toI18nString)
    def remarks: Option[I18nString] = descriptive_Attributes.remarks.map(_.toI18nString)
    def tooltip: Option[I18nString] = descriptive_Attributes.tooltip.map(_.toI18nString)
  }

  val empty: DescriptiveAttributes =
    DescriptiveAttributes(None, None, None, None, None, None, None, None, None)

  case class Builder(
    bsummary: Option[I18nSummary] = None,
    bdescription: Option[I18nDescription] = None,
  ) {
    def build(): DescriptiveAttributes = DescriptiveAttributes(
      summary = bsummary,
      description = bdescription
    )

    def summary(p: String) = copy(bsummary = Some(I18nSummary(p)))
    def description(p: String) = copy(bdescription = Some(I18nDescription(p)))
  }
}
