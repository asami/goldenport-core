package org.goldenport.protocol

import org.goldenport.Consequence
import org.goldenport.protocol.handler.ingress.ArgsIngress
import org.goldenport.protocol.spec.ServiceDefinitionGroup
import org.goldenport.protocol.handler.ProtocolHandler

/*
 * @since   Dec. 28, 2025
 * @version Dec. 28, 2025
 * @author  ASAMI, Tomoharu
 */
case class Protocol(
  services: ServiceDefinitionGroup,
  handler: ProtocolHandler
) {
  def takeIngress(args: Array[String]): Consequence[ArgsIngress] =
    handler.ingresses.takeIngress(args)
}
