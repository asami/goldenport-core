package org.goldenport.schema.constraint

import org.goldenport.schema.Constraint

/*
 * @since   Dec. 31, 2025
 * @version Dec. 31, 2025
 * @author  ASAMI, Tomoharu
 */
enum PredicateKind {
  case NotEmpty
  case Length
  case Lt
  case Le
  case Gt
  case Ge
  case In
}

final case class PredicateSpec(
  name: String,
  aliases: Set[String],
  kind: PredicateKind,
  arity: Int,
  compile: Vector[Any] => Either[CompileError, Constraint]
)

sealed trait CompileError {
  def message: String
}

object CompileError {
  final case class UnknownPredicate(name: String) extends CompileError {
    val message = s"unknown predicate: ${name}"
  }

  final case class WrongArity(
    name: String,
    expected: Int,
    actual: Int
  ) extends CompileError {
    val message = s"wrong arity for ${name}: expected ${expected}, actual ${actual}"
  }

  final case class TypeMismatch(
    name: String,
    detail: String
  ) extends CompileError {
    val message = s"type mismatch for ${name}: ${detail}"
  }

  final case class NotSupported(
    feature: String
  ) extends CompileError {
    val message = s"not supported: ${feature}"
  }
}

enum RelOp {
  case Lt
  case Le
  case Gt
  case Ge
}

sealed trait PredicateExpr

object PredicateExpr {
  final case class Atom(
    kind: PredicateKind,
    args: Vector[Any] = Vector.empty
  ) extends PredicateExpr

  final case class And(
    left: PredicateExpr,
    right: PredicateExpr
  ) extends PredicateExpr

  final case class Or(
    left: PredicateExpr,
    right: PredicateExpr
  ) extends PredicateExpr

  final case class Not(
    expr: PredicateExpr
  ) extends PredicateExpr
}
