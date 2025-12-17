package org.simplemodeling.record

import org.simplemodeling.convert.ValueReader

/*
 * @since   Oct. 17, 2025
 * @version Nov.  7, 2025
 * @author  ASAMI, Tomoharu
 */
case class Record(fields: Vector[Field]) {
  lazy val asMap: Map[String, Any] = fields.map(x => (x.key, x.value.single)).toMap

  def getAs[T](key: String)(using reader: ValueReader[T]): Option[T] =
    fields.find(_.key == key).flatMap(reader.read)

  def as[T](using reader: ValueReader[T]): Option[T] =
    reader.read(asMap)
}

object Record {
  def data(xs: (String, Any)*): Record = ???
}
