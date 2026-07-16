package org.goldenport.datatype

import io.circe.Codec
import org.goldenport.Consequence
import org.goldenport.context.ExecutionContext
import org.goldenport.convert.{StringCodex, StringCodexable, ValueReader}

/*
 * @since   Aug.  2, 2025
 *  version Dec. 30, 2025
 *  version Apr. 17, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
case class I18nText(value: I18nString = I18nString("")) extends StringCodexable {
  def toI18nString: I18nString = value
  def displayMessage(locale: java.util.Locale): String = value.displayMessage(locale)
  def encode(using ctx: ExecutionContext): String = summon[StringCodex[I18nText]].encode(this)
}

object I18nText {
  def apply(p: String): I18nText = I18nText(I18nString(p))
  given Codec[I18nText] = I18nString.semanticCodec(I18nText(_), _.value)
  given ValueReader[I18nText] with
    def readC(value: Any): Consequence[I18nText] = value match {
      case p: I18nText => Consequence.success(p)
      case _ => I18nString.readC(value).map(I18nText(_))
    }
    override def readContextC(value: Any)(using ExecutionContext): Consequence[I18nText] = value match {
      case p: I18nText => I18nString.readContextC(p.value).map(I18nText(_))
      case _ => I18nString.readContextC(value).map(I18nText(_))
    }
  given StringCodex[I18nText] with
    def encode(value: I18nText)(using ExecutionContext): String = value.value.encode
    def decode(value: String)(using ExecutionContext): Consequence[I18nText] =
      I18nString.decode(value).map(I18nText(_))
  def decode(p: String)(using ctx: ExecutionContext): Consequence[I18nText] =
    summon[StringCodex[I18nText]].decode(p)
}
