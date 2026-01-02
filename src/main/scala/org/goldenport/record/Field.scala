package org.goldenport.record

/*
 * @since   Oct. 17, 2025
 * @version Jan.  2, 2026
 * @author  ASAMI, Tomoharu
 */
case class Field(key: String, value: Field.Value) {
}

object Field {
  trait Value {
    def single: Any
  }
  object Value {
    case class Single(single: Any) extends Value
  }
}
