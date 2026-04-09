package org.goldenport.protocol.spec

import org.goldenport.schema.DataType

/*
 * @since   Oct.  6, 2018
 *  version Oct.  8, 2018
 *  version Feb. 24, 2019
 *  version Mar.  2, 2025
 *  version Dec. 25, 2025
 *  version Jan. 14, 2026
 * @version Apr. 10, 2026
 * @author  ASAMI, Tomoharu
 */
case class ResponseDefinition(
  result: List[DataType] = Nil,
  isVoid: Boolean = false
) {
  def isSpecified: Boolean = isVoid || result.nonEmpty
}

object ResponseDefinition {
  val void = ResponseDefinition(isVoid = true)
  val script = void // TODO
}
