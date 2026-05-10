package org.goldenport.error

import org.goldenport.Conclusion
import org.goldenport.conclusion.{Interpretation, Disposition}
import org.goldenport.observation.Observation

/**
 * DefaultErrorSystem is the minimal core implementation.
 *
 * Design intent:
 *
 * - Provide a safe, deterministic default mapping
 * - Never throw exceptions
 * - Never inspect framework-specific semantics
 * - Never infer retry / escalation runtime policy
 *
 * This implementation is intentionally conservative.
 *
 * It is suitable for:
 * - simplemodeling-lib standalone usage
 * - Config loading / validation phase
 * - CLI exploratory development
 *
 * Higher layers (CNCF, SIE) are expected to override this.
 */
/*
 * @since   Dec. 18, 2025
 *  version Jan. 28, 2026
 * @version May. 11, 2026
 * @author  ASAMI, Tomoharu
 */
object DefaultErrorSystem extends ErrorSystem {

  /**
   * Minimal interpretation rule:
   *
   * - If Observation contains an exception → Error
   * - Otherwise → Success
   *
   * No use of:
   * - Observation.strategy
   * - Observation.handler
   * - Observation.code
   */
  override def conclude(observation: Observation): Conclusion = {
    observation.exception match {
      case Some(_) =>
        Conclusion(
          status = Conclusion.Status(),
          observation = observation,
          interpretation = Interpretation.defect,
          disposition = Disposition.defect,
          previous = None
        )
      case None =>
        Conclusion(
          status = Conclusion.Status(),
          observation = observation,
          interpretation = Interpretation.success,
          disposition = Disposition.none,
          previous = None
        )
    }
  }
}
