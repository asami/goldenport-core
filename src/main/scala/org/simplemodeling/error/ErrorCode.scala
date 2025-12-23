package org.simplemodeling.error

/**
 * ErrorCode represents a semantic error identifier.
 *
 * This is a stable, declarative identifier used by:
 * - Conclusion.Status (core)
 * - ErrorSystem (mapping / interpretation)
 * - Application-specific error translation
 *
 * ErrorCode itself carries no behavior.
 * It is intentionally minimal.
 */
/*
 * @since   Dec. 18, 2025
 * @version Dec. 18, 2025
 * @author  ASAMI, Tomoharu
 */
trait ErrorCode {
  def id: String
  def description: Option[String] = None
}

/**
 * Default SimpleModeling error codes.
 *
 * These are core-level, application-agnostic codes.
 * Applications and frameworks (CNCF, SIE) may define
 * their own ErrorCode implementations.
 */
object ErrorCode {

  /**
   * An unspecified internal error.
   *
   * Used as a safe default when no detailed classification
   * is available.
   */
  case object InternalError extends ErrorCode {
    val id = "sm.internal_error"
    override val description = Some("Unspecified internal error")
  }

  /**
   * Configuration-related error.
   *
   * Typically raised during config loading / validation phase.
   */
  case object ConfigError extends ErrorCode {
    val id = "sm.config_error"
    override val description = Some("Configuration error")
  }
}
