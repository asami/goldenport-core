package org.goldenport.context

import java.nio.charset.Charset
import java.time.{Clock, ZoneId}
import java.util.Locale
import java.math.MathContext

import org.goldenport.log.Logger

/**
 * ExecutionContext is the central execution model of goldenport core.
 *
 * It represents explicit, immutable execution assumptions required to
 * produce meaningful and testable results.
 *
 * This is a value-backed abstract class.
 */
/*
 * @since   Dec. 30, 2025
 * @version Dec. 31, 2025
 * @author  ASAMI, Tomoharu
 */
abstract class ExecutionContext extends ExecutionContext.Core.Holder {
}

object ExecutionContext {
  /**
   * Core is the canonical value implementation of ExecutionContext.
   *
   * It is intended for core, CLI, and test usage.
   * CNCF may wrap or adapt this with additional runtime state.
   */
  final case class Core(
    environment: EnvironmentContext,
    vm: VirtualMachineContext,
    i18n: I18nContext,
    locale: Locale,
    timezone: ZoneId,
    encoding: Charset,
    clock: Clock,
    math: MathContext,
    random: RandomContext,
    logger: Logger
  )
  object Core {
    trait Holder {
      def core: Core

      def environment: EnvironmentContext = core.environment
      def vm: VirtualMachineContext = core.vm
      def i18n: I18nContext = core.i18n
      def locale: Locale = core.locale
      def timezone: ZoneId = core.timezone
      def encoding: Charset = core.encoding

      def clock: Clock = core.clock
      def math: MathContext = core.math
      def random: RandomContext = core.random

      def logger: Logger = core.logger
    }
  }

  case class Instant(core: Core) extends ExecutionContext {
  }
}
