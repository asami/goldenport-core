package org.goldenport.protocol.spec

import org.goldenport.model.value.{BaseContent, DescriptiveAttributes}
import org.goldenport.schema.*
import org.goldenport.schema.ValueDomain
import org.goldenport.util.{SmEnum, SmEnumClass}

/*
 * @since   Oct.  6, 2018
 *  version Oct.  8, 2018
 *  version Feb. 16, 2020
 *  version Jan. 30, 2023
 *  version Mar. 17, 2025
 *  version Apr.  2, 2025
 *  version Jun.  5, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
case class ParameterDefinition(
  content: BaseContent,
  kind: ParameterDefinition.Kind,
  domain: ValueDomain = ValueDomain(),
  default: ParameterDefinition.Default = ParameterDefinition.Default.Undefined,
  isMagicSequence: Boolean = true,
  isEagerSequence: Boolean = false
) extends BaseContent.BareHolder {
  protected def baseContent: BaseContent = content
  def datatype: DataType = domain.datatype
  def multiplicity: Multiplicity = domain.multiplicity
  def constraints: Vector[Constraint] = domain.constraints
  def aliases: Vector[String] =
    content.nameAttributes.alias
      .map(_.toVector.map(_.toI18nString.displayMessage))
      .getOrElse(Vector.empty)
  def names: Vector[String] = name +: aliases
}

object ParameterDefinition {
  enum Kind extends SmEnum {
    case Argument
    case Switch
    case Property
  }
  object Kind extends SmEnumClass[Kind] {
    protected def enum_Values = Kind.values
  }

  sealed trait Default
  object Default {
    case object Undefined extends Default
    case object Empty extends Default
    case class Value(value: Any) extends Default
  }

  @deprecated("Use BaseContent-based constructor", "2025-12-30")
  def apply(
    name: String,
    kind: ParameterDefinition.Kind
  ): ParameterDefinition =
    apply(
      name = name,
      kind = kind,
      domain = ValueDomain(),
      default = ParameterDefinition.Default.Undefined,
      isMagicSequence = true,
      isEagerSequence = false
    )

  @deprecated("Use BaseContent-based constructor", "2025-12-30")
  def apply(
    name: String,
    kind: ParameterDefinition.Kind,
    domain: ValueDomain
  ): ParameterDefinition =
    apply(
      name = name,
      kind = kind,
      domain = domain,
      default = ParameterDefinition.Default.Undefined,
      isMagicSequence = true,
      isEagerSequence = false
    )

  @deprecated("Use BaseContent-based constructor", "2025-12-30")
  def apply(
    name: String,
    kind: ParameterDefinition.Kind,
    domain: ValueDomain,
    default: ParameterDefinition.Default
  ): ParameterDefinition =
    apply(
      name = name,
      kind = kind,
      domain = domain,
      default = default,
      isMagicSequence = true,
      isEagerSequence = false
    )

  @deprecated("Use BaseContent-based constructor", "2025-12-30")
  def apply(
    name: String,
    kind: ParameterDefinition.Kind,
    domain: ValueDomain,
    default: ParameterDefinition.Default,
    isMagicSequence: Boolean,
    isEagerSequence: Boolean
  ): ParameterDefinition =
    ParameterDefinition(
      BaseContent.simple(name),
      kind = kind,
      domain = domain,
      default = default,
      isMagicSequence = isMagicSequence,
      isEagerSequence = isEagerSequence
    )
}
