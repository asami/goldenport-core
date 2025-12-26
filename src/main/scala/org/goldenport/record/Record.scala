package org.goldenport.record

import org.goldenport.convert.ValueReader

/*
 * @since   Oct. 17, 2025
 *  version Nov.  7, 2025
 * @version Dec. 25, 2025
 * @author  ASAMI, Tomoharu
 */
case class Record(fields: Vector[Field]) {
  lazy val asMap: Map[String, Any] = fields.map(x => (x.key, x.value.single)).toMap
  def asNameStringVector: Vector[(String, String)] =
    fields.map(x => x.key -> x.value.single.toString)

  def getAs[T](key: String)(using reader: ValueReader[T]): Option[T] =
    fields.find(_.key == key).flatMap(reader.read)

  def as[T](using reader: ValueReader[T]): Option[T] =
    reader.read(asMap)
}

object Record {
  private case class SimpleValue(value: String) extends Field.Value {
    def single: Any = value
  }

  val empty: Record = Record(Vector.empty)

  def create(xs: Seq[(String, String)]): Record =
    Record(xs.map { case (k, v) => Field(k, SimpleValue(v)) }.toVector)

  def data(xs: (String, Any)*): Record = ???
}
