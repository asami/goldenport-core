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
class ConstraintRegistrySpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks {

  import PredicateExpr._
  import RelOp.Lt

  "ConstraintRegistry" should {
    "compile notEmpty into Constraint.NotEmpty" in {
      Given("a notEmpty predicate expression")
      val expr = Atom(PredicateKind.NotEmpty)
      When("compiling the expression")
      val result = ConstraintRegistry.compile(expr)
      Then("it compiles to Constraint.NotEmpty")
      result.shouldBe(Right(Vector(Constraint.NotEmpty)))
    }

    "compile length < n into Constraint.LengthLt" in {
      Given("a length < n predicate expression")
      forAll(Gen.posNum[Int]) { n =>
        When("compiling the expression")
        val expr = Atom(PredicateKind.Length, Vector(Lt, BigInt(n)))
        val result = ConstraintRegistry.compile(expr)
        Then("it compiles to Constraint.LengthLt")
        result.shouldBe(Right(Vector(Constraint.LengthLt(n))))
      }
    }

    "compile >= n into Constraint.MinInclusive" in {
      Given("a numeric >= n predicate expression")
      forAll(Gen.chooseNum(-1000L, 1000L)) { n =>
        When("compiling the expression")
        val expr = Atom(PredicateKind.Ge, Vector(BigInt(n)))
        val result = ConstraintRegistry.compile(expr)
        Then("it compiles to Constraint.MinInclusive")
        result.shouldBe(Right(Vector(Constraint.MinInclusive(BigInt(n)))))
      }
    }

    "compile enum in {v1,v2} into Constraint.Enum" in {
      Given("an enum predicate expression")
      val expr = Atom(PredicateKind.In, Vector(Vector("a", "b")))
      When("compiling the expression")
      val result = ConstraintRegistry.compile(expr)
      Then("it compiles to Constraint.Enum")
      result.shouldBe(Right(Vector(Constraint.Enum(Set("a", "b")))))
    }

    "validate compiled constraints with OCL semantics" in {
      Given("a compiled numeric constraint")
      val expr = Atom(PredicateKind.Ge, Vector(BigInt(0)))
      val constraints = ConstraintRegistry.compile(expr).getOrElse(Vector.empty)
      When("validating a violating value")
      val invalid = constraints.exists(_.validate(BigInt(-1)).isLeft)
      Then("validation reports failure")
      invalid.shouldBe(true)
    }
  }
}
