package org.goldenport.record

import org.goldenport.convert.ValueReader

/*
 * @since   Oct. 17, 2025
 *  version Nov.  7, 2025
 *  version Dec. 25, 2025
 * @version Jan. 10, 2026
 * @author  ASAMI, Tomoharu
 */
case class Record(fields: Vector[Field] = Vector.empty) {
  lazy val asMap: Map[String, Any] = fields.map(x => (x.key, x.value.single)).toMap
  def asNameStringVector: Vector[(String, String)] =
    fields.map(x => x.key -> x.value.single.toString)

  def getAs[T](key: String)(using reader: ValueReader[T]): Option[T] =
    fields.find(_.key == key).flatMap(reader.read)

  def as[T](using reader: ValueReader[T]): Option[T] =
    reader.read(asMap)

  def keySet: Set[String] = asMap.keySet

  def ++(rhs: Record): Record = copy(fields = fields ++ rhs.fields)

  /**
   * Returns a new Record containing only fields whose keys are in the given set.
   * This is a convenience helper for projection use-cases.
   */
  def filterKeys(keyset: Set[String]): Record =
    Record(fields.filter(f => keyset.contains(f.key)))

  /**
   * Returns the value of the field as String if present.
   * Intended for safe sorting / display purposes.
   */
  def getString(key: String): Option[String] =
    asMap.get(key).map(_to_string)

  private def _to_string(p: Any): String = p.toString // TODO
}

object Record {
  val empty: Record = Record()

  def create(xs: Seq[(String, Any)]): Record =
    Record(xs.map { case (k, v) => Field(k, Field.Value.Single(v)) }.toVector)

  def data(xs: (String, Any)*): Record = create(xs)
}
