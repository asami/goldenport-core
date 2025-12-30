package org.goldenport.context

import java.math.MathContext
import java.nio.charset.Charset
import java.time.{Clock, ZoneId}
import java.util.Locale

import org.goldenport.config.Config
import org.goldenport.log.Logger

/*
 * @since   Dec. 30, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
trait ExecutionContextFactory {
  def coreFrom(config: Config.Core): ExecutionContext.Core
  def instantFrom(config: Config.Core): ExecutionContext.Instant
}

final class DefaultExecutionContextFactory(
  defaultlogger: Logger,
  defaultclock: Clock
) extends ExecutionContextFactory {
  def coreFrom(config: Config.Core): ExecutionContext.Core =
    ExecutionContext.Core(
      locale = Locale.forLanguageTag(config.locale),
      timezone = ZoneId.of(config.timezone),
      encoding = Charset.forName(config.encoding),
      clock = defaultclock,
      mathContext = _math_context(config.mathContext),
      random = RandomContext.from(config.random),
      logger = defaultlogger
    )

  def instantFrom(config: Config.Core): ExecutionContext.Instant =
    ExecutionContext.Instant(coreFrom(config))

  private def _math_context(name: String): MathContext =
    name match {
      case "DECIMAL32" => MathContext.DECIMAL32
      case "DECIMAL64" => MathContext.DECIMAL64
      case "DECIMAL128" => MathContext.DECIMAL128
      case "UNLIMITED" => MathContext.UNLIMITED
      case _ =>
        _precision_math_context(name)
    }

  private def _precision_math_context(name: String): MathContext = {
    val precision = name.toIntOption.getOrElse(MathContext.DECIMAL64.getPrecision)
    new MathContext(precision)
  }
}
