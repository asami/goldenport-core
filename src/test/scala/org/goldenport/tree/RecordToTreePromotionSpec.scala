package org.goldenport.tree

import org.goldenport.Consequence
import org.goldenport.record.Record
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.GivenWhenThen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/*
 * @since   Feb.  5, 2026
 * @version Feb.  5, 2026
 * @author  ASAMI, Tomoharu
 */
class RecordToTreePromotionSpec
  extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckPropertyChecks {

  private def makeRecord(xs: (String, Any)*): Record =
    Record.create(xs.toVector)

  private def assertSuccess[T](result: Consequence[T]): Unit =
    result match {
      case Consequence.Success(_) => ()
      case other => fail(s"expected success but got $other")
    }

  private def assertFailure[T](result: Consequence[T]): Unit =
    result match {
      case Consequence.Failure(_) => ()
      case other => fail(s"expected failure but got $other")
    }

  /*
   * This spec defines when a semi-structured Record
   * MAY be promoted to a structured Tree.
   *
   * It does not prescribe how the conversion is implemented.
   */

  "Record to Tree promotion" should {

    "allow promotion when the record is recursively tree-shaped" in {
      Given("a record whose values are either records or leaf values")
      val record =
        makeRecord(
          "a" -> makeRecord(
            "b" -> makeRecord(
              "c" -> "C"
            )
          )
        )

      When("the record is promoted to Tree")
      val result =
        Tree.fromRecord[String](record)

      Then("the promotion succeeds")
      assertSuccess(result)
    }

    "reject promotion when non-record values appear at intermediate nodes" in {
      Given("a record with a non-record value at an intermediate node")
      val record =
        makeRecord(
          "a" -> "A",
          "a/b" -> "B"
        )

      When("the record is promoted to Tree")
      val result =
        Tree.fromRecord[String](record)

      Then("the promotion fails")
      assertFailure(result)
    }

    "reject promotion when sibling nodes mix leaf and record semantics" in {
      Given("a record where a node acts as both leaf and directory")
      val record =
        makeRecord(
          "a" -> "A",
          "a" -> makeRecord(
            "b" -> "B"
          )
        )

      When("the record is promoted to Tree")
      val result =
        Tree.fromRecord[String](record)

      Then("the promotion fails")
      assertFailure(result)
    }

    "allow promotion when all leaves are terminal values" in {
      forAll { (a: String, b: String) =>
        whenever(a.nonEmpty && b.nonEmpty) {
          Given("a record with only terminal leaf values")
          val record =
            makeRecord(
              "x" -> a,
              "y" -> b
            )

          When("the record is promoted to Tree")
          val result =
            Tree.fromRecord[String](record)

          Then("the promotion succeeds")
          assertSuccess(result)
        }
      }
    }

    "reject promotion when record structure is ambiguous" in {
      Given("a record with structurally ambiguous nesting")
      val record =
        makeRecord(
          "a" -> makeRecord(
            "b" -> "B"
          ),
          "a/b/c" -> "C"
        )

      When("the record is promoted to Tree")
      val result =
        Tree.fromRecord[String](record)

      Then("the promotion fails")
      assertFailure(result)
    }
  }
}
