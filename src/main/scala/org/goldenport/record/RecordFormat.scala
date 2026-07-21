package org.goldenport.record

import java.nio.file.Path

/*
 * @since   Apr.  8, 2026
 *  version May. 24, 2026
 * @version Jul. 21, 2026
 * @author  ASAMI, Tomoharu
 */
enum RecordFormat:
  case Json, Yaml, Xml, Hocon, Properties, Csv, Tsv, Ltsv, Lines, Tsl, Excel, Toml

object RecordFormat:
  def fromSuffix(name: String): Option[RecordFormat] =
    name.trim.toLowerCase match
      case s if s.endsWith(".json") => Some(Json)
      case s if s.endsWith(".yaml") => Some(Yaml)
      case s if s.endsWith(".yml") => Some(Yaml)
      case s if s.endsWith(".xml") => Some(Xml)
      case s if s.endsWith(".conf") => Some(Hocon)
      case s if s.endsWith(".hocon") => Some(Hocon)
      case s if s.endsWith(".toml") => Some(Toml)
      case s if s.endsWith(".props") => Some(Properties)
      case s if s.endsWith(".properties") => Some(Properties)
      case s if s.endsWith(".csv") => Some(Csv)
      case s if s.endsWith(".tsv") => Some(Tsv)
      case s if s.endsWith(".ltsv") => Some(Ltsv)
      case s if s.endsWith(".lines") => Some(Lines)
      case s if s.endsWith(".txt") => Some(Lines)
      case s if s.endsWith(".tsl") => Some(Tsl)
      case s if s.endsWith(".xlsx") => Some(Excel)
      case s if s.endsWith(".xls") => Some(Excel)
      case _ => None

  def fromPath(path: Path): Option[RecordFormat] =
    Option(path.getFileName).map(_.toString).flatMap(fromSuffix)
