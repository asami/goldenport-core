package org.goldenport.record

/*
 * @since   Oct. 17, 2025
 * @version Oct. 17, 2025
 * @author  ASAMI, Tomoharu
 */
case class Field(key: String, value: Field.Value) {
}

object Field {
  trait Value {
    def single: Any
  }
}
