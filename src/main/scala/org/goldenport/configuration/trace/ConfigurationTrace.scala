package org.goldenport.configuration

/**
 * ConfigurationTrace captures how configuration values were resolved.
 *
 * This is a descriptive data structure:
 *   - no execution semantics
 *   - no side effects
 *   - no policy decisions
 *
 * Intended usage:
 *   - explain-config / debug output
 *   - CLI inspection
 *   - test assertions
 *   - AI-assisted reasoning
 */
/*
 * @since   Dec. 18, 2025
 * @version Jan. 16, 2026
 * @author  ASAMI, Tomoharu
 */
case class ConfigurationTrace(
  entries: Map[String, ConfigurationResolution]
) {
  def get(key: String): Option[ConfigurationResolution] =
    entries.get(key)
}

object ConfigurationTrace {
  val empty: ConfigurationTrace = ConfigurationTrace(Map.empty)
}

case class ConfigurationResolution(
  key: String,
  finalValue: ConfigurationValue,
  origin: ConfigurationOrigin,
  history: List[ConfigurationResolution],
  sourceType: Option[String] = None,
  sourceId: Option[String] = None
)

sealed trait ConfigurationOrigin

object ConfigurationOrigin {
  case object Default extends ConfigurationOrigin
  case object Home extends ConfigurationOrigin
  case object Project extends ConfigurationOrigin
  case object Cwd extends ConfigurationOrigin
  case object Environment extends ConfigurationOrigin
  case object Arguments extends ConfigurationOrigin
  case object Resource extends ConfigurationOrigin
}
