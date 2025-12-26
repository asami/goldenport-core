package org.goldenport.observation

import org.goldenport.util.{SmEnum, SmEnumClass}

/*
 * @since   Jul. 23, 2025
 * @version Dec. 24, 2025
 * @author  ASAMI, Tomoharu
 */
enum Subject extends Enum[Subject], SmEnum {
  case User
  case System
  case Subsystem
}

object Subject extends SmEnumClass[Subject] {
  protected def enum_Values = values
}
