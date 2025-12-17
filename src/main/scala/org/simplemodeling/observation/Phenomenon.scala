package org.simplemodeling.observation

import org.simplemodeling.util.{SmEnum, SmEnumClass}

/*
 * @since   Jul. 19, 2025
 *  version Jul. 23, 2025
 * @version Aug.  1, 2025
 * @author  ASAMI, Tomoharu
 */
enum Phenomenon(val code: Int) extends Enum[Phenomenon], SmEnum {
  case Failure extends Phenomenon(1)
  case Rejection extends Phenomenon(2)
  case Incident extends Phenomenon(3)
  case Deviation extends Phenomenon(4)
}


object Phenomenon extends SmEnumClass[Phenomenon] {
}
