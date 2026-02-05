package org.goldenport.tree

import org.goldenport.datatype.PathName
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.GivenWhenThen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/*
 * @since   Feb.  5, 2026
 * @version Feb.  5, 2026
 * @author  ASAMI, Tomoharu
 */
class TreeSpec
  extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckPropertyChecks {

  private def emptyTree[A]: Tree[A] =
    Tree(TreeDir(Vector.empty))

  // ------------------------------------------------------------
  // Semantic invariants
  // ------------------------------------------------------------

  "Tree" should {

    "represent a partial function from PathName to values" in {
      Given("an empty Tree")
      val tree = emptyTree[String]

      When("a value is put at a path")
      val updated = tree.put(PathName.parse("a/b/c"), "C")

      Then("the value is defined exactly at that path")
      updated.getValue(PathName.parse("a/b/c")) shouldBe Some("C")

      And("no value is defined at intermediate paths")
      updated.getValue(PathName.parse("a/b")) shouldBe None
      updated.getValue(PathName.parse("a")) shouldBe None
    }

    "preserve insertion order as semantic order" in {
      Given("an empty Tree")

      When("values are inserted in sequence")
      val tree =
        emptyTree[Int]
          .put(PathName.parse("a"), 1)
          .put(PathName.parse("b"), 2)
          .put(PathName.parse("c"), 3)

      Then("the root children preserve insertion order")
      tree.root.children.map(_.name) shouldBe Vector("a", "b", "c")
    }

    "preserve order when overwriting existing entries" in {
      Given("a Tree with multiple entries")
      val tree =
        emptyTree[String]
          .put(PathName.parse("a"), "A")
          .put(PathName.parse("b"), "B")

      When("an existing path is overwritten")
      val updated =
        tree.put(PathName.parse("a"), "A2")

      Then("the original order is preserved")
      updated.root.children.map(_.name) shouldBe Vector("a", "b")

      And("the value is replaced")
      updated.getValue(PathName.parse("a")) shouldBe Some("A2")
    }

    "promote a leaf node to a directory when required by deeper insertion" in {
      Given("a Tree with a leaf at path 'a'")
      val tree =
        emptyTree[String]
          .put(PathName.parse("a"), "A")

      When("a deeper path under that leaf is inserted")
      val updated =
        tree.put(PathName.parse("a/b"), "B")

      Then("the original leaf is no longer a value node")
      updated.getValue(PathName.parse("a")) shouldBe None

      And("the deeper value is accessible")
      updated.getValue(PathName.parse("a/b")) shouldBe Some("B")

      And("the node at 'a' is now a directory")
      updated.get(PathName.parse("a")) shouldBe defined
    }

    // ------------------------------------------------------------
    // Property-based semantics
    // ------------------------------------------------------------

    "satisfy last-write-wins semantics for identical paths" in {
      forAll { (v1: Int, v2: Int) =>
        Given("an empty Tree")

        When("the same path is written twice")
        val tree =
          emptyTree[Int]
            .put(PathName.parse("x"), v1)
            .put(PathName.parse("x"), v2)

        Then("the final value is the last written value")
        tree.getValue(PathName.parse("x")) shouldBe Some(v2)
      }
    }

    "be closed under map while preserving structure and order" in {
      forAll { (a: Int, b: Int) =>
        Given("a Tree with two values")
        val tree =
          emptyTree[Int]
            .put(PathName.parse("a"), a)
            .put(PathName.parse("b"), b)

        When("the Tree is mapped")
        val mapped =
          tree.map(_.toString)

        Then("values are transformed")
        mapped.getValue(PathName.parse("a")) shouldBe Some(a.toString)
        mapped.getValue(PathName.parse("b")) shouldBe Some(b.toString)

        And("structure and order are preserved")
        mapped.root.children.map(_.name) shouldBe Vector("a", "b")
      }
    }

    "support total traversal via walk with path-node consistency" in {
      Given("a Tree with multiple leaves")
      val tree =
        emptyTree[String]
          .put(PathName.parse("a/b"), "B")
          .put(PathName.parse("a/c"), "C")

      When("the Tree is traversed")
      val visited =
        scala.collection.mutable.Buffer.empty[(PathName, String)]

      tree.walk { (path, node) =>
        node match {
          case TreeLeaf(v, _) =>
            visited.append(path -> v)
          case _ =>
            ()
        }
      }

      Then("each leaf is visited exactly once with its path")
      visited.toSet shouldBe Set(
        PathName.parse("a/b") -> "B",
        PathName.parse("a/c") -> "C"
      )
    }
  }
}
