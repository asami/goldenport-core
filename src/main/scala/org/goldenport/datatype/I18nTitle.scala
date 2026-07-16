package org.goldenport.datatype

import io.circe.Codec
import org.goldenport.Consequence
import org.goldenport.context.ExecutionContext
import org.goldenport.convert.{StringCodex, StringCodexable, ValueReader}

/*
 * @since   Jul. 23, 2025
 *  version Dec. 30, 2025
 *  version Jan. 21, 2026
 *  version Apr. 17, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
case class I18nTitle(value: I18nString = I18nString("")) extends StringCodexable {
  def toI18nString: I18nString = value
  def displayMessage(locale: java.util.Locale): String = value.displayMessage(locale)
  def encode(using ctx: ExecutionContext): String = summon[StringCodex[I18nTitle]].encode(this)
}

object I18nTitle {
  def apply(p: String): I18nTitle = I18nTitle(I18nString(p))
  given Codec[I18nTitle] = I18nString.semanticCodec(I18nTitle(_), _.value)
  given ValueReader[I18nTitle] with
    def readC(value: Any): Consequence[I18nTitle] = value match {
      case p: I18nTitle => Consequence.success(p)
      case _ => I18nString.readC(value).map(I18nTitle(_))
    }
    override def readContextC(value: Any)(using ExecutionContext): Consequence[I18nTitle] = value match {
      case p: I18nTitle => I18nString.readContextC(p.value).map(I18nTitle(_))
      case _ => I18nString.readContextC(value).map(I18nTitle(_))
    }
  given StringCodex[I18nTitle] with
    def encode(value: I18nTitle)(using ExecutionContext): String = value.value.encode
    def decode(value: String)(using ExecutionContext): Consequence[I18nTitle] =
      I18nString.decode(value).map(I18nTitle(_))
  def decode(p: String)(using ctx: ExecutionContext): Consequence[I18nTitle] =
    summon[StringCodex[I18nTitle]].decode(p)
}
