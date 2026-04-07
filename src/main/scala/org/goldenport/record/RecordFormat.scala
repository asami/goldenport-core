package org.goldenport.record

import java.nio.file.Path

/*
 * @since   Apr.  8, 2026
 * @version Apr.  8, 2026
 * @author  ASAMI, Tomoharu
 */
enum RecordFormat:
  case Json, Yaml, Xml, Hocon

object RecordFormat:
  def fromSuffix(name: String): Option[RecordFormat] =
    name.trim.toLowerCase match
      case s if s.endsWith(".json") => Some(Json)
      case s if s.endsWith(".yaml") => Some(Yaml)
      case s if s.endsWith(".yml") => Some(Yaml)
      case s if s.endsWith(".xml") => Some(Xml)
      case s if s.endsWith(".conf") => Some(Hocon)
      case s if s.endsWith(".hocon") => Some(Hocon)
      case _ => None

  def fromPath(path: Path): Option[RecordFormat] =
    Option(path.getFileName).map(_.toString).flatMap(fromSuffix)
