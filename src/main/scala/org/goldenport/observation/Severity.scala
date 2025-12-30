package org.goldenport.observation

import org.goldenport.util.{SmEnum, SmEnumClass}

/*
 * @since   Jul. 23, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
enum Severity extends Enum[Severity], SmEnum {
  case Fatal, Error, Warn, Info, Debug, Trace
}

object Severity extends SmEnumClass[Severity] {
  protected def enum_Values = values

  def max(a: Severity, b: Severity): Severity =
    if (a.ordinal <= b.ordinal) a else b
}
