package org.goldenport.model.statemachine

import org.goldenport.util.SmEnumClass

/*
 * @since   Aug.  2, 2025
 * @version Mar. 18, 2026
 * @author  ASAMI, Tomoharu
 */
enum Aliveness(
  val dbValue: Int
) extends StateMachine {
  case Alive extends Aliveness(1)
  case Suspended extends Aliveness(2)
  case Dead extends Aliveness(3)

  def isOperational: Boolean =
    this == Aliveness.Alive
}

object Aliveness extends SmEnumClass[Aliveness] {
  protected def enum_Values = values

  val default: Aliveness = Alive

  private val _by_db_value = values.map(x => x.dbValue -> x).toMap

  def fromDbValue(dbvalue: Int): Option[Aliveness] =
    _by_db_value.get(dbvalue)
}
