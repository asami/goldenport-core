package org.goldenport.error

import org.goldenport.Conclusion
import org.goldenport.observation.Observation

/**
 * DefaultErrorSystem is the minimal core implementation.
 *
 * Design intent:
 *
 * - Provide a safe, deterministic default mapping
 * - Never throw exceptions
 * - Never inspect framework-specific semantics
 * - Never infer retry / escalation / handling strategies
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
 * @version Dec. 18, 2025
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
   * - Observation.code / DetailCode
   */
  override def conclude(observation: Observation): Conclusion = {
    observation.exception match {
      case Some(_) =>
        Conclusion(
          status = Conclusion.Status(
            webCode = Conclusion.WebCode(500)
          ),
          observation = observation,
          previous = None
        )
      case None =>
        Conclusion(
          status = Conclusion.Status(
            webCode = Conclusion.WebCode(200)
          ),
          observation = observation,
          previous = None
        )
    }
  }
}
