package org.goldenport.observation

import org.goldenport.text.Presentable
import org.goldenport.datatype.Name
import org.goldenport.record.Record

/*
 * @since   Jul. 23, 2025
 * @version Mar.  4, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class Resource extends Presentable {
  def toRecord: Record
}

object Resource {
  case object Unknown extends Resource {
    def toRecord: Record = Record.data("kind" -> "unkonwn")
    def print = "unkonwn"
  }
  case class File(file: java.io.File) extends Resource {
    def toRecord: Record = Record.data(
      "kind" -> "file",
      "file" -> Presentable.print(file)
    )
    def print = s"Resoruce[file](${Presentable.print(file)})"
  }
  case class Url(url: java.net.URL) extends Resource {
    def toRecord: Record = Record.data(
      "kind" -> "url",
      "url" -> Presentable.print(url)
    )
    def print = s"Resource[url](${Presentable.print(url)})"
  }
  case class Miscellaneous(name: Name, value: String) extends Resource {
    def toRecord: Record = Record.data(
      "kind" -> "miscellaneous",
      "url" -> value
    )
    def print = s"Miscellaneous[$name](${value})"
  }
}
