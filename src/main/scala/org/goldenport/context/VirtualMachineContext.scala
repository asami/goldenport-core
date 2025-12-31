package org.goldenport.context

import java.math.MathContext
import java.nio.charset.Charset
import java.time.{Clock, ZoneId}
import java.util.Locale

/*
 * @since   Dec. 31, 2025
 * @version Dec. 31, 2025
 * @author  ASAMI, Tomoharu
 */
abstract class VirtualMachineContext extends VirtualMachineContext.Core.Holder {
}

object VirtualMachineContext {
  final case class Core(
    clock: Clock,
    timezone: ZoneId,
    encoding: Charset,
    lineSeparator: String,
    mathContext: MathContext,
    environmentVariables: Map[String, String],
    resourceBundleBaseNames: List[String],
    resourceBundleLocales: List[Locale],
    resourceBundleResolutionOrder: List[String]
  )

  object Core {
    trait Holder {
      def core: Core

      def clock: Clock = core.clock
      def timezone: ZoneId = core.timezone
      def encoding: Charset = core.encoding
      def lineSeparator: String = core.lineSeparator
      def mathContext: MathContext = core.mathContext
      def environmentVariables: Map[String, String] = core.environmentVariables
      def resourceBundleBaseNames: List[String] = core.resourceBundleBaseNames
      def resourceBundleLocales: List[Locale] = core.resourceBundleLocales
      def resourceBundleResolutionOrder: List[String] = core.resourceBundleResolutionOrder
    }
  }

  final case class Instant(core: Core) extends VirtualMachineContext {
  }
}
