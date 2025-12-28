package org.goldenport.protocol

import org.goldenport.Consequence
import org.goldenport.protocol.logic.ProtocolLogic
import org.goldenport.protocol.operation.OperationRequest
/*
 * @since   Dec. 26, 2025
 * @version Dec. 28, 2025
 * @author  ASAMI, Tomoharu
 */
class ProtocolEngine(
  val protocol: Protocol,
  val logic: ProtocolLogic
) {
  def makeOperationRequest(args: Array[String]): Consequence[OperationRequest] =
    logic.makeOperationRequest(args)
}

object ProtocolEngine {
  def create(protocol: Protocol): ProtocolEngine =
    new ProtocolEngine(protocol, ProtocolLogic(protocol))
}
