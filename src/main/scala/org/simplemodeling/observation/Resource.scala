package org.simplemodeling.observation

import org.simplemodeling.datatype

/*
 * @since   Jul. 23, 2025
 * @version Jul. 23, 2025
 * @author  ASAMI, Tomoharu
 */
enum Resource {
  case Unknown
  case File(file: java.io.File)
  case Url(url: java.net.URL)
  case Miscellaneous(name: Resource.Name, value: Resource.Value)
}

object Resource {
  case class Name(value: String) extends datatype.Name
  case class Value(value: String) extends datatype.Text
}
