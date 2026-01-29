package org.goldenport.error

import org.goldenport.Conclusion
import org.goldenport.provisional.observation.Observation

/**
 * ErrorSystem is responsible for interpreting Observations
 * and producing Conclusions.
 *
 * This is a core, minimal abstraction.
 *
 * Design principles:
 *
 * - Observation represents factual events only.
 * - ErrorSystem performs interpretation and classification.
 * - No framework-specific logic (HTTP, CLI, MCP, CNCF, SIE).
 * - No side effects (logging, exporting, I/O).
 * - Configurable / replaceable via Abstract Factory pattern.
 *
 * In the current phase (Config / CLI development):
 *
 * - strategy / handler / code inside Observation are ignored
 * - interpretation is intentionally minimal
 *
 * Higher layers (CNCF, SIE, CLI) may provide:
 * - richer mappings
 * - context-aware overrides
 * - transport-specific interpretations
 */
/*
 * @since   Dec. 18, 2025
 * @version Jan. 25, 2026
 * @author  ASAMI, Tomoharu
 */
trait ErrorSystem {

  /**
   * Interpret an Observation and produce a Conclusion.
   *
   * This method MUST:
   * - Be pure (no side effects)
   * - Never throw exceptions
   * - Always return a Conclusion
   *
   * This is the primary interpretation entry point.
   */
  def conclude(observation: Observation): Conclusion

  /**
   * Optional hook for chaining or enrichment.
   *
   * Default behavior should be identity.
   * Higher layers may override to:
   * - merge context
   * - attach previous conclusions
   * - apply policy-based refinement
   */
  def refine(
    observation: Observation,
    conclusion: Conclusion
  ): Conclusion = conclusion
}
