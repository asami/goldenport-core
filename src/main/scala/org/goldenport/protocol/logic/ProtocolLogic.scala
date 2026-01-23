package org.goldenport.protocol.logic

import org.goldenport.Consequence
import org.goldenport.protocol.Protocol
import org.goldenport.protocol.Property
import org.goldenport.protocol.Request
import org.goldenport.protocol.Response
import org.goldenport.protocol.operation.{OperationRequest, OperationResponse}
import org.goldenport.protocol.spec.{OperationDefinition, ServiceDefinition}
import org.goldenport.protocol.handler.egress.Egress

/*
 * @since   Dec. 24, 2025
 *  version Dec. 28, 2025
 *  version Jan.  2, 2026
 * @version Jan.  3, 2026
 * @author  ASAMI, Tomoharu
 */
class ProtocolLogic(protocol: Protocol) {
  def makeOperationRequest(args: Array[String]): Consequence[OperationRequest] =
    for {
      req <- makeRequest(args)
      opreq <- makeOperationRequest(req)
    } yield opreq

  /** CLI parsing always wins; injected properties fill gaps before operation resolution.
    */
  def makeOperationRequest(
    args: Array[String],
    initialProperties: Map[String, String]
  ): Consequence[OperationRequest] = {
    if (initialProperties.isEmpty) {
      makeOperationRequest(args)
    } else {
      for {
        req <- makeRequest(args)
        enhanced = _with_initial_properties(req, initialProperties)
        opreq <- makeOperationRequest(enhanced)
      } yield opreq
    }
  }

  def makeOperationRequest(request: Request): Consequence[OperationRequest] = {
    _resolve_service(request) match {
      case Consequence.Success(service) =>
        _resolve_operation(service, request) match {
          case Consequence.Success(op) =>
            op.createOperationRequest(request)
          case Consequence.Failure(c) =>
            Consequence.Failure(c)
        }
      case Consequence.Failure(c) =>
        Consequence.Failure(c)
    }
  }

  private def _resolve_service(req: Request): Consequence[ServiceDefinition] =
    req.service match {
      case Some(name) =>
        protocol.services.services.find(_.name == name) match {
          case Some(service) => Consequence.success(service)
          case None => Consequence.failure("service not found")
        }
      case None =>
        protocol.services.services.find(_.operations.operations.exists(_.name == req.operation)) match {
          case Some(service) => Consequence.success(service)
          case None => Consequence.failure("operation not found")
        }
    }

  private def _resolve_operation(
    service: ServiceDefinition,
    req: Request
  ): Consequence[OperationDefinition] =
    service.operations.operations.find(_.name == req.operation) match {
      case Some(op) => Consequence.success(op)
      case None =>
        Consequence
          .failArgumentValidationError
          .withOperation(req.operation)
          .build
    }

  def makeRequest(args: Array[String]): Consequence[Request] = {
    for {
      in <- protocol.argsIngress(args)
      req <- in.encode(protocol.services, args)
    } yield req
  }

  def makeStringOperationResponse(res: OperationResponse): Consequence[String] = {
    makeStringResponse(res.toResponse)
  }

  def makeStringResponse(res: Response): Consequence[String] = {
    protocol.egress(Egress.Kind.`String`, res)
  }

  // CLI-provided names win; injected entries only cover missing names before defaults run.
  private def _with_initial_properties(
    req: Request,
    initialProperties: Map[String, String]
  ): Request = {
    val existing = req.properties.map(_.name).toSet
    val injected =
      initialProperties.iterator.collect {
        case (name, value) if !existing.contains(name) =>
          Property(name, value, None)
      }.toList
    if (injected.isEmpty) req
    else req.copy(properties = req.properties ++ injected)
  }
}

object ProtocolLogic {
  def apply(protocol: org.goldenport.protocol.Protocol): ProtocolLogic =
    new ProtocolLogic(protocol)
}
