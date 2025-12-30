package org.goldenport.schema.constraint

import org.goldenport.schema.Constraint
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Dec. 31, 2025
 * @version Dec. 31, 2025
 * @author  ASAMI, Tomoharu
 */
class PredicateExprSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks {

  import PredicateExpr._
  import RelOp.Lt

  private def compile_atom(atom: Atom): Constraint =
    ConstraintRegistry.compile(atom).getOrElse(Vector.empty).head

  private def flatten_and(expr: PredicateExpr): Vector[Atom] =
    expr match {
      case a: Atom => Vector(a)
      case And(l, r) => flatten_and(l) ++ flatten_and(r)
      case _ => Vector.empty
    }

  "PredicateExpr" should {
    "flatten AND into ordered constraints" in {
      Given("a conjunction of predicates")
      val left = Atom(PredicateKind.NotEmpty)
      val right = Atom(PredicateKind.Length, Vector(Lt, BigInt(100)))
      val expr = PredicateExpr.And(left, right)

      When("compiling the expression")
      val result = ConstraintRegistry.compile(expr)

      Then("it compiles into ordered constraints")
      result.shouldBe(
        Right(Vector(Constraint.NotEmpty, Constraint.LengthLt(100)))
      )
    }

    "reject OR as not supported in Phase 1" in {
      Given("a disjunction of predicates")
      val expr = Or(
        Atom(PredicateKind.NotEmpty),
        Atom(PredicateKind.Length, Vector(Lt, BigInt(3)))
      )

      When("compiling the expression")
      val result = ConstraintRegistry.compile(expr)

      Then("it returns NotSupported")
      result.left.toOption.collect {
        case CompileError.NotSupported(feature) => feature
      }.shouldBe(Some("Or"))
    }

    "flatten AND chains property-wise" in {
      Given("a non-empty list of atoms")
      val atomGen =
        Gen.oneOf(
          Gen.const(Atom(PredicateKind.NotEmpty)),
          Gen.posNum[Int].map(n => Atom(PredicateKind.Length, Vector(Lt, BigInt(n))))
        )

      forAll(Gen.nonEmptyListOf(atomGen)) { atoms =>
        When("building a left-associated AND chain")
        val expr = atoms.tail.foldLeft(atoms.head: PredicateExpr) { (acc, atom) =>
          PredicateExpr.And(acc, atom)
        }

        Then("compilation preserves left-to-right order")
        val compiled = ConstraintRegistry.compile(expr).getOrElse(Vector.empty)
        val expected = flatten_and(expr).map(compile_atom)
        compiled.shouldBe(expected)
      }
    }
  }
}
