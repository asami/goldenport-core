package org.simplemodeling.observation

import org.simplemodeling.util.{SmEnum, SmEnumClass}

/*
 * @since   Jul. 23, 2025
 *  version Jul. 23, 2025
 * @version Aug.  1, 2025
 * @author  ASAMI, Tomoharu
 */
enum Handler(val code: Int) extends Enum[Handler], SmEnum {
  case EndUser extends Handler(1)
  case ApplicationAdmin extends Handler(2)
  case SystemAdmin extends Handler(3)
  case Developer extends Handler(4)
}

object Handler extends SmEnumClass[Handler] {
}
