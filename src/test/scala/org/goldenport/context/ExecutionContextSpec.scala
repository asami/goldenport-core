package org.goldenport.context

import java.nio.charset.StandardCharsets
import java.time.{Clock, ZoneOffset}
import java.util.Locale

import org.goldenport.log.Logger
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class ExecutionContextSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks {
  private val _logger = new Logger {
    def trace(message: => String): Unit = ()
    def debug(message: => String): Unit = ()
    def info(message: => String): Unit = ()
    def warn(message: => String): Unit = ()
    def error(message: => String): Unit = ()
    def error(cause: Throwable, message: => String): Unit = ()
    def fatal(message: => String): Unit = ()
    def fatal(cause: Throwable, message: => String): Unit = ()
  }

  "ExecutionContext" should {
    "compose independent environment, VM, i18n, random, and entropy capabilities" in {
      val seedgen = Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
      forAll(seedgen) { seed =>
        Given("explicit core contexts and independent random and entropy capabilities")
        val environment = EnvironmentContext.Instant(EnvironmentContext.Core("local"))
        val vm = VirtualMachineContext.Instant(VirtualMachineContext.Core(
          clock = Clock.systemUTC(),
          timezone = ZoneOffset.UTC,
          encoding = StandardCharsets.UTF_8,
          lineSeparator = "\n",
          mathContext = java.math.MathContext.DECIMAL64,
          environmentVariables = Map("TEST_ENV" -> "true"),
          resourceBundleBaseNames = Nil,
          resourceBundleLocales = Nil,
          resourceBundleResolutionOrder = Nil
        ))
        val i18n = I18nContext.Instant(I18nContext.Core(
          textNormalizationPolicy = "JapaneseCanonical",
          textComparisonPolicy = "JapaneseCollation",
          dateTimeFormatPolicy = "Iso8601",
          locale = None
        ))
        val random = RandomContext.seeded(seed)
        val entropy = EntropyContext.deterministic(seed)

        When("an execution context is composed")
        val context = ExecutionContext.Instant(ExecutionContext.Core(
          environment = environment,
          vm = vm,
          i18n = i18n,
          locale = Locale.JAPAN,
          timezone = ZoneOffset.UTC,
          encoding = StandardCharsets.UTF_8,
          clock = Clock.systemUTC(),
          math = java.math.MathContext.DECIMAL64,
          random = random,
          entropy = entropy,
          logger = _logger
        ))

        Then("the holder exposes every supplied capability without reinterpretation")
        context.environment shouldBe environment
        context.vm shouldBe vm
        context.i18n shouldBe i18n
        context.random should be theSameInstanceAs random
        context.entropy should be theSameInstanceAs entropy
      }
    }
  }
}
