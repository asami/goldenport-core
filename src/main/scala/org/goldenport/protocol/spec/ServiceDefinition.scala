package org.goldenport.protocol.spec

import cats.data.NonEmptyVector
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
 * @version Jan. 15, 2026
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

    def apply(
      name: String,
      operations: NonEmptyVector[OperationDefinition]
    ): Specification = Specification(
      content = BaseContent.simple(name),
      operations = OperationDefinitionGroup(operations)
    )

    def apply(
      name: String,
      op: OperationDefinition,
      ops: OperationDefinition*
    ): Specification = apply(name, NonEmptyVector(op, ops.toVector))

    def apply(
      name: String,
      opname: String,
      req: RequestDefinition,
      res: ResponseDefinition
    ): Specification = apply(
      name,
      OperationDefinition(opname, req, res)
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

  def apply(
    name: String,
    op: OperationDefinition,
    ops: OperationDefinition*
  ): Instance = Instance(Specification(name, NonEmptyVector(op, ops.toVector)))

  def apply(
    name: String,
    opname: String,
    req: RequestDefinition,
    res: ResponseDefinition
  ): Instance = Instance(Specification(name, opname, req, res))
}

case class ServiceDefinitionGroup(
  services: Vector[ServiceDefinition] = Vector.empty
) {
  def addOperation(name: String, op: OperationDefinition): ServiceDefinitionGroup = {
    val idx = services.indexWhere(_.name == name)
    if (idx >= 0) {
      val oldService = services(idx)
      val newOps = oldService.operations.add(op)
      val newService = ServiceDefinition.Instance(
        oldService.specification.copy(operations = newOps)
      )
      ServiceDefinitionGroup(services.updated(idx, newService))
    } else {
      val newService = ServiceDefinition.Instance(
        ServiceDefinition.Specification(name, OperationDefinitionGroup(op))
      )
      ServiceDefinitionGroup(services :+ newService)
    }
  }
}
object ServiceDefinitionGroup {
  import OperationDefinition.Builder.OperationFactory

  val empty = ServiceDefinitionGroup()

  def apply(ps: Seq[ServiceDefinition]): ServiceDefinitionGroup =
    ServiceDefinitionGroup(ps.toVector)

  def apply(p: ServiceDefinition, ps: ServiceDefinition*): ServiceDefinitionGroup = apply(p +: ps)

  case class Builder(
    operationFactory: OperationFactory = OperationFactory.Default,
    opsMap: Map[String, Vector[OperationDefinition]] = Map.empty
  ) {
    def build(
      services: ServiceDefinitionGroup = ServiceDefinitionGroup.empty,
      operationFactory: OperationFactory = OperationFactory.Default
    ): ServiceDefinitionGroup = {
      opsMap.foldLeft(services) { case (group, (name, ops)) =>
        val opDefs = ops match {
          case Vector() => Vector.empty
          case Vector(single) => Vector(single)
          case multiple => multiple
        }
        val opGroup =
          if (opDefs.isEmpty)
            None
          else
            Some(OperationDefinitionGroup(NonEmptyVector.fromVectorUnsafe(opDefs.toVector)))
        opGroup match {
          case Some(s) =>
            val newService = ServiceDefinition.Instance(
              ServiceDefinition.Specification(name, s)
            )
            val idx = group.services.indexWhere(_.name == name)
            if (idx >= 0)
              group.copy(services = group.services.updated(idx, newService))
            else
              group.copy(services = group.services :+ newService)
          case None => group
        }
      }
    }

    def addOperation(
      name: String,
      opname: String,
      req: RequestDefinition,
      res: ResponseDefinition
    ): Builder = {
      val op = OperationDefinition(opname, req, res)
      addOperation(name, op)
    }

    def addOperation(
      service: String,
      op: OperationDefinition
    ): Builder = {
      val currentOps = opsMap.getOrElse(service, Vector.empty)
      copy(opsMap = opsMap.updated(service, currentOps :+ op))
    }
  }
  object Builder {
  }
}

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
