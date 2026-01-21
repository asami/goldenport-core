package org.goldenport.model.value

/*
 * @since   Dec. 24, 2025
 *  version Dec. 30, 2025
 * @version Jan. 21, 2026
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

  case class Builder(
    names: NameAttributes.Builder = NameAttributes.Builder(),
    descs: DescriptiveAttributes.Builder = DescriptiveAttributes.Builder()
  ) {
    def build(): BaseContent = BaseContent(
      names.build(),
      descs.build()
    )

    def name(p: String) = copy(names = names.name(p))
    def label(p: String) = copy(names = names.label(p))
    def title(p: String) = copy(names = names.title(p))
    def summary(p: String) = copy(descs = descs.summary(p))
    def description(p: String) = copy(descs = descs.description(p))
  }
  object Builder {
    def apply(name: String): Builder = Builder(names = NameAttributes.Builder(name))
  }
}
