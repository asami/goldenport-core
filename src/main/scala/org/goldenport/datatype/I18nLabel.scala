package org.goldenport.datatype

import io.circe.Codec
import org.goldenport.Consequence
import org.goldenport.context.ExecutionContext
import org.goldenport.convert.{StringCodex, StringCodexable, ValueReader}

/*
 * @since   Jul. 23, 2025
 *  version Dec. 30, 2025
 *  version Apr.  3, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
case class I18nLabel(value: I18nString = I18nString("")) extends StringCodexable {
  def toI18nString: I18nString = value
  def encode(using ctx: ExecutionContext): String = summon[StringCodex[I18nLabel]].encode(this)
}

object I18nLabel {
  def apply(p: String): I18nLabel = I18nLabel(I18nString(p))
  given Codec[I18nLabel] = I18nString.semanticCodec(I18nLabel(_), _.value)
  given ValueReader[I18nLabel] with
    def readC(value: Any): Consequence[I18nLabel] = value match {
      case p: I18nLabel => Consequence.success(p)
      case _ => I18nString.readC(value).map(I18nLabel(_))
    }
    override def readContextC(value: Any)(using ExecutionContext): Consequence[I18nLabel] = value match {
      case p: I18nLabel => I18nString.readContextC(p.value).map(I18nLabel(_))
      case _ => I18nString.readContextC(value).map(I18nLabel(_))
    }
  given StringCodex[I18nLabel] with
    def encode(value: I18nLabel)(using ExecutionContext): String = value.value.encode
    def decode(value: String)(using ExecutionContext): Consequence[I18nLabel] =
      I18nString.decode(value).map(I18nLabel(_))
  def decode(p: String)(using ctx: ExecutionContext): Consequence[I18nLabel] =
    summon[StringCodex[I18nLabel]].decode(p)
}
