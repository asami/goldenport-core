package org.simplemodeling.model.value

import org.simplemodeling.datatype.I18nLabel
import org.simplemodeling.datatype.I18nBrief
import org.simplemodeling.datatype.I18nSummary
import org.simplemodeling.datatype.I18nDescription
import org.simplemodeling.datatype.I18nText

/*
 * @since   Aug.  1, 2025
 *  version Aug.  2, 2025
 * @version Oct.  7, 2025
 * @author  ASAMI, Tomoharu
 */
case class DescriptiveAttributes(
  headline: Option[I18nBrief],
  brief: Option[I18nBrief],
  summary: Option[I18nSummary],
  description: Option[I18nDescription],
  lead: Option[I18nSummary],
  content: Option[I18nText],
  `abstract`: Option[I18nSummary],
  remarks: Option[I18nSummary],
  tooltip: Option[I18nLabel]
)

object DescriptiveAttributes {
  trait Holder {
    protected def descriptive_Attributes: DescriptiveAttributes
  }
}
