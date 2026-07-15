package org.goldenport.datatype

import io.circe.Codec
import org.goldenport.Consequence
import org.goldenport.context.ExecutionContext
import org.goldenport.convert.{StringCodex, StringCodexable, ValueReader}

/*
 * @since   Aug.  2, 2025
 *  version Dec. 30, 2025
 *  version Apr.  3, 2026
 * @version Jul. 15, 2026
 * @author  ASAMI, Tomoharu
 */
case class I18nSummary(value: I18nString = I18nString("")) extends StringCodexable {
  def toI18nString: I18nString = value
  def encode(using ctx: ExecutionContext): String = summon[StringCodex[I18nSummary]].encode(this)
}

object I18nSummary {
  def apply(p: String): I18nSummary = I18nSummary(I18nString(p))
  given Codec[I18nSummary] = I18nString.semanticCodec(I18nSummary(_), _.value)
  given ValueReader[I18nSummary] with
    def readC(value: Any): Consequence[I18nSummary] = value match {
      case p: I18nSummary => Consequence.success(p)
      case _ => I18nString.readC(value).map(I18nSummary(_))
    }
  given StringCodex[I18nSummary] with
    def encode(value: I18nSummary)(using ExecutionContext): String = value.value.encode
    def decode(value: String)(using ExecutionContext): Consequence[I18nSummary] =
      I18nString.decode(value).map(I18nSummary(_))
  def decode(p: String)(using ctx: ExecutionContext): Consequence[I18nSummary] =
    summon[StringCodex[I18nSummary]].decode(p)
}
