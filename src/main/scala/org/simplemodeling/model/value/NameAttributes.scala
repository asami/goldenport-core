package org.simplemodeling.model.value

import cats.data.NonEmptyVector
import org.simplemodeling.datatype.Name
import org.simplemodeling.datatype.Identifier
import org.simplemodeling.datatype.Slug
import org.simplemodeling.datatype.I18nLabel
import org.simplemodeling.datatype.I18nTitle

/*
 * @since   Aug.  1, 2025
 *  version Aug.  2, 2025
 * @version Dec. 24, 2025
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
}
