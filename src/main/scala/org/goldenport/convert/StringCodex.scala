package org.goldenport.convert

import java.math.MathContext
import java.nio.charset.StandardCharsets
import java.time.{Clock, ZoneId}
import java.util.Locale
import org.goldenport.Consequence
import org.goldenport.context.ExecutionContext
import org.goldenport.context.{EnvironmentContext, I18nContext, RandomContext, VirtualMachineContext}
import org.goldenport.log.Logger

/*
 * @since   Apr.  3, 2026
 * @version Apr.  3, 2026
 * @author  ASAMI, Tomoharu
 */
trait StringEncoder[A]:
  def encode(value: A)(using ExecutionContext): String

object StringEncoder {
  lazy val storageExecutionContext: ExecutionContext =
    ExecutionContext.Instant(
      ExecutionContext.Core(
        environment = EnvironmentContext.local(),
        vm = VirtualMachineContext.Instant(
          VirtualMachineContext.Core(
          clock = Clock.systemUTC(),
          timezone = ZoneId.of("UTC"),
          encoding = StandardCharsets.UTF_8,
          lineSeparator = "\n",
          mathContext = MathContext.DECIMAL64,
          environmentVariables = Map.empty,
          resourceBundleBaseNames = Nil,
          resourceBundleLocales = Nil,
          resourceBundleResolutionOrder = Nil
        )),
        i18n = I18nContext.Instant(
          I18nContext.Core(
            textNormalizationPolicy = "default",
            textComparisonPolicy = "default",
            dateTimeFormatPolicy = "default",
            locale = Some(Locale.ROOT)
          )
        ),
        locale = Locale.ROOT,
        timezone = ZoneId.of("UTC"),
        encoding = StandardCharsets.UTF_8,
        clock = Clock.systemUTC(),
        math = MathContext.DECIMAL64,
        random = RandomContext.from("fixed"),
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
      )
    )

  def encodeForStorage[A](value: A)(using enc: StringEncoder[A]): String = {
    given ExecutionContext = storageExecutionContext
    enc.encode(value)
  }
}

trait StringDecoder[A]:
  def decode(value: String)(using ExecutionContext): Consequence[A]

trait StringCodex[A]
  extends StringEncoder[A]
     with StringDecoder[A]

trait StringEncodable:
  def encode(using ExecutionContext): String

trait StringDecodable

trait StringCodexable
  extends StringEncodable
     with StringDecodable

trait Encodable
  extends StringEncodable
