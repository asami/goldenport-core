package org.goldenport.protocol.operation

import org.goldenport.text.Presentable
import org.goldenport.protocol.Response
import org.goldenport.protocol.scalar.ScalarValue
import org.goldenport.http.HttpResponse

/*
 * @since   Dec. 28, 2025
 *  version Jan.  2, 2026
 * @version Jan. 20, 2026
 * @author  ASAMI, Tomoharu
 */
/**
 * OperationResponse represents the semantic result of an operation execution.
 * It is converted to Response at the protocol boundary.
 */
abstract class OperationResponse extends Presentable {
  def toResponse: Response
}

object OperationResponse {
  final case class Void() extends OperationResponse {
    def toResponse: Response = Response.Void()
    override def print: String = "Void"
  }

  /**
   * Http wraps a protocol-boundary response, not a domain value.
   *
   * This is intended for client/CLI/server layers to interpret directly.
   */
  final case class Http(
    response: HttpResponse
  ) extends OperationResponse {
    def toResponse: Response = Response.Void()
    override def print: String = toString
  }

  /**
   * Scalar requires an implicit ScalarValue[T].
   * It guarantees the result can safely exit via Protocol.egress.
   */
  final case class Scalar[T: ScalarValue](value: T) extends OperationResponse {
    def toResponse: Response = Response.Scalar(value)
    override def print: String = value.toString
  }

  final case class Opaque(value: Any) extends OperationResponse {
    def toResponse: Response = Response.Opaque(value)
    override def print: String = value.toString
  }

  def create(p: Any): OperationResponse = p match {
    case m: String => Scalar(m)
    case m: HttpResponse => Http(m)
    case m: Unit => Void()
    case m => Opaque(m)
  }
}
