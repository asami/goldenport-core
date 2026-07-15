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
case class I18nDescription(value: I18nString = I18nString("")) extends StringCodexable {
  def toI18nString: I18nString = value
  def encode(using ctx: ExecutionContext): String = summon[StringCodex[I18nDescription]].encode(this)
}

object I18nDescription {
  def apply(p: String): I18nDescription = I18nDescription(I18nString(p))
  given Codec[I18nDescription] = I18nString.semanticCodec(I18nDescription(_), _.value)
  given ValueReader[I18nDescription] with
    def readC(value: Any): Consequence[I18nDescription] = value match {
      case p: I18nDescription => Consequence.success(p)
      case _ => I18nString.readC(value).map(I18nDescription(_))
    }
  given StringCodex[I18nDescription] with
    def encode(value: I18nDescription)(using ExecutionContext): String = value.value.encode
    def decode(value: String)(using ExecutionContext): Consequence[I18nDescription] =
      I18nString.decode(value).map(I18nDescription(_))
  def decode(p: String)(using ctx: ExecutionContext): Consequence[I18nDescription] =
    summon[StringCodex[I18nDescription]].decode(p)
}
