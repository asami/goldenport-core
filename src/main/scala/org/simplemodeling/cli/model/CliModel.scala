package org.simplemodeling.cli.model

/*
 * @since   Dec. 22, 2025
 * @version Dec. 23, 2025
 * @author  ASAMI, Tomoharu
 */
case class Service(name: String, operations: Seq[Operation])

case class Operation(name: String, endpoint: String, schema: OperationSchema)

case class OperationSchema(input: String, output: String)

case class ServiceRegistry(services: Seq[Service], defaultService: Option[String])

/*
 * Protocol-level operation request.
 *
 * Idiom:
 *   - ProtocolRequest (semantic idiom)
 *   - Based on ValueBackedAbstractObject (structural idiom)
 *
 * This represents a parsed, semantically resolved CLI / REST / MCP request,
 * but does NOT imply Command or Query semantics.
 *
 * Classification into Command / Query is a responsibility of the runtime
 * layer (e.g. CNCF), not the CLI or protocol layer.
 */
abstract class OperationRequest extends OperationRequest.Core.Holder {
  def core: OperationRequest.Core
}

object OperationRequest {

  /*
   * Parameter carrier.
   *
   * Grouping parameters into a case class makes the request easy to pass,
   * copy, and transform without affecting the abstract API.
   */
  final case class Core(
    service: String,
    operation: String,
    params: List[String]
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

      def service: String = core.service
      def operation: String = core.operation
      def params: List[String] = core.params
    }
  }

  /*
   * Default concrete implementation.
   *
   * Used by lightweight clients (CLI / REST / MCP) as the standard
   * protocol-level request instance.
   */
  final case class Instance(
    core: Core
  ) extends OperationRequest

  def apply(
    service: String,
    operation: String,
    params: List[String]
  ): OperationRequest =
    Instance(
      Core(
        service,
        operation,
        params
      )
    )
}
