package org.goldenport.protocol.handler.egress

import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.protocol.spec.OperationDefinition

/*
 * @since   Dec. 28, 2025
 * @version Dec. 28, 2025
 * @author  ASAMI, Tomoharu
 */
abstract class Egress[T] {
  def decode(op: OperationDefinition, res: OperationResponse): Consequence[T]
}

case class EgressCollection(
  egresses: Vector[Egress[?]]
)

abstract class JsonEgress extends Egress[Json] {
  override def decode(op: OperationDefinition, res: OperationResponse): Consequence[Json] = {
    val _ = op
    decode(res)
  }

  def decode(res: OperationResponse): Consequence[Json]
}
