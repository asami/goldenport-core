package org.simplemodeling.schema

import cats.data.NonEmptyList
import cats.data.NonEmptyVector
import org.simplemodeling.model.value.BaseContent

/*
 * @since   Nov. 23, 2012
 *  version Dec. 28, 2012
 *  version Jan. 30, 2013
 *  version Mar. 12, 2013
 *  version Apr. 26, 2013
 *  version Jun. 24, 2013
 *  version Oct. 23, 2013
 *  version Feb.  6, 2014
 *  version Jun.  9, 2014
 *  version Jul. 25, 2014
 *  version Aug.  6, 2014
 *  version Sep. 25, 2015
 *  version Oct. 15, 2015
 *  version May. 26, 2016
 *  version Sep.  8, 2016
 *  version Jan. 21, 2017
 *  version May. 25, 2017
 *  version Aug.  1, 2017
 *  version Sep. 21, 2017
 *  version Oct. 25, 2017
 *  version Nov. 23, 2017
 *  version Dec. 13, 2017
 *  version Jan. 22, 2018
 *  version May. 16, 2018
 *  version Jul. 28, 2018
 *  version Aug. 29, 2018
 *  version Sep.  5, 2018
 *  version Jan.  1, 2019
 *  version Feb. 12, 2019
 *  version Apr. 29, 2019
 *  version May.  1, 2019
 *  version Aug. 20, 2019
 *  version Oct.  1, 2019
 *  version Dec. 30, 2019
 *  version Jan. 12, 2020
 *  version Apr. 17, 2020
 *  version Jun.  8, 2020
 *  version Mar. 17, 2021
 *  version Oct. 31, 2021 restart
 * @version Dec. 24, 2025 Scala3
 * @author  ASAMI, Tomoharu
 */
case class Schema(
  columns: Vector[Column]
) {
}

case class Column(
  baseContent: BaseContent,
  domain: ValueDomain
) extends BaseContent.Holder {
}

case class ValueDomain(
  datatype: DataType = XString,
  multiplicity: Multiplicity = Multiplicity.One,
  constraints: List[Constraint] = Nil
) {
}

sealed trait Multiplicity {
  def mark: String
  def label: String
}
object Multiplicity {
  case object One extends Multiplicity {
    val mark = "1"
    val label = "1"
  }
  case object ZeroOne extends Multiplicity {
    val mark = "?"
    val label = "0,1"
  }
  case object OneMore extends Multiplicity {
    val mark = "+"
    val label = "1..*"
  }
  case object ZeroMore extends Multiplicity {
    val mark = "*"
    val label = "0..*"
  }

  case class Range(from: Int, to: Int) extends Multiplicity {
    val mark = ".."
    val label = s"${from}..${to}"
  }

  case class Ranges(ranges: List[NonEmptyList[Range]]) extends Multiplicity {
    val mark = "..."
    val label = s"""${ranges.mkString("[", ",", "]")}"""
  }
}

/**
 * Maps semantic multiplicity to concrete collection representations.
 *
 * This policy controls how values validated by ValueDomain.multiplicity
 * are materialized into concrete container types.
 */
trait MultiplicityMappingPolicy {

  def one[T](value: T): T

  def zeroOne[T](value: Option[T]): Option[T]

  def oneMore[T](values: Vector[T]): NonEmptyVector[T]

  def zeroMore[T](values: Vector[T]): Vector[T]
}

object MultiplicityMappingPolicy {
  given default: MultiplicityMappingPolicy =
    DefaultMultiplicityMappingPolicy

  object DefaultMultiplicityMappingPolicy
      extends MultiplicityMappingPolicy {
    def one[T](value: T): T =
      value

    def zeroOne[T](value: Option[T]): Option[T] =
      value

    def oneMore[T](values: Vector[T]): NonEmptyVector[T] =
      NonEmptyVector.fromVectorUnsafe(values)

    def zeroMore[T](values: Vector[T]): Vector[T] =
      values
  }
}

sealed trait Constraint {
}
