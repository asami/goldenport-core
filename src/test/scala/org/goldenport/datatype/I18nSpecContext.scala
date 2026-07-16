package org.goldenport.datatype

import java.nio.charset.StandardCharsets
import java.time.{Clock, ZoneOffset}
import java.util.Locale
import org.goldenport.context.{EntropyContext, EnvironmentContext, ExecutionContext, I18nContext, RandomContext, VirtualMachineContext}
import org.goldenport.log.Logger

/*
 * @since   Jul. 15, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
private[datatype] object I18nSpecContext {
  def create(
    locale: Locale,
    randomseed: String,
    allowedlocales: Option[Set[Locale]] = None
  ): ExecutionContext = {
    val environment = EnvironmentContext.Instant(EnvironmentContext.Core("test"))
    val vm = VirtualMachineContext.Instant(VirtualMachineContext.Core(
      clock = Clock.systemUTC(),
      timezone = ZoneOffset.UTC,
      encoding = StandardCharsets.UTF_8,
      lineSeparator = "\n",
      mathContext = java.math.MathContext.DECIMAL64,
      environmentVariables = Map.empty,
      resourceBundleBaseNames = Nil,
      resourceBundleLocales = Nil,
      resourceBundleResolutionOrder = Nil
    ))
    val i18n = I18nContext.Instant(I18nContext.Core(
      textNormalizationPolicy = "none",
      textComparisonPolicy = "unicode",
      dateTimeFormatPolicy = "iso-8601",
      locale = Some(locale),
      allowedLocales = allowedlocales
    ))
    ExecutionContext.Instant(ExecutionContext.Core(
      environment = environment,
      vm = vm,
      i18n = i18n,
      locale = locale,
      timezone = ZoneOffset.UTC,
      encoding = StandardCharsets.UTF_8,
      clock = Clock.systemUTC(),
      math = java.math.MathContext.DECIMAL64,
      random = RandomContext.from(randomseed),
      entropy = EntropyContext.deterministic(randomseed),
      logger = new Logger {
        def trace(message: => String): Unit = ()
        def debug(message: => String): Unit = ()
        def info(message: => String): Unit = ()
        def warn(message: => String): Unit = ()
        def error(message: => String): Unit = ()
        def error(cause: Throwable, message: => String): Unit = ()
        def fatal(message: => String): Unit = ()
        def fatal(cause: Throwable, message: => String): Unit = ()
      }
    ))
  }
}
