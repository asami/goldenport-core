package org.goldenport.protocol.spec

import org.goldenport.model.value.BaseContent
import org.goldenport.protocol.service.Service

/*
 * @since   Oct.  6, 2018
 *  version Oct.  8, 2018
 *  version Feb. 24, 2019
 *  version Feb. 13, 2020
 *  version Nov. 25, 2023
 *  version Feb.  2, 2025
 *  version Dec. 30, 2025
 * @version Jan.  3, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class ServiceDefinition
    extends ServiceDefinition.Specification.Holder {
  def specification: ServiceDefinition.Specification

  def defaultOperation: Option[OperationDefinition] =
    if (useDefault) Some(operations.head) else None

  def createService[T <: Service](factory: ServiceDefinition.Factory[T]): T = {
    val core = Service.Core(this)
    factory.create(core)
  }
}

object ServiceDefinition {
  case class Specification(
    content: BaseContent,
    operations: OperationDefinitionGroup,
    useDefault: Boolean = false,
    metadata: ServiceMetadata = ServiceMetadata.empty
  ) extends BaseContent.BareHolder {
    protected def baseContent: BaseContent = content
  }
  object Specification {
    trait Holder extends BaseContent.BareHolder {
      def specification: Specification

      protected def baseContent: BaseContent = specification.content
      def operations: OperationDefinitionGroup = specification.operations
      def useDefault: Boolean = specification.useDefault
      def metadata: ServiceMetadata = specification.metadata
    }

    def apply(
      name: String,
      operations: OperationDefinitionGroup
    ): Specification = Specification(
      content = BaseContent.simple(name),
      operations = operations
    )
  }

  trait Factory[T <: Service] {
    def create(core: Service.Core): T
  }

  final case class Instance(specification: Specification) extends ServiceDefinition {
  }

  def apply(
    name: String,
    operations: OperationDefinitionGroup
  ): Instance = Instance(Specification(name, operations))
}

case class ServiceDefinitionGroup(
  services: Vector[ServiceDefinition]
)

case class ServiceMetadata(
  description: Option[String] = None,
  descriptionLong: Option[String] = None,
  author: Option[String] = None,
  homepage: Option[java.net.URI] = None,
  license: Option[String] = None,
  keywords: Vector[String] = Vector.empty
)

object ServiceMetadata {
  val empty: ServiceMetadata = ServiceMetadata()
}
