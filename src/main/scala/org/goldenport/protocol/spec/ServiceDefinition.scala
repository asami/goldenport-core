package org.goldenport.protocol.spec

/*
 * @since   Oct.  6, 2018
 *  version Oct.  8, 2018
 *  version Feb. 24, 2019
 *  version Feb. 13, 2020
 *  version Nov. 25, 2023
 *  version Feb.  2, 2025
 * @version Dec. 25, 2025
 * @author  ASAMI, Tomoharu
 */
case class ServiceDefinition(
  name: String,
  operations: OperationDefinitionGroup,
  useDefault: Boolean = false
) {
  def defaultOperation: Option[OperationDefinition] =
    if (useDefault) Some(operations.head) else None
}

case class ServiceDefinitionGroup(
  services: Vector[ServiceDefinition]
)
