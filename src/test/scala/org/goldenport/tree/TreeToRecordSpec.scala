package org.goldenport.tree

import org.goldenport.record.Record
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.GivenWhenThen
import org.goldenport.datatype.PathName

/*
 * This spec defines how a structured Tree
 * is relaxed into a semi-structured Record.
 *
 * This is a lossy projection by design.
 */
class TreeToRecordSpec
  extends AnyWordSpec
    with Matchers
    with GivenWhenThen {

  "Tree to Record conversion" should {

    "convert a single leaf tree into a record with a terminal value" in {
      Given("a Tree consisting of a single leaf")
      val tree =
        Tree(
          TreeDir(
            Vector(
              TreeEntry("value", TreeLeaf("value"))
            )
          )
        )

      When("the tree is converted to Record")
      val record =
        Tree.toRecord(tree)

      Then("the record contains the leaf value")
      record.getString("value") shouldBe Some("value")
    }

    "convert a flat directory tree into a record with corresponding fields" in {
      Given("a Tree with multiple leaf children")
      val tree =
        Tree(
          TreeDir(
            Vector(
              TreeEntry("a", TreeLeaf("A")),
              TreeEntry("b", TreeLeaf("B"))
            )
          )
        )

      When("the tree is converted to Record")
      val record =
        Tree.toRecord(tree)

      Then("each leaf becomes a record field")
      record.getString("a") shouldBe Some("A")
      record.getString("b") shouldBe Some("B")
    }

    "convert a nested tree into nested records" in {
      Given("a Tree with nested directory structure")
      val tree =
        Tree(
          TreeDir(
            Vector(
              TreeEntry(
                "a",
                TreeDir(
                  Vector(
                    TreeEntry("b", TreeLeaf("B"))
                  )
                )
              )
            )
          )
        )

      When("the tree is converted to Record")
      val record =
        Tree.toRecord(tree)

      Then("nested structure is preserved as nested records")
      record.getString(PathName.parse("a/b")) shouldBe Some("B")
    }

    "preserve sibling order semantically but not structurally" in {
      Given("a Tree with ordered children")
      val tree =
        Tree(
          TreeDir(
            Vector(
              TreeEntry("first", TreeLeaf(1)),
              TreeEntry("second", TreeLeaf(2))
            )
          )
        )

      When("the tree is converted to Record")
      val record =
        Tree.toRecord(tree)

      Then("the record contains both values without relying on order")
      record.getAs[Int]("first") shouldBe Some(1)
      record.getAs[Int]("second") shouldBe Some(2)
    }

    "allow lossy conversion without structural guarantees" in {
      Given("a Tree with complex structure")
      val tree =
        Tree(
          TreeDir(
            Vector(
              TreeEntry("a", TreeLeaf("A")),
              TreeEntry(
                "b",
                TreeDir(
                  Vector(
                    TreeEntry("c", TreeLeaf("C"))
                  )
                )
              )
            )
          )
        )

      When("the tree is converted to Record")
      val record =
        Tree.toRecord(tree)

      Then("the record is a semi-structured representation")
      record.getString("a") shouldBe Some("A")
      record.getString(PathName.parse("b/c")) shouldBe Some("C")
    }
  }
}
