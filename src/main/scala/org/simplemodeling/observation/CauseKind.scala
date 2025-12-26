package org.simplemodeling.observation

import org.simplemodeling.util.{SmEnum, SmEnumClass}

/*
 * @since   Jul. 19, 2025
 *  version Jul. 23, 2025
 *  version Aug.  1, 2025
 * @version Dec. 24, 2025
 * @author  ASAMI, Tomoharu
 */
enum CauseKind(val code: Int) extends Enum[CauseKind], SmEnum {
  case Defect  extends CauseKind(1)
  case Fault   extends CauseKind(2)
  case Anomaly extends CauseKind(3)
}

object CauseKind extends SmEnumClass[CauseKind] {
  protected def enum_Values = values
}
