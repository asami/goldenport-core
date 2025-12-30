package org.goldenport.protocol

import org.goldenport.Consequence
import org.goldenport.protocol.handler.ingress.ArgsIngress
import org.goldenport.protocol.spec.ServiceDefinitionGroup
import org.goldenport.protocol.handler.ProtocolHandler

/*
 * @since   Dec. 28, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
case class Protocol(
  services: ServiceDefinitionGroup,
  handler: ProtocolHandler
) {
  def ingress(args: Array[String]): Consequence[ArgsIngress] =
    handler.ingresses.ingress(args)

  def project[Out](kind: org.goldenport.protocol.handler.projection.ProjectionKind[Out]): Consequence[Out] =
    handler.projections.project(kind, services)

  def projectByName(name: String): Consequence[Any] =
    handler.projections.projectByName(name, services)

  def egress[Out](
    kind: org.goldenport.protocol.handler.egress.EgressKind[Out],
    c: org.goldenport.Conclusion
  ): Consequence[Out] =
    handler.egresses.egress(kind, c)
}
