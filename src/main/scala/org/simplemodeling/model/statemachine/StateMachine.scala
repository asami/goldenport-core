package org.simplemodeling.model.statemachine

import org.goldenport.util.SmEnum

/*
 * @since   Aug.  2, 2025
 * @version Mar. 24, 2026
 * @author  ASAMI, Tomoharu
 */
trait StateMachine extends SmEnum {
  def dbValue: Int

  def stateName: String = value
}
