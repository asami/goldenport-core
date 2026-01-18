package org.goldenport.configuration

/**
 * Configuration represents a resolved configuration set.
 *
 * This is a simple structural container:
 *   - no validation
 *   - no schema awareness
 *   - no resolution logic
 *
 * All semantics (meaning, constraints, defaults) must be applied
 * by higher layers using this structure.
 */
/*
 * @since   Dec. 18, 2025
 * @version Jan. 16, 2026
 * @author  ASAMI, Tomoharu
 */
case class Configuration(
  values: Map[String, ConfigurationValue]
) {

  /**
   * Get a raw ConfigValue by key.
   */
  def get(key: String): Option[ConfigurationValue] =
    values.get(key)

  /**
   * Convenience accessors (non-normative).
   * These helpers intentionally avoid implicit conversion or coercion.
   */
  def string(key: String): Option[String] =
    values.get(key).collect { case ConfigurationValue.StringValue(v) => v }

  def boolean(key: String): Option[Boolean] =
    values.get(key).collect { case ConfigurationValue.BooleanValue(v) => v }

  def number(key: String): Option[BigDecimal] =
    values.get(key).collect { case ConfigurationValue.NumberValue(v) => v }
}

object Configuration {
  val empty: Configuration = Configuration(Map.empty)
}
