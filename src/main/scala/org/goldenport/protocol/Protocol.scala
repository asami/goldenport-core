package org.goldenport.protocol

import org.goldenport.Consequence
import org.goldenport.protocol.handler.ingress.ArgsIngress
import org.goldenport.protocol.spec.*
import org.goldenport.protocol.handler.ProtocolHandler
import org.goldenport.protocol.handler.egress.Egress
import org.goldenport.protocol.handler.projection.ProjectionKind

/*
 * @since   Dec. 28, 2025
 *  version Dec. 31, 2025
 * @version Jan. 17, 2026
 * @author  ASAMI, Tomoharu
 */
case class Protocol(
  services: ServiceDefinitionGroup = ServiceDefinitionGroup.empty,
  handler: ProtocolHandler = ProtocolHandler.empty
) {

  /**
   * Canonical entry point for selecting ingress by input kind.
   *
   * This generic API dispatches to the appropriate ingress handler based on the input type.
   * For example, Array[String] will select ArgsIngress. Future extensions (e.g.n, HttpRequest)
   * should be added here as new cases.
   *
   * Callers should prefer this API (or the more specific argsIngress) over direct access
   * to handler.ingresses.
   */
  def ingressOf[I <: org.goldenport.protocol.handler.ingress.Ingress[?]](
    input: Any
  ): Consequence[I] = input match {
    case a: Array[String] =>
      // Only ArgsIngress is supported for now
      handler.ingresses.ingress(a).asInstanceOf[Consequence[I]]
    // Future: Add cases for HttpRequest, etc.
    case _ =>
      Consequence.failure(new IllegalArgumentException(s"Unsupported ingress input: ${input.getClass.getName}"))
  }

  /**
   * Convenience method for Array[String] ingress.
   */
  def argsIngress(args: Array[String]): Consequence[ArgsIngress] =
    ingressOf[ArgsIngress](args)

  def egress[Out](
    kind: Egress.Kind[Out],
    res: Response
  ): Consequence[Out] =
    handler.egresses.egress(kind, res)

  def enproject[Out](kind: ProjectionKind[Out]): Consequence[Out] =
    handler.projections.project(kind, services)

  def enprojectByName(name: String): Consequence[Any] =
    handler.projections.projectByName(name, services)
}

object Protocol {
  import OperationDefinition.Builder.OperationFactory

  val empty = Protocol()

  def apply(p: Seq[ServiceDefinition]): Protocol =
    Protocol(ServiceDefinitionGroup(p))

  case class Builder(
    services: ServiceDefinitionGroup = ServiceDefinitionGroup.empty,
    handler: ProtocolHandler = ProtocolHandler.default,
    operationFactory: OperationFactory = OperationFactory.Default
  ) {
    def build(): Protocol = Protocol(services, handler)

    def addOperation(
      service: String,
      operation: String,
      req: RequestDefinition,
      res: ResponseDefinition
    ): Builder = {
      val op = operationFactory.createOperation(operation, req, res)
      copy(services = services.addOperation(service, op))
    }
  }
  object Builder {
  }
}
