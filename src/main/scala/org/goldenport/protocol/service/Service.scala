package org.goldenport.protocol.service

import org.goldenport.protocol.spec.ServiceDefinition

/*
 * @since   Jan.  3, 2026
 * @version Jan.  3, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class Service extends Service.Core.Holder {
}

object Service {
  final case class Core(
    serviceDefinition: ServiceDefinition
  )

  object Core {
    trait Holder {
      def core: Core

      def serviceDefinition: ServiceDefinition = core.serviceDefinition
    }
  }
}
