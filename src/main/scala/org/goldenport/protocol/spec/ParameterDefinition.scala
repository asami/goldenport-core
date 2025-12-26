package org.goldenport.protocol.spec

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
 * @version Dec. 26, 2025
 * @author  ASAMI, Tomoharu
 */
case class ParameterDefinition(
  name: String,
  kind: ParameterDefinition.Kind,
  domain: ValueDomain = ValueDomain(),
  defaut: ParameterDefinition.Default = ParameterDefinition.Default.Undefined,
  isMagicSequence: Boolean = true,
  isEagerSequence: Boolean = false
) {
  def datatype: DataType = domain.datatype
  def multiplicity: Multiplicity = domain.multiplicity
  def constraints: List[Constraint] = domain.constraints
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

  enum Default extends SmEnum {
    case Undefined
    case Empty
  }
  object Default extends SmEnumClass[Default] {
    protected def enum_Values = Default.values
  }
}
