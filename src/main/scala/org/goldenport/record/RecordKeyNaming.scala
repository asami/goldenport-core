package org.goldenport.record

import java.util.Locale
import org.goldenport.Consequence

/*
 * @since   May. 26, 2026
 * @version May. 26, 2026
 * @author  ASAMI, Tomoharu
 */
object RecordKeyNaming {
  def toCanonicalCamelName(name: String): String = {
    val trimmed = name.trim
    if (trimmed.isEmpty)
      trimmed
    else if (!_has_separator(trimmed))
      trimmed
    else {
      val parts = trimmed
        .split("[_\\-\\.\\s]+")
        .toVector
        .filter(_.nonEmpty)
        .map(_.toLowerCase(Locale.ROOT))
      parts.headOption match {
        case None => ""
        case Some(head) =>
          head + parts.tail.map(_capitalize).mkString
      }
    }
  }

  def toSnakeColumnName(name: String): String = {
    val b = new StringBuilder(name.length + 8)
    var i = 0
    var previousunderscore = false
    while (i < name.length) {
      val c = name.charAt(i)
      if (c == '-' || c == '.' || c == ' ') {
        if (!previousunderscore && b.nonEmpty) {
          b.append('_')
          previousunderscore = true
        }
      } else if (c.isUpper) {
        if (b.nonEmpty && !previousunderscore)
          b.append('_')
        b.append(c.toLower)
        previousunderscore = false
      } else if (c == '_') {
        if (!previousunderscore && b.nonEmpty) {
          b.append('_')
          previousunderscore = true
        }
      } else {
        b.append(c.toLower)
        previousunderscore = false
      }
      i += 1
    }
    val raw = b.result()
    raw.dropWhile(_ == '_').reverse.dropWhile(_ == '_').reverse
  }

  def normalizeKnownKeys(
    record: Record,
    knowncanonicalkeys: Set[String]
  ): Consequence[Record] = {
    val normalized = record.fields.map { field =>
      val canonical = toCanonicalCamelName(field.key)
      val target = if (knowncanonicalkeys.contains(canonical)) canonical else field.key
      target -> field
    }
    val collisions = normalized
      .groupBy(_._1)
      .collect {
        case (key, values) if values.lengthCompare(1) > 0 && knowncanonicalkeys.contains(key) =>
          key -> values.map(_._2.key).distinct
      }
      .filter(_._2.lengthCompare(1) > 0)
    if (collisions.nonEmpty) {
      val message = collisions.toVector.sortBy(_._1).map {
        case (key, aliases) => s"$key: ${aliases.mkString(", ")}"
      }.mkString("; ")
      Consequence.argumentInvalid(s"Duplicate property aliases after canonical naming: $message")
    } else {
      Consequence.success(Record(normalized.map {
        case (key, field) => field.copy(key = key)
      }))
    }
  }

  def normalizeAllKeys(record: Record): Consequence[Record] = {
    val normalized = record.fields.map { field =>
      toCanonicalCamelName(field.key) -> field
    }
    val collisions = normalized
      .groupBy(_._1)
      .collect {
        case (key, values) if values.lengthCompare(1) > 0 =>
          key -> values.map(_._2.key).distinct
      }
      .filter(_._2.lengthCompare(1) > 0)
    if (collisions.nonEmpty) {
      val message = collisions.toVector.sortBy(_._1).map {
        case (key, aliases) => s"$key: ${aliases.mkString(", ")}"
      }.mkString("; ")
      Consequence.argumentInvalid(s"Duplicate property aliases after canonical naming: $message")
    } else {
      Consequence.success(Record(normalized.map {
        case (key, field) => field.copy(key = key)
      }))
    }
  }

  private def _has_separator(p: String): Boolean =
    p.exists(c => c == '_' || c == '-' || c == '.' || c.isWhitespace)

  private def _capitalize(p: String): String =
    if (p.isEmpty)
      p
    else
      p.head.toUpper + p.tail
}
