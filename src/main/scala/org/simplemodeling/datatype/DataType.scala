package org.goldenport.datatype

import scala.util.*
import java.util.Currency
import cats.data.NonEmptyList

/*
 * @since   Apr. 11, 2025
 *  version Apr. 14, 2025
 *  version Jul. 20, 2025
 * @version Sep. 30, 2025
 * @author  ASAMI, Tomoharu
 */
abstract class DataType() {
}

trait NameString {
  def name: String
}

abstract class Quantity() {
  def quantity: Long
  def +(rhs: Quantity): Quantity
}
object Quantity {
  trait NonNegative extends Quantity {
    require (quantity >= 0)
  }
  object NonNegative {
    def apply(quantity: Long): NonNegative = NonNegativeQuantity(quantity)
  }
  trait Positive extends NonNegative {
    require (quantity > 0)
  }
  object Positive {
    def apply(quantity: Long): Positive = PositiveQuantity(quantity)
  }

  def apply(quantity: Long): Quantity = NonNegativeQuantity(quantity)
}

case class NonNegativeQuantity(quantity: Long) extends Quantity.NonNegative {
  def +(rhs: Quantity): NonNegativeQuantity = NonNegativeQuantity(quantity + rhs.quantity)
}

case class PositiveQuantity(quantity: Long) extends Quantity.Positive {
  def +(rhs: Quantity): PositiveQuantity = PositiveQuantity(quantity + rhs.quantity)
}

abstract class Amount() {
  def amount: BigDecimal
  def +(rhs: Amount): Amount
}
object Amount {
  trait NonNegative extends Amount {
    require (amount >= 0)
  }
  object NonNegative {
    def apply(amount: BigDecimal): NonNegative = NonNegativeAmount(amount)
  }
  trait Positive extends NonNegative {
    require (amount > 0)
  }
  object Positive {
    def apply(amount: BigDecimal): Positive = PositiveAmount(amount)
  }
}

case class NonNegativeAmount(amount: BigDecimal) extends Amount.NonNegative {
  def +(rhs: Amount): NonNegativeAmount = NonNegativeAmount(amount + rhs.amount)
}

case class PositiveAmount(amount: BigDecimal) extends Amount.Positive {
  def +(rhs: Amount): PositiveAmount = PositiveAmount(amount + rhs.amount)
}

// sealed trait Price {
//   def total: money.Money
// }
// object Price {
//   case class Gross(base: money.Money, tax: Money) extends Price {
//     def total = base + tax
//   }
//   case class Net(total: Money) extends Price {
//   }
// }

// trait EntityId {
//   def string: String
// }

// trait NameString {
//   def name: String
// }

// case class Money() {
// }

// case class NonNegativeQuantity(quantity: Int) {
//   def +(rhs: NonNegativeQuantity) = NonNegativeQuantity(quantity + rhs.quantity)
// }

// case class PositiveQuantity(quantity: Int) {
//   def +(rhs: PositiveQuantity) = PositiveQuantity(quantity + rhs.quantity)
// }
