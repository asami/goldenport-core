package org.goldenport.protocol.operation

import org.goldenport.text.Presentable
import org.goldenport.protocol.Request
import org.goldenport.protocol.Argument
import org.goldenport.protocol.Property
import org.goldenport.protocol.Switch

/*
 * Protocol-level operation request.
 *
 * This class serves as a minimal extension point for defining concrete
 * operation request types (e.g. Command / Query implementations).
 *
 * It does not prescribe a value-backed structure by itself.
 */
/*
 * @since   Dec. 23, 2025
 *  version Dec. 26, 2025
 * @version Jan. 20, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class OperationRequest extends Presentable {
  def request: Request

  override def print: String = request.print
}

object OperationRequest {

  /*
   * Parameter carrier.
   *
   * Grouping parameters into a case class makes the request easy to pass,
   * copy, and transform without affecting the abstract API.
   */
  final case class Core(
    request: Request
  )

  object Core {

    /*
     * Public-facing accessor.
     *
     * Exposes Core fields as stable members while keeping the internal
     * representation encapsulated.
     */
    trait Holder {
      def core: Core

      def request: Request = core.request
    }
  }

  /*
   * Lightweight default implementation.
   *
   * This implementation carries request data via Core and is intended for
   * inline or lightweight use cases where a dedicated OperationRequest
   * subclass is not required.
   */
  final case class Instance(
    core: Core
  ) extends OperationRequest with Core.Holder {
    override def print: String = request.print
  }

  def apply(req: Request): OperationRequest =
    Instance(Core(req))

  def apply(
    service: Option[String],
    operation: String,
    arguments: List[Argument],
    switches: List[Switch],
    properties: List[Property]
  ): OperationRequest =
    Instance(
      Core(Request(None, service, operation, arguments, switches, properties))
    )
}
