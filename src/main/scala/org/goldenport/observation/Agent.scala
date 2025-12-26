package org.goldenport.observation

import org.goldenport.datatype

/*
 * @since   Jul. 23, 2025
 * @version Jul. 23, 2025
 * @author  ASAMI, Tomoharu
 */
enum Agent {
  case System
  case Component(name: Agent.Name)
}

object Agent {
  case class Name(value: String) extends datatype.Name
}
