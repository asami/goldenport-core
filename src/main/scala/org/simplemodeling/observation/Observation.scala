package org.simplemodeling.observation

import java.time.Instant
import org.simplemodeling.datatype.I18nMessage

/*
 * @since   Jul. 19, 2025
 *  version Jul. 23, 2025
 *  version Aug.  1, 2025
 * @version Dec. 18, 2025
 * @author  ASAMI, Tomoharu
 */
/*
 * NOTE:
 * In the current phase (Config / CLI development),
 * the following fields are intentionally NOT used:
 *
 *   - strategy
 *   - handler
 *   - code / DetailCode
 *
 * Observation represents factual events only.
 * Interpretation (reaction, handling, classification)
 * is delegated to ErrorSystem / higher layers.
 *
 * See TODO.md and docs/notes/error-model-exploration.md
 * for deferred design items.
 */
case class Observation(
  phenomenon: Phenomenon,
  causeKind: CauseKind,
  cause: Option[Cause],
  severity: Severity,
  strategy: Strategy,
  handler: Handler,
  timestamp: Instant,
  subject: Subject,
  `object`: Resource,
  agent: Agent,
  location: SystemLocation,
  traceId: Option[TraceId],
  spanId: Option[SpanId],
  message: Option[I18nMessage],
  exception: Option[Throwable],
  properties: Map[String, String] = Map.empty
) {
  import Observation._

  def code: Code = {
    val detail = makeDetailCode(phenomenon.code, causeKind.code, strategy.code, handler.code)
    ???
  }

  def makeDetailCode(a: Int, b: Int, c: Int, d: Int): Int =
    a * 1000 + b * 100 + c * 10 + d
}

object Observation {
  case class Code(status: Int, detail: Long) {

  }
  object Code {
    def makeDetailCode(a: Int, b: Int, c: Int, d: Int): Int = {
      ???
    }
  }
}
