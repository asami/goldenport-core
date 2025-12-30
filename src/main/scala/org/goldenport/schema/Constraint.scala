package org.goldenport.schema

/*
 * @since   Dec. 30, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
sealed trait Constraint {
  def validate(value: Any): Either[ValueDomainError, Unit]
}

sealed trait ValueDomainError
case object ValueDomainError extends ValueDomainError

object Constraint {
  final case class Enum(values: Set[String]) extends Constraint {
    def validate(value: Any): Either[ValueDomainError, Unit] = {
      val rendered = value match {
        case s: String => s
        case other => other.toString
      }
      if (values.contains(rendered)) Right(())
      else Left(ValueDomainError)
    }
  }

  case object NotEmpty extends Constraint {
    def validate(value: Any): Either[ValueDomainError, Unit] =
      value match {
        case s: String if s.nonEmpty => Right(())
        case _ => Left(ValueDomainError)
      }
  }

  final case class LengthLt(value: Int) extends Constraint {
    def validate(input: Any): Either[ValueDomainError, Unit] =
      input match {
        case s: String if s.length < value => Right(())
        case _ => Left(ValueDomainError)
      }
  }

  final case class LengthLe(value: Int) extends Constraint {
    def validate(input: Any): Either[ValueDomainError, Unit] =
      input match {
        case s: String if s.length <= value => Right(())
        case _ => Left(ValueDomainError)
      }
  }

  final case class LengthGt(value: Int) extends Constraint {
    def validate(input: Any): Either[ValueDomainError, Unit] =
      input match {
        case s: String if s.length > value => Right(())
        case _ => Left(ValueDomainError)
      }
  }

  final case class LengthGe(value: Int) extends Constraint {
    def validate(input: Any): Either[ValueDomainError, Unit] =
      input match {
        case s: String if s.length >= value => Right(())
        case _ => Left(ValueDomainError)
      }
  }

  final case class MinInclusive(value: BigInt) extends Constraint {
    def validate(input: Any): Either[ValueDomainError, Unit] =
      input match {
        case v: BigInt if v >= value => Right(())
        case _ => Left(ValueDomainError)
      }
  }

  final case class MinExclusive(value: BigInt) extends Constraint {
    def validate(input: Any): Either[ValueDomainError, Unit] =
      input match {
        case v: BigInt if v > value => Right(())
        case _ => Left(ValueDomainError)
      }
  }

  final case class MaxInclusive(value: BigInt) extends Constraint {
    def validate(input: Any): Either[ValueDomainError, Unit] =
      input match {
        case v: BigInt if v <= value => Right(())
        case _ => Left(ValueDomainError)
      }
  }

  final case class MaxExclusive(value: BigInt) extends Constraint {
    def validate(input: Any): Either[ValueDomainError, Unit] =
      input match {
        case v: BigInt if v < value => Right(())
        case _ => Left(ValueDomainError)
      }
  }
}
