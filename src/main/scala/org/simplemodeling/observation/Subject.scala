package org.simplemodeling.observation

import org.simplemodeling.util.{SmEnum, SmEnumClass}

/*
 * @since   Jul. 23, 2025
 * @version Jul. 23, 2025
 * @author  ASAMI, Tomoharu
 */
enum Subject extends Enum[Subject], SmEnum {
  case User
  case System
  case Subsystem
}

object Subject extends SmEnumClass[Subject] {
}
