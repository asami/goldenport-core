package org.goldenport.observation

import org.goldenport.util.{SmEnum, SmEnumClass}

/*
 * @since   Jul. 23, 2025
 *  version Aug.  1, 2025
 * @version Dec. 24, 2025
 * @author  ASAMI, Tomoharu
 */
enum Strategy(val code: Int) extends Enum[Strategy], SmEnum {
  case Manual extends Strategy(1)
  case Recoverable extends Strategy(2)
  case Retriable extends Strategy(3)
  case Escalatable extends Strategy(4)
  case Alert extends Strategy(5)
  case Ignored extends Strategy(6)
}

object Strategy extends SmEnumClass[Strategy] {
  protected def enum_Values = values
}
