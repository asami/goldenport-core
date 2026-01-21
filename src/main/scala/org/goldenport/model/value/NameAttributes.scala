package org.goldenport.model.value

import cats.data.NonEmptyVector
import org.goldenport.datatype.Name
import org.goldenport.datatype.Identifier
import org.goldenport.datatype.Slug
import org.goldenport.datatype.I18nLabel
import org.goldenport.datatype.I18nTitle
import org.goldenport.datatype.I18nString

/*
 * @since   Aug.  1, 2025
 *  version Aug.  2, 2025
 *  version Dec. 30, 2025
 * @version Jan. 21, 2026
 * @author  ASAMI, Tomoharu
 */
case class NameAttributes(
  name: Name,
  label: Option[I18nLabel],
  title: Option[I18nTitle],
  code: Option[Identifier],
  alias: Option[NonEmptyVector[I18nLabel]],
  slug: Option[Slug],
  shortid: Option[Identifier]
) {
  def withName(p: Name) = copy(name = p)
  def withLabel(p: I18nLabel) = copy(label = Some(p))

  def isMatchName(p: String): Boolean = name.isMatch(p)
}

object NameAttributes {
  trait Holder {
    protected def name_Attributes: NameAttributes

    def name: Name = name_Attributes.name
    def label: Option[I18nLabel] = name_Attributes.label
  }

  trait BareHolder {
    protected def name_Attributes: NameAttributes

    def name: String = name_Attributes.name.value
    def label: Option[I18nString] = name_Attributes.label.map(_.toI18nString)
  }

  def simple(name: String): NameAttributes =
    simple(Name(name))

  def simple(name: Name): NameAttributes =
    NameAttributes(
      name = name,
      label = None,
      title = None,
      code = None,
      alias = None,
      slug = None,
      shortid = None
    )

  case class Builder(
    bname: Option[Name] = None,
    blabel: Option[I18nLabel] = None,
    btitle: Option[I18nTitle] = None,
    bcode: Option[Identifier] = None,
    balias: Option[NonEmptyVector[I18nLabel]] = None,
    bslug: Option[Slug] = None,
    bshortid: Option[Identifier] = None
  ) {
    def build(
    ): NameAttributes = NameAttributes(
      bname.get,
      blabel,
      btitle,
      bcode,
      balias,
      bslug,
      bshortid,
    )

    def name(p: String) = copy(bname = Some(Name(p)))
    def label(p: String) = copy(blabel = Some(I18nLabel(p)))
    def title(p: String) = copy(btitle = Some(I18nTitle(p)))
  }
  object Builder {
    def apply(name: String): Builder = Builder(Some(Name(name)))
  }
}
