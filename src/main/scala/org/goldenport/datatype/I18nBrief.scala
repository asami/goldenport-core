package org.goldenport.datatype

import org.goldenport.Consequence
import org.goldenport.context.ExecutionContext
import org.goldenport.convert.{StringCodex, StringCodexable}

/*
 * @since   Aug.  2, 2025
 *  version Dec. 30, 2025
 * @version Apr.  3, 2026
 * @author  ASAMI, Tomoharu
 */
case class I18nBrief(value: I18nString = I18nString("")) extends StringCodexable {
  def toI18nString: I18nString = value
  def encode(using ctx: ExecutionContext): String = summon[StringCodex[I18nBrief]].encode(this)
}

object I18nBrief {
  def apply(p: String): I18nBrief = I18nBrief(I18nString(p))
  given StringCodex[I18nBrief] with
    def encode(value: I18nBrief)(using ExecutionContext): String = value.value.encode
    def decode(value: String)(using ExecutionContext): Consequence[I18nBrief] =
      I18nString.decode(value).map(I18nBrief(_))
  def decode(p: String)(using ctx: ExecutionContext): Consequence[I18nBrief] =
    summon[StringCodex[I18nBrief]].decode(p)
}
