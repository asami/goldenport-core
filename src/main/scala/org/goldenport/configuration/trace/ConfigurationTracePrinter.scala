package org.goldenport.configuration

/**
 * ConfigTracePrinter renders ConfigTrace in human-readable form.
 *
 * This is a presentation utility for CLI / debugging purposes.
 *
 * Responsibilities:
 *   - render effective configuration values
 *   - explain where values came from
 *
 * Non-responsibilities:
 *   - interpretation of configuration semantics
 *   - validation
 *   - formatting policies beyond plain text
 */
/*
 * @since   Dec. 18, 2025
 * @version Jan. 16, 2026
 * @author  ASAMI, Tomoharu
 */
object ConfigurationTracePrinter {

  def print(trace: ConfigurationTrace): Unit = {
    if (trace.entries.isEmpty) {
      println("(no configuration entries)")
    } else {
      trace.entries.toSeq.sortBy(_._1).foreach {
        case (_, resolution) =>
          printResolution(resolution)
      }
    }
  }

  def explain(
    key: String,
    trace: ConfigurationTrace
  ): Unit = {
    trace.entries.get(key) match {
      case Some(resolution) =>
        printResolution(resolution)
      case None =>
        println(s"(no configuration found for key: $key)")
    }
  }

  private def printResolution(
    resolution: ConfigurationResolution
  ): Unit = {
    println(s"${resolution.key} = ${renderValue(resolution.finalValue)}")
    println(s"  origin : ${resolution.origin}")
    resolution.sourceType.foreach(st => println(s"  source type : ${st}"))
    resolution.sourceId.foreach(id => println(s"  source id   : ${id}"))
    printHistory(resolution.history, indent = "  ")
  }

  private def printHistory(
    history: List[ConfigurationResolution],
    indent: String
  ): Unit = {
    if (history.nonEmpty) {
      println(s"${indent}history:")
      history.reverse.foreach { h =>
        println(
          s"$indent- ${h.origin}: ${renderValue(h.finalValue)}"
        )
      }
    }
  }

  private def renderValue(
    value: ConfigurationValue
  ): String =
    value match {
      case ConfigurationValue.StringValue(v) => v
      case other => other.toString
    }
}
