package org.goldenport.configuration

/**
 * ConfigurationValue represents a raw configuration value.
 *
 * Design goals:
 *   - Schema-agnostic
 *   - Serialization-friendly (JSON / YAML / HOCON)
 *   - Traceable and explainable
 *   - No validation or coercion logic
 *
 * This is a structural model, not a semantic one.
 */
/*
 * @since   Dec. 18, 2025
 * @version Jan. 16, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait ConfigurationValue

object ConfigurationValue {

  /** Primitive values */
  final case class StringValue(value: String) extends ConfigurationValue
  final case class NumberValue(value: BigDecimal) extends ConfigurationValue
  final case class BooleanValue(value: Boolean) extends ConfigurationValue

  /** Composite values */
  final case class ListValue(values: List[ConfigurationValue]) extends ConfigurationValue
  final case class ObjectValue(values: Map[String, ConfigurationValue]) extends ConfigurationValue

  /** Explicit null / disabled */
  case object NullValue extends ConfigurationValue

  /**
   * Utility helpers (non-normative)
   *
   * These helpers are intentionally minimal and optional.
   */
  object syntax {
    def str(p: String): ConfigurationValue = StringValue(p)
    def num(p: BigDecimal): ConfigurationValue = NumberValue(p)
    def bool(p: Boolean): ConfigurationValue = BooleanValue(p)
    def list(p: List[ConfigurationValue]): ConfigurationValue = ListValue(p)
    def obj(p: Map[String, ConfigurationValue]): ConfigurationValue = ObjectValue(p)
    val nullValue: ConfigurationValue = NullValue
  }
}
