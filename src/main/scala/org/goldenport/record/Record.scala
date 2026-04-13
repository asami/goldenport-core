package org.goldenport.record

import cats.kernel.Monoid
import org.goldenport.Consequence
import org.goldenport.text.Presentable
import org.goldenport.convert.ValueReader
import org.goldenport.datatype.PathName

/*
 * @since   Oct. 17, 2025
 *  version Nov.  7, 2025
 *  version Dec. 25, 2025
 *  version Jan. 10, 2026
 *  version Feb. 28, 2026
 *  version Mar. 31, 2026
 *  version Apr.  8, 2026
 * @version Apr. 14, 2026
 * @author  ASAMI, Tomoharu
 */
case class Record(fields: Vector[Field] = Vector.empty) extends Presentable {
  def isEmpty: Boolean = fields.isEmpty

  def print: String = fields.map(_.print).mkString(",")

  def getAs[T](key: String)(using reader: ValueReader[T]): Option[T] =
    fields.find(_.key == key).flatMap { field =>
      field.value match {
        case Field.Value.Single(v) => reader.read(v)
      }
    }

  def getAsC[T](key: String)(using reader: ValueReader[T]): Consequence[Option[T]] =
    fields.find(_.key == key) match {
      case None => Consequence.none
      case Some(field) => field.value match {
        case Field.Value.Single(v) => reader.readC(v).map(Some.apply)
      }
    }

  def as[T](key: String)(using reader: ValueReader[T]): T =
    asC(key).RAISE

  def asC[T](key: String)(using reader: ValueReader[T]): Consequence[T] =
    fields.find(_.key == key) match {
      case None => Consequence.recordNotFound(key, this)
      case Some(field) => field.value match {
        case Field.Value.Single(v) => reader.readC(v)
      }
    }

  def getAny(key: String): Option[Any] =
    fields.find(_.key == key).map {
      case field => field.value.single
    }

  def getFirst(keys: Seq[String]): Option[Any] =
    keys.iterator.map(getAny).collectFirst { case Some(value) => value }

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
    getAny(key).map(_to_string)

  def getByte(key: String): Option[Byte] =
    getAs[Byte](key)

  def getShort(key: String): Option[Short] =
    getAs[Short](key)

  def getInt(key: String): Option[Int] =
    getAs[Int](key)

  def getLong(key: String): Option[Long] =
    getAs[Long](key)

  def getInteger(key: String): Option[BigInt] =
    getAs[BigInt](key)

  def getBigInt(key: String): Option[BigInt] =
    getInteger(key)

  def getDecimal(key: String): Option[BigDecimal] =
    getAs[BigDecimal](key)

  def getBigDecimal(key: String): Option[BigDecimal] =
    getDecimal(key)

  def getFloat(key: String): Option[Float] =
    getAs[Float](key)

  def getDouble(key: String): Option[Double] =
    getAs[Double](key)

  def getBoolean(key: String): Option[Boolean] =
    getAs[Boolean](key)

  def getRecord(key: String): Option[Record] =
    getAny(key).collect {
      case r: Record => r
    }

  def getVector(key: String): Option[Vector[Any]] =
    getAny(key).collect {
      case xs: Vector[?] => xs.asInstanceOf[Vector[Any]]
      case xs: Seq[?] => xs.toVector.asInstanceOf[Vector[Any]]
      case xs: Array[?] => xs.toVector.asInstanceOf[Vector[Any]]
    }

  def getStringVector(key: String): Option[Vector[String]] =
    getVector(key).map(_.map(_to_string))

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

  def toJsonStringC: Consequence[String] = io.RecordEncoder.jsonC(this)

  def toJsonString: String = io.RecordEncoder.json(this)

  def toYamlStringC: Consequence[String] = io.RecordEncoder.yamlC(this)

  def toYamlString: String = io.RecordEncoder.yaml(this)
}

object Record {
  val empty: Record = Record()

  given Monoid[Record] with
    def empty: Record = Record.empty
    def combine(x: Record, y: Record): Record = x ++ y

  def create(xs: Seq[(String, Any)]): Record = {
    val a = xs.flatMap {
      case (k, v) => v match {
        case m: Record if m.isEmpty => None
        case _ => Some(Field(k, Field.Value.Single(v)))
      }
    }.toVector
    Record(a.toVector)
  }

  def createFull(xs: Seq[(String, Any)]): Record =
    Record(xs.map { case (k, v) => Field(k, Field.Value.Single(v)) }.toVector)

  def create(p: Map[String, Any]): Record = Record.create(p.toVector)

  def data(xs: (String, Any)*): Record = create(xs)

  def dataOption(xs: (String, Option[Any])*): Record = {
    val a = xs.flatMap {
      case (k, Some(v)) => Some(k -> v)
      case (k, None) => None
    }
    create(a)
  }

  def dataAuto(xs: (String, Any)*): Record = {
    val a = xs.flatMap {
      case (k, v) => v match {
        case Some(s) => Some(k -> s)
        case None => None
        case m: Seq[_] if m.isEmpty => None
        case m: Set[_] if xs.isEmpty => None
        case m: Record if m.isEmpty => None
        case m => Some(k -> m)
      }
    }
    create(a)
  }
}
