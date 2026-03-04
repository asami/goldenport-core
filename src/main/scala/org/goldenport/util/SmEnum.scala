package org.goldenport.util

import org.goldenport.text.Presentable
import StringUtils.camelToSnake

/*
 * @since   Jul. 19, 2025
 *  version Dec. 24, 2025
 * @version Mar.  3, 2026
 * @author  ASAMI, Tomoharu
 */
trait SmEnum extends Product with Presentable {
  def value: String = camelToSnake(productPrefix)

  def print = value
  override def display = s"Enum(${productPrefix})"
  override def show = s"Enum(${productPrefix}):$value"
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

trait SmEnumClass[E <: Product] {
  protected def enum_Values: Array[E]

  protected def normalize_value(s: String): String =
    s.trim.toLowerCase

  protected def keyOf(e: E): String =
    e match
      case m: SmEnum => normalize_value(m.value)
      case _         => normalize_value(e.productPrefix)

  def from(str: String): Option[E] = {
    val key = normalize_value(str)
    enum_Values.find(e => keyOf(e) == key)
  }

  def get(str: String): E =
    from(str).getOrElse {
      throw new IllegalArgumentException(s"Unknown enum value: $str")
    }
}
