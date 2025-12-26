package org.goldenport.error

/**
 * ErrorStrategy represents a declarative handling intention.
 *
 * It does NOT execute behavior.
 * It does NOT select handlers.
 *
 * ErrorStrategy is attached to Conclusion.Status and interpreted
 * later by ErrorSystem or higher-level frameworks.
 *
 * Typical usages:
 * - retry is allowed / suggested
 * - user input correction is required
 * - escalation is required
 */
/*
 * @since   Dec. 18, 2025
 * @version Dec. 18, 2025
 * @author  ASAMI, Tomoharu
 */
trait ErrorStrategy {
  def id: String
  def description: Option[String] = None
}

/**
 * Default SimpleModeling error strategies.
 *
 * These are core-level, application-agnostic strategies.
 * Frameworks or applications may define their own strategies.
 */
object ErrorStrategy {

  /**
   * No specific strategy.
   * Safe default.
   */
  case object None extends ErrorStrategy {
    val id = "sm.none"
    override val description = Some("No specific error handling strategy")
  }

  /**
   * Retry may succeed.
   * Typically used for transient failures.
   */
  case object Retry extends ErrorStrategy {
    val id = "sm.retry"
    override val description = Some("Retry may succeed")
  }

  /**
   * User or caller input correction is required.
   */
  case object InputRequired extends ErrorStrategy {
    val id = "sm.input_required"
    override val description = Some("Caller input correction is required")
  }

  /**
   * Escalation is required.
   * Typically indicates defects or operator intervention.
   */
  case object Escalate extends ErrorStrategy {
    val id = "sm.escalate"
    override val description = Some("Escalation is required")
  }
}
