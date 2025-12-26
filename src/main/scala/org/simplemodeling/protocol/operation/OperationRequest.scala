package org.simplemodeling.protocol.operation

import org.simplemodeling.protocol.Argument
import org.simplemodeling.protocol.Property
import org.simplemodeling.protocol.Switch

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
 * @version Dec. 26, 2025
 * @author  ASAMI, Tomoharu
 */
abstract class OperationRequest {
}

object OperationRequest {

  /*
   * Parameter carrier.
   *
   * Grouping parameters into a case class makes the request easy to pass,
   * copy, and transform without affecting the abstract API.
   */
  final case class Core(
    service: Option[String],
    operation: String,
    arguments: List[Argument],
    switches: List[Switch],
    properties: List[Property]
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

      def service: Option[String] = core.service
      def operation: String = core.operation
      def arguments: List[Argument] = core.arguments
      def switches: List[Switch] = core.switches
      def properties: List[Property] = core.properties
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
  ) extends OperationRequest with Core.Holder

  def apply(
    service: Option[String],
    operation: String,
    arguments: List[Argument],
    switches: List[Switch],
    properties: List[Property]
  ): OperationRequest =
    Instance(
      Core(
        service,
        operation,
        arguments,
        switches,
        properties
      )
    )
}
