package org.simplemodeling.util

import StringUtils.camelToSnake

/*
 * @since   Jul. 19, 2025
 * @version Dec. 24, 2025
 * @author  ASAMI, Tomoharu
 */
trait SmEnum { self: Product =>
  val label: String = camelToSnake(self.productPrefix)

  override def toString(): String = label
}

// trait SmEnumClass[E <: Enum[E] & Product]:

//   protected def enumValues: Seq[E]

//   protected def normalize(s: String): String =
//     s.trim.toLowerCase

//   protected def keyOf(e: E): String =
//     e match
//       case m: SmEnum => normalize(m.label)
//       case _         => normalize(e.productPrefix)

//   def from(str: String): Option[E] =
//     val key = normalize(str)
//     enumValues.find(e => keyOf(e) == key)

//   def get(str: String): E =
//     from(str).getOrElse {
//       throw new IllegalArgumentException(s"Unknown enum value: $str")
//     }

// trait SmEnumClass[E <: Enum[E] & Product] {
//   protected def enum_Values: Array[E]

//   def from(str: String): Option[E] = {
//     val normalized = str.toLowerCase
//     enum_Values.find(e => camelToSnake(e.productPrefix).equalsIgnoreCase(str))
//   }
// }

trait SmEnumClass[E] {
  protected def enum_Values: Array[E]

  def from(str: String): Option[E] = {
    enum_Values.find {
      case m: SmEnum => m.label.equalsIgnoreCase(str)
      case m: Product => m.productPrefix.equalsIgnoreCase(str)
      case m => m.toString.equalsIgnoreCase(str)
    }
  }
}
