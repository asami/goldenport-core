package org.goldenport.protocol

import cats.data.NonEmptyVector
import org.goldenport.Conclusion
import org.goldenport.Consequence
import org.goldenport.observation.Cause
import org.goldenport.observation.Descriptor
import org.goldenport.protocol.handler.ProtocolHandler
import org.goldenport.protocol.handler.egress.EgressCollection
import org.goldenport.protocol.handler.ingress.{ArgsIngress, IngressCollection}
import org.goldenport.protocol.handler.projection.{CliHelpProjection, McpGetManifestProjection, OpenApiProjection, ProjectionCollection}
import org.goldenport.protocol.spec._

/*
 * @since   Dec. 28, 2025
 *  version Apr. 11, 2026
 * @version Apr. 14, 2026
 * @author  ASAMI, Tomoharu
 */
object TestProtocol {
  def simple: Protocol = {
    val operation = OperationDefinition(
      content = org.goldenport.value.BaseContent.simple("query"),
      request = RequestDefinition(
        parameters = Nil
      ),
      response = ResponseDefinition.void
    )
    val service = ServiceDefinition(
      name = "default",
      operations = OperationDefinitionGroup(
        NonEmptyVector.of(operation)
      )
    )
    val services = ServiceDefinitionGroup(Vector(service))
    val handler = ProtocolHandler(
      IngressCollection(Vector(SimpleArgsIngress)),
      EgressCollection(Vector.empty),
      ProjectionCollection(Vector(new CliHelpProjection, new OpenApiProjection, new McpGetManifestProjection))
    )
    Protocol(services, handler)
  }

  def withArgsIngress: Protocol = {
    val operation = OperationDefinition(
      content = org.goldenport.value.BaseContent.simple("query"),
      request = RequestDefinition(
        parameters = Nil
      ),
      response = ResponseDefinition.void
    )
    val service = ServiceDefinition(
      name = "default",
      operations = OperationDefinitionGroup(
        NonEmptyVector.of(operation)
      )
    )
    val services = ServiceDefinitionGroup(Vector(service))
    val handler = ProtocolHandler(
      IngressCollection(Vector(DefaultArgsIngress)),
      EgressCollection(Vector.empty),
      ProjectionCollection(Vector(new CliHelpProjection, new OpenApiProjection, new McpGetManifestProjection))
    )
    Protocol(services, handler)
  }

  private object SimpleArgsIngress extends ArgsIngress {
    override def encode(
      services: ServiceDefinitionGroup,
      args: Array[String]
    ): Consequence[Request] = {
      val _ = services
      encode(args)
    }

    override def encode(args: Array[String]): Consequence[Request] =
      if (args.isEmpty) {
        // _failure("syntax error", Cause.SyntaxError)
        Consequence.argumentMissing
      } else {
        val operation = args.head
        if (operation != "query") {
          // _failure(
          //   "invalid operation",
          //   Cause.Argument(Cause.Reason.ValidationError),
          //   Vector(Descriptor.Aspect.Operation(operation))
          // )
          Consequence.operationInvalid(operation)
        } else if (args.length == 1) {
          // _failure(
          //   "missing argument",
          //   Cause.Argument(Cause.Reason.Missing),
          //   Vector(
          //     Descriptor.Aspect.Operation(operation),
          //     Descriptor.Aspect.Input(name = Some("target"), value = None)
          //   )
          // )
          Consequence.argumentMissingOperation("target", operation)
        } else if (args.length > 2) {
          // _failure(
          //   "redundant argument",
          //   Cause.Argument(Cause.Reason.Redundant),
          //   Vector(Descriptor.Aspect.Operation(operation))
          // )
          Consequence.argumentRedundantOperationInput(operation, args.tail.toIndexedSeq)
        } else {
          Consequence.success(
            Request(
              component = None,
              service = None,
              operation = operation,
              arguments = List(Argument("target", args(1), None)),
              switches = Nil,
              properties = Nil
            )
          )
        }
      }
  }

  private object DefaultArgsIngress extends ArgsIngress {
  }

  // private def _failure[A](
  //   message: String,
  //   cause: Cause,
  //   facets: Vector[Descriptor.Facet] = Vector.empty
  // ): Consequence[A] = {
  // val base = Conclusion.simple(message)
  // val observation = base.observation.copy(
  //   cause = Some(cause),
  //   descriptor = Descriptor(facets)
  // )
  // Consequence.Failure(base.copy(observation = observation))
  // }
}
