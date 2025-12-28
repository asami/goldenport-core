package org.goldenport.protocol.handler

import org.goldenport.protocol.handler.ingress.*
import org.goldenport.protocol.handler.egress.*

/*
 * @since   Dec. 28, 2025
 * @version Dec. 28, 2025
 * @author  ASAMI, Tomoharu
 */
case class ProtocolHandler(
  ingresses: IngressCollection,
  egresses: EgressCollection
)
