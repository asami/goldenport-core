package org.goldenport.model.value

/*
 * @since   Dec. 24, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
case class BaseContent(
  nameAttributes: NameAttributes,
  descriptiveAttributes: DescriptiveAttributes
) {
  def isMatchName(p: String): Boolean = nameAttributes.isMatchName(p)
}

object BaseContent {
  trait Holder extends NameAttributes.Holder with DescriptiveAttributes.Holder {
    protected def baseContent: BaseContent

    protected def name_Attributes: NameAttributes = baseContent.nameAttributes
    protected def descriptive_Attributes: DescriptiveAttributes = baseContent.descriptiveAttributes

    def isMatchName(p: String): Boolean = baseContent.isMatchName(p)
  }

  trait BareHolder extends NameAttributes.BareHolder with DescriptiveAttributes.BareHolder {
    protected def baseContent: BaseContent

    protected def name_Attributes: NameAttributes = baseContent.nameAttributes
    protected def descriptive_Attributes: DescriptiveAttributes = baseContent.descriptiveAttributes

    def isMatchName(p: String): Boolean = baseContent.isMatchName(p)
  }

  def simple(
    name: String,
    descriptiveAttributes: DescriptiveAttributes = DescriptiveAttributes.empty
  ): BaseContent =
    BaseContent(NameAttributes.simple(name), descriptiveAttributes)
}
