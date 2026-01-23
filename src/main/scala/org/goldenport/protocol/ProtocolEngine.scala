package org.goldenport.protocol

import org.goldenport.Consequence
import org.goldenport.protocol.logic.ProtocolLogic
import org.goldenport.protocol.operation.OperationRequest
import org.goldenport.protocol.handler.projection.{CliHelp, McpGetManifestProjectionKind, OpenApi, ProjectionKind}
import io.circe.Json
/*
 * @since   Dec. 26, 2025
 *  version Dec. 30, 2025
 * @version Jan.  1, 2026
 * @author  ASAMI, Tomoharu
 */
class ProtocolEngine(
  val protocol: Protocol,
  val logic: ProtocolLogic
) {
  def makeOperationRequest(args: Array[String]): Consequence[OperationRequest] =
    logic.makeOperationRequest(args)

  /** Allows injecting runtime configuration before the CLI values are evaluated.
    */
  def makeOperationRequest(
    args: Array[String],
    initialProperties: Map[String, String]
  ): Consequence[OperationRequest] =
    logic.makeOperationRequest(args, initialProperties)

  def openApi(): Consequence[Json] =
    protocol.enproject(OpenApi)

  def cliHelp(): Consequence[String] =
    protocol.enproject(CliHelp)

  def getManifest(): Consequence[Json] =
    protocol.enproject(McpGetManifestProjectionKind)

  def enproject[Out](kind: ProjectionKind[Out]): Consequence[Out] =
    protocol.enproject(kind)

  def enprojectByName(name: String): Consequence[Any] =
    protocol.enprojectByName(name)
}

object ProtocolEngine {
  def create(protocol: Protocol): ProtocolEngine =
    new ProtocolEngine(protocol, ProtocolLogic(protocol))
}
