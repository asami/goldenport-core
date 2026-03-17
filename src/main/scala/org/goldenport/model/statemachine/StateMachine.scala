package org.goldenport.model.statemachine

import org.goldenport.util.SmEnum

/*
 * @since   Aug.  2, 2025
 * @version Mar. 18, 2026
 * @author  ASAMI, Tomoharu
 */
trait StateMachine extends SmEnum {
  def dbValue: Int

  def stateName: String = value
}
