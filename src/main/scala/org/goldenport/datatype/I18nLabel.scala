package org.goldenport.datatype

import org.goldenport.Consequence
import org.goldenport.context.ExecutionContext
import org.goldenport.convert.{StringCodex, StringCodexable}

/*
 * @since   Jul. 23, 2025
 *  version Dec. 30, 2025
 * @version Apr.  3, 2026
 * @author  ASAMI, Tomoharu
 */
case class I18nLabel(value: I18nString = I18nString("")) extends StringCodexable {
  def toI18nString: I18nString = value
  def encode(using ctx: ExecutionContext): String = summon[StringCodex[I18nLabel]].encode(this)
}

object I18nLabel {
  def apply(p: String): I18nLabel = I18nLabel(I18nString(p))
  given StringCodex[I18nLabel] with
    def encode(value: I18nLabel)(using ExecutionContext): String = value.value.encode
    def decode(value: String)(using ExecutionContext): Consequence[I18nLabel] =
      I18nString.decode(value).map(I18nLabel(_))
  def decode(p: String)(using ctx: ExecutionContext): Consequence[I18nLabel] =
    summon[StringCodex[I18nLabel]].decode(p)
}
