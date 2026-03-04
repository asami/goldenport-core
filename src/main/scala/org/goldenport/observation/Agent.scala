package org.goldenport.observation

import org.goldenport.record.Record
import org.goldenport.datatype.Name

/*
 * @since   Jul. 23, 2025
 * @version Mar.  4, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class Agent {
  def toRecord: Record
}

object Agent {
  case object System extends Agent {
    def toRecord: Record = Record.data(
      "kind" -> "system"
    )
  }
  case class Component(name: Name) extends Agent {
    def toRecord: Record = Record.data(
      "kind" -> "component",
      "name" -> name.value
    )
  }
}
