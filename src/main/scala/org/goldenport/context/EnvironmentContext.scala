package org.goldenport.context

/*
 * @since   Dec. 31, 2025
 * @version Dec. 31, 2025
 * @author  ASAMI, Tomoharu
 */
abstract class EnvironmentContext extends EnvironmentContext.Core.Holder {
}

object EnvironmentContext {
  final case class Core(
    environment: String
  )

  object Core {
    trait Holder {
      def core: Core

      def environment: String = core.environment
    }
  }

  final case class Instant(core: Core) extends EnvironmentContext {
  }

  def localCore(): Core = Core("local")

  def local(): EnvironmentContext = Instant(localCore())
}
