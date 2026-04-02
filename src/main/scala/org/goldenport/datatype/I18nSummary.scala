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
case class I18nSummary(value: I18nString = I18nString("")) extends StringCodexable {
  def toI18nString: I18nString = value
  def encode(using ctx: ExecutionContext): String = summon[StringCodex[I18nSummary]].encode(this)
}

object I18nSummary {
  def apply(p: String): I18nSummary = I18nSummary(I18nString(p))
  given StringCodex[I18nSummary] with
    def encode(value: I18nSummary)(using ExecutionContext): String = value.value.encode
    def decode(value: String)(using ExecutionContext): Consequence[I18nSummary] =
      I18nString.decode(value).map(I18nSummary(_))
  def decode(p: String)(using ctx: ExecutionContext): Consequence[I18nSummary] =
    summon[StringCodex[I18nSummary]].decode(p)
}
