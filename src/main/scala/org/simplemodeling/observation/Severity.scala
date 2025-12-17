package org.simplemodeling.observation

import org.simplemodeling.util.{SmEnum, SmEnumClass}

/*
 * @since   Jul. 23, 2025
 * @version Jul. 23, 2025
 * @author  ASAMI, Tomoharu
 */
enum Severity extends Enum[Severity], SmEnum {
  case Fatal, Error, Warn, Info, Debug, Trace
}

object Severity extends SmEnumClass[Severity] {
}
