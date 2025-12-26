package org.goldenport.protocol

import org.goldenport.protocol.logic.ProtocolLogic
/*
 * @since   Dec. 26, 2025
 * @version Dec. 26, 2025
 * @author  ASAMI, Tomoharu
 */
class ProtocolEngine(val logic: ProtocolLogic) {
}

object ProtocolEngine {
  def create(): ProtocolEngine = new ProtocolEngine(new ProtocolLogic())
}
