package org.goldenport.protocol.operation

import org.goldenport.Consequence
import org.goldenport.text.Presentable
import org.goldenport.protocol.Response
import org.goldenport.protocol.scalar.ScalarValue
import org.goldenport.http.HttpResponse
import org.goldenport.record.Record
import org.goldenport.record.io.RecordEncoder

/*
 * @since   Dec. 28, 2025
 *  version Jan.  2, 2026
 *  version Jan. 21, 2026
 *  version Feb. 26, 2026
 * @version Mar.  2, 2026
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
  val void = Void()

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
    override def print: String = response.print
    override def display: String = response.display
    override def show: String = response.show
  }

  /**
   * Scalar requires an implicit ScalarValue[T].
   * It guarantees the result can safely exit via Protocol.egress.
   */
  final case class Scalar[T](value: T)(using val scalarValue: ScalarValue[T]) extends OperationResponse {
    def toResponse: Response = Response.Scalar(value)(using scalarValue)
    override def print: String = Presentable.print(value)
    override def display: String = Presentable.display(value)
    override def show: String = Presentable.show(value)
  }

  final case class Json(json: io.circe.Json) extends OperationResponse {
    def toResponse: Response = Response.Json(json.toString) // TODO
    override def print: String = Presentable.print(json)
    override def display: String = Presentable.display(json)
    override def show: String = Presentable.show(json)
  }

  final case class Yaml(yaml: String) extends OperationResponse {
    def toResponse: Response = Response.Yaml(yaml) // TODO
    override def print: String = Presentable.print(yaml)
    override def display: String = Presentable.display(yaml)
    override def show: String = Presentable.show(yaml)
  }

  final case class RecordResponse(record: Record) extends OperationResponse {
    private def _json = RecordEncoder().json(record)
    def toResponse: Response = Response.Json(_json) // Record
    override def print: String = Presentable.print(_json)
    override def display: String = Presentable.display(_json)
    override def show: String = Presentable.show(_json)
  }

  final case class Opaque(value: Any) extends OperationResponse {
    def toResponse: Response = Response.Opaque(value)
    override def print: String = Presentable.print(value)
    override def display: String = Presentable.display(value)
    override def show: String = Presentable.show(value)
  }

  def apply(rec: Record): OperationResponse = RecordResponse(rec)

  def create(p: Any): OperationResponse = p match {
    case m: String => Scalar(m)
    case m: HttpResponse => Http(m)
    case m: Unit => Void()
    case m: Response => from(m)
    case m: Record => RecordResponse(m)
    case m => Opaque(m)
  }

  def from(p: Response): OperationResponse = p match {
    case m: Response.Void => Void()
    case Response.Json(value) => Consequence.failNotImplemented.RAISE
    case Response.Yaml(value) => Yaml(value)
    case m: Response.Scalar[?] => Scalar(m.value)(using m.scalarValue)
    case Response.Opaque(value) => Opaque(value)
  }
}
