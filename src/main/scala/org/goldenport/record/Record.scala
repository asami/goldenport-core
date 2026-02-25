package org.goldenport.record

import org.goldenport.Consequence
import org.goldenport.text.Presentable
import org.goldenport.convert.ValueReader
import org.goldenport.datatype.PathName

/*
 * @since   Oct. 17, 2025
 *  version Nov.  7, 2025
 *  version Dec. 25, 2025
 *  version Jan. 10, 2026
 * @version Feb. 23, 2026
 * @author  ASAMI, Tomoharu
 */
case class Record(fields: Vector[Field] = Vector.empty) extends Presentable {
  def print: String = fields.map(_.print).mkString(",")

  def getAs[T](key: String)(using reader: ValueReader[T]): Option[T] =
    fields.find(_.key == key).flatMap { field =>
      field.value match {
        case Field.Value.Single(v) => reader.read(v)
      }
    }

  def getAsC[T](key: String)(using reader: ValueReader[T]): Consequence[T] =
    fields.find(_.key == key) match {
      case None => Consequence.failRecordNotFound(key, this)
      case Some(field) => field.value match {
        case Field.Value.Single(v) => reader.readC(v)
      }
    }

  def as[T](key: String)(using reader: ValueReader[T]): T =
    asC(key).RAISE

  def asC[T](key: String)(using reader: ValueReader[T]): Consequence[T] =
    Consequence.successOrRecordNotFound(this, key)

  def keySet: Set[String] = asMap.keySet

  lazy val asMap: Map[String, Any] = fields.map(x => (x.key, x.value.single)).toMap

  def asNameStringVector: Vector[(String, String)] =
    fields.map(x => x.key -> x.value.single.toString)

  def ++(rhs: Record): Record = copy(fields = fields ++ rhs.fields)

  def update(p: (String, Any), ps: (String, Any)*): Record = update(Field.creates(p +: ps))

  def update(rec: Record): Record = ???

  def update(ps: Seq[Field]): Record = ???

  def complement(p: (String, Any), ps: (String, Any)*): Record = complement(Field.creates(p +: ps))

  def complement(rec: Record): Record = ???

  def complement(ps: Seq[Field]): Record = ???

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

  def getString(path: PathName): Option[String] =
    _value_for_segments(path.segments).map(_to_string)

  private def _value_for_segments(segments: Vector[String]): Option[Any] =
    segments match {
      case Vector() => None
      case _ =>
        val pathKey = segments.mkString("/")
        asMap.get(pathKey) match {
          case Some(value) => Some(value)
          case None =>
            fields.find(_.key == segments.head).flatMap { field =>
              val value = field.value match {
                case Field.Value.Single(v) => v
              }
              if (segments.tail.isEmpty)
                Some(value)
              else
                value match {
                  case r: Record => r._value_for_segments(segments.tail)
                  case _ => None
                }
            }
        }
    }

  private def _to_string(p: Any): String = p.toString // TODO

  private def _record_as[T](reader: ValueReader[T]): Option[T] =
    reader.read(asMap)
}

object Record {
  val empty: Record = Record()

  def create(xs: Seq[(String, Any)]): Record =
    Record(xs.map { case (k, v) => Field(k, Field.Value.Single(v)) }.toVector)

  def data(xs: (String, Any)*): Record = create(xs)
}
