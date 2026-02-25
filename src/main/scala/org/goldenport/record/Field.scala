package org.goldenport.record

import org.goldenport.text.Presentable

/*
 * @since   Oct. 17, 2025
 *  version Jan.  2, 2026
 * @version Feb. 23, 2026
 * @author  ASAMI, Tomoharu
 */
case class Field(key: String, value: Field.Value) extends Presentable {
  def print: String = s"$key=${value.print}"
}

object Field {
  trait Value extends Presentable {
    def single: Any
  }
  object Value {
    case class Single(single: Any) extends Value {
      def print: String = Presentable.print(single)
    }
  }

  def create(key: String, value: Any): Field = {
    ???
  }

  def create(p: (String, Any)): Field = create(p._1, p._2)

  def creates(ps: Seq[(String, Any)]): Vector[Field] =
    ps.toVector.map(create)
}
