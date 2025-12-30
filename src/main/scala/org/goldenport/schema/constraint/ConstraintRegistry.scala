package org.goldenport.schema.constraint

import org.goldenport.schema.Constraint

/*
 * @since   Dec. 30, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
object ConstraintRegistry {
  import CompileError._
  import PredicateExpr._

  private val _specs: Vector[PredicateSpec] = Vector(
    PredicateSpec(
      name = "notEmpty",
      aliases = Set.empty,
      kind = PredicateKind.NotEmpty,
      arity = 0,
      compile = _ => Right(Constraint.NotEmpty)
    ),
    PredicateSpec(
      name = "length",
      aliases = Set.empty,
      kind = PredicateKind.Length,
      arity = 2,
      compile = args => _compile_length(args)
    ),
    PredicateSpec(
      name = ">=",
      aliases = Set.empty,
      kind = PredicateKind.Ge,
      arity = 1,
      compile = args => _compile_numeric(args, Constraint.MinInclusive.apply)
    ),
    PredicateSpec(
      name = ">",
      aliases = Set.empty,
      kind = PredicateKind.Gt,
      arity = 1,
      compile = args => _compile_numeric(args, Constraint.MinExclusive.apply)
    ),
    PredicateSpec(
      name = "<=",
      aliases = Set.empty,
      kind = PredicateKind.Le,
      arity = 1,
      compile = args => _compile_numeric(args, Constraint.MaxInclusive.apply)
    ),
    PredicateSpec(
      name = "<",
      aliases = Set.empty,
      kind = PredicateKind.Lt,
      arity = 1,
      compile = args => _compile_numeric(args, Constraint.MaxExclusive.apply)
    ),
    PredicateSpec(
      name = "in",
      aliases = Set.empty,
      kind = PredicateKind.In,
      arity = 1,
      compile = args => _compile_enum(args)
    )
  )

  def lookup(kind: PredicateKind): Option[PredicateSpec] =
    _specs.find(_.kind == kind)

  def compile(
    exprs: Vector[PredicateExpr]
  ): Either[CompileError, Vector[Constraint]] = {
    val results = Vector.newBuilder[Constraint]
    exprs.foreach { expr =>
      compile(expr) match {
        case Left(err) => return Left(err)
        case Right(cs) => results ++= cs
      }
    }
    Right(results.result())
  }

  def compile(
    expr: PredicateExpr
  ): Either[CompileError, Vector[Constraint]] =
    expr match {
      case Atom(kind, args) =>
        lookup(kind) match {
          case None =>
            Left(UnknownPredicate(kind.toString))
          case Some(spec) =>
            if (args.size != spec.arity)
              Left(WrongArity(spec.name, spec.arity, args.size))
            else
              spec.compile(args).map(Vector(_))
        }
      case And(left, right) =>
        for {
          l <- compile(left)
          r <- compile(right)
        } yield l ++ r
      case Or(_, _) =>
        Left(NotSupported("Or"))
      case Not(_) =>
        Left(NotSupported("Not"))
    }

  private def _compile_length(
    args: Vector[Any]
  ): Either[CompileError, Constraint] =
    args match {
      case Vector(op: RelOp, n: BigInt) =>
        _to_int("length", n).map { v =>
          op match {
            case RelOp.Lt => Constraint.LengthLt(v)
            case RelOp.Le => Constraint.LengthLe(v)
            case RelOp.Gt => Constraint.LengthGt(v)
            case RelOp.Ge => Constraint.LengthGe(v)
          }
        }
      case _ =>
        Left(TypeMismatch("length", "expected (RelOp, Number)"))
    }

  private def _compile_numeric(
    args: Vector[Any],
    ctor: BigInt => Constraint
  ): Either[CompileError, Constraint] =
    args match {
      case Vector(n: BigInt) =>
        Right(ctor(n))
      case _ =>
        Left(TypeMismatch("numeric", "expected (Number)"))
    }

  private def _compile_enum(
    args: Vector[Any]
  ): Either[CompileError, Constraint] =
    args match {
      case Vector(values: Vector[?]) =>
        val rendered = values.collect { case s: String => s }
        if (rendered.size == values.size)
          Right(Constraint.Enum(rendered.toSet))
        else
          Left(TypeMismatch("in", "expected (EnumValues)"))
      case Vector(values: Set[?]) =>
        val rendered = values.collect { case s: String => s }
        if (rendered.size == values.size)
          Right(Constraint.Enum(rendered.toSet))
        else
          Left(TypeMismatch("in", "expected (EnumValues)"))
      case Vector(values: Seq[?]) =>
        val rendered = values.collect { case s: String => s }
        if (rendered.size == values.size)
          Right(Constraint.Enum(rendered.toSet))
        else
          Left(TypeMismatch("in", "expected (EnumValues)"))
      case _ =>
        Left(TypeMismatch("in", "expected (EnumValues)"))
    }

  private def _to_int(
    name: String,
    value: BigInt
  ): Either[CompileError, Int] =
    if (value.isValidInt)
      Right(value.toInt)
    else
      Left(TypeMismatch(name, "integer out of range"))
}
