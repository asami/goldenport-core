package org.goldenport.configuration

import org.goldenport.configuration.Configuration
import org.goldenport.configuration.ConfigurationTrace
import org.goldenport.convert.ValueReader
import org.goldenport.convert.TemporalValueReader
import org.goldenport.context.ExecutionContext
import org.goldenport.Consequence

/**
 * ResolvedConfig represents the result of configuration resolution.
 *
 * It contains both:
 *   - the final resolved configuration values
 *   - the trace information describing how each value was determined
 *
 * Design principles:
 *   - Boring over clever
 *   - Explicit over convenient
 *   - Deterministic over flexible
 *
 * Notes:
 *   - Consumers may ignore `trace` in normal operation.
 *   - `trace` exists for debugging, explanation, CLI output,
 *     and AI-assisted reasoning.
 */
/*
 * @since   Dec. 18, 2025
 * @version Dec. 18, 2025
 * @author  ASAMI, Tomoharu
 */
case class ResolvedConfiguration(
  configuration: Configuration,
  trace: ConfigurationTrace
) {
  def get[T](key: String)(using reader: ValueReader[T]): Consequence[Option[T]] = {
    configuration.get(key) match {
      case None => Consequence.success(None)
      case Some(value) =>
        reader.read(value) match {
          case Some(v) => Consequence.success(Some(v))
          case None => Consequence.failure(s"Invalid value for key: $key")
        }
    }
  }

  def getTemporal[T](key: String)(using reader: TemporalValueReader[T], ctx: ExecutionContext): Consequence[Option[T]] = {
    configuration.get(key) match {
      case None =>
        Consequence.success(None)
      case Some(value) =>
        reader.read(value).map(Some(_))
    }
  }
}
