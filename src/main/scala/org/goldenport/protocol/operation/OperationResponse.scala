package org.goldenport.protocol.operation

import org.goldenport.protocol.Response
import org.goldenport.protocol.scalar.ScalarValue

/*
 * @since   Dec. 28, 2025
 * @version Jan.  2, 2026
 * @author  ASAMI, Tomoharu
 */
/**
 * OperationResponse represents the semantic result of an operation execution.
 * It is converted to Response at the protocol boundary.
 */
abstract class OperationResponse {
  def toResponse: Response
}

object OperationResponse {
  case object Void extends OperationResponse {
    def toResponse = Response.Void
  }

  /**
   * Scalar requires an implicit ScalarValue[T].
   * It guarantees the result can safely exit via Protocol.egress.
   */
  final case class Scalar[T: ScalarValue](value: T) extends OperationResponse {
    def toResponse: Response = Response.Scalar(value)
  }
}
