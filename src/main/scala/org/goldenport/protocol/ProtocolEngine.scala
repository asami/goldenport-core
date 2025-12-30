package org.goldenport.protocol

import org.goldenport.Consequence
import org.goldenport.protocol.logic.ProtocolLogic
import org.goldenport.protocol.operation.OperationRequest
import org.goldenport.protocol.handler.projection.{CliHelp, McpGetManifestProjectionKind, OpenApi, ProjectionKind}
import io.circe.Json
/*
 * @since   Dec. 26, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
class ProtocolEngine(
  val protocol: Protocol,
  val logic: ProtocolLogic
) {
  def makeOperationRequest(args: Array[String]): Consequence[OperationRequest] =
    logic.makeOperationRequest(args)

  def openApi(): Consequence[Json] =
    protocol.project(OpenApi)

  def cliHelp(): Consequence[String] =
    protocol.project(CliHelp)

  def getManifest(): Consequence[Json] =
    protocol.project(McpGetManifestProjectionKind)

  def enproject[Out](kind: ProjectionKind[Out]): Consequence[Out] =
    protocol.project(kind)

  def enprojectByName(name: String): Consequence[Any] =
    protocol.projectByName(name)
}

object ProtocolEngine {
  def create(protocol: Protocol): ProtocolEngine =
    new ProtocolEngine(protocol, ProtocolLogic(protocol))
}
