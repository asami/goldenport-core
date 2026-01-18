package org.goldenport.protocol.handler

import org.goldenport.protocol.handler.ingress.*
import org.goldenport.protocol.handler.projection.*
import org.goldenport.protocol.handler.egress.*

/*
 * @since   Dec. 28, 2025
 * @version Jan. 17, 2026
 * @author  ASAMI, Tomoharu
 */
case class ProtocolHandler(
  ingresses: IngressCollection= IngressCollection.empty,
  egresses: EgressCollection = EgressCollection.empty,
  projections: ProjectionCollection = ProjectionCollection.empty
)

object ProtocolHandler {
  val empty = ProtocolHandler()
  val default = ProtocolHandler(
    IngressCollection(ArgsIngress(), RestIngress())
  )
}
