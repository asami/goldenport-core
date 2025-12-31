package org.goldenport.context

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

class ExecutionContextSpec extends AnyWordSpec with Matchers {

  "ExecutionContext" should {

    "compose EnvironmentContext, VirtualMachineContext, and I18nContext" in {
      val envCore =
        EnvironmentContext.Core(
          environment = "local"
        )

      val vmCore =
        VirtualMachineContext.Core(
          clock = java.time.Clock.systemUTC(),
          timezone = java.time.ZoneOffset.UTC,
          encoding = java.nio.charset.StandardCharsets.UTF_8,
          lineSeparator = "\n",
          mathContext = java.math.MathContext.DECIMAL64,
          environmentVariables = Map("TEST_ENV" -> "true"),
          resourceBundleBaseNames = Nil,
          resourceBundleLocales = Nil,
          resourceBundleResolutionOrder = Nil
        )

      val i18nCore =
        I18nContext.Core(
          textNormalizationPolicy = "JapaneseCanonical",
          textComparisonPolicy = "JapaneseCollation",
          dateTimeFormatPolicy = "Iso8601",
          locale = None
        )

      val env = EnvironmentContext.Instant(envCore)
      val vm  = VirtualMachineContext.Instant(vmCore)
      val i18n = I18nContext.Instant(i18nCore)

      val ec =
        ExecutionContext.Instant(
          ExecutionContext.Core(
            environment = env,
            vm = vm,
            i18n = i18n,
            locale = java.util.Locale.JAPAN,
            timezone = java.time.ZoneOffset.UTC,
            encoding = java.nio.charset.StandardCharsets.UTF_8,
            clock = java.time.Clock.systemUTC(),
            math = java.math.MathContext.DECIMAL64,
            random = RandomContext.from("deterministic"),
            logger = new org.goldenport.log.Logger {
              def trace(message: => String): Unit = ()
              def debug(message: => String): Unit = ()
              def info(message: => String): Unit = ()
              def warn(message: => String): Unit = ()
              def error(message: => String): Unit = ()
              def error(cause: Throwable, message: => String): Unit = ()
              def fatal(message: => String): Unit = ()
              def fatal(cause: Throwable, message: => String): Unit = ()
            }
          )
        )

      ec.environment shouldBe env
      ec.vm shouldBe vm
      ec.i18n shouldBe i18n
    }
  }
}
