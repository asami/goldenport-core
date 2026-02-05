package org.goldenport.datatype

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.GivenWhenThen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.Gen

/*
 * @since   Feb.  5, 2026
 * @version Feb.  5, 2026
 * @author  ASAMI, Tomoharu
 */
class PathNameSpec
  extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckPropertyChecks {

  "PathName" should {

    "represent a normalized sequence of name segments" in {
      Given("a raw path string with redundant separators and blanks")
      val raw = "a//b///c/"

      When("the path is parsed")
      val path = PathName.parse(raw)

      Then("empty segments are removed")
      path.segments shouldBe Vector("a", "b", "c")
    }

    "treat the root path as an empty segment sequence" in {
      Given("an empty path string")

      When("the path is parsed")
      val path = PathName.parse("")

      Then("the path represents root")
      path.isRoot shouldBe true
      path.segments shouldBe Vector.empty
    }

    "support structural composition using the slash operator" in {
      Given("the root PathName")

      When("child segments are appended")
      val path =
        PathName.Root / "a" / "b" / "c"

      Then("the resulting path contains all segments in order")
      path.segments shouldBe Vector("a", "b", "c")
    }

    "define parent-child relationships structurally" in {
      Given("a nested PathName")
      val path =
        PathName.parse("a/b/c")

      When("the parent is requested")
      val parent = path.parent

      Then("the parent path removes the last segment")
      parent shouldBe Some(PathName.parse("a/b"))
    }

    "have no parent for the root path" in {
      Given("the root PathName")

      When("the parent is requested")
      val parent = PathName.Root.parent

      Then("no parent exists")
      parent shouldBe None
    }

    "not interpret dot segments semantically" in {
      Given("a path containing dot segments")
      val raw = "a/./b/./c"

      When("the path is parsed")
      val path = PathName.parse(raw)

      Then("dot segments are removed without semantic interpretation")
      path.segments shouldBe Vector("a", "b", "c")
    }

    "treat dot-dot segments as ordinary names" in {
      Given("a path containing dot-dot segments")
      val raw = "a/../b"

      When("the path is parsed")
      val path = PathName.parse(raw)

      Then("dot-dot is preserved as a literal segment")
      path.segments shouldBe Vector("a", "..", "b")
    }

    "define equality purely by segment sequence" in {
      forAll { (segments: Vector[String]) =>
        Given("two PathNames constructed from the same segments")

        When("both paths are created")
        val p1 = PathName(segments)
        val p2 = PathName(segments)

        Then("they are equal")
        p1 shouldBe p2
      }
    }

    "preserve segment order as semantic order" in {
      val validSegment = Gen.alphaNumStr.suchThat(s => s.nonEmpty && s != ".")

      forAll(validSegment, validSegment, validSegment) { (a, b, c) =>
        Given("three arbitrary segments")

        When("a PathName is constructed in sequence")
        val path =
          PathName.Root / a / b / c

        Then("the segment order is preserved")
        path.segments shouldBe Vector(a, b, c)
      }
    }

    "render to string using a configurable separator" in {
      Given("a PathName with multiple segments")
      val path =
        PathName.parse("a/b/c")

      When("rendered as a string")
      val rendered = path.asString('/')

      Then("segments are joined using the separator")
      rendered shouldBe "a/b/c"
    }
  }
}
