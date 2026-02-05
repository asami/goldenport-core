package org.goldenport.tree

import org.goldenport.datatype.PathName
import org.goldenport.record.Record
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.GivenWhenThen
import org.goldenport.test.matchers.ConsequenceMatchers

/*
 * @since   Feb.  5, 2026
 * @version Feb.  5, 2026
 * @author  ASAMI, Tomoharu
 */
class RecordTreeRecordRoundtripSpec
  extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ConsequenceMatchers {
    /*
     * This spec asserts that Record -> Tree -> Record
     * is NOT an identity transformation.
     *
     * Loss of information and shape is intentional.
     */

  "Record -> Tree -> Record roundtrip" should {

    "not preserve record identity even when promotion succeeds" in {
      Given("a semi-structured record with nested content")
      val original =
          Record.data(
            "a" -> Record.data("b" -> "B")
          )

      When("the record is promoted to Tree and relaxed back to Record")
      val promoted = Tree.fromRecord[String](original)

      promoted should be_success
      val roundtripped = Tree.toRecord(promoted.take)

      Then("the resulting record is not identical to the original")
      roundtripped should not be theSameInstanceAs(original)
    }

    "lose shape information that is not representable in Tree" in {
      Given("a record with semi-structured ambiguity")
      val original =
        Record.data(
          "a" -> "A",
          "a" -> Record.data("b" -> "B")
        )

      When("promotion is attempted")
      val promoted = Tree.fromRecord[String](original)
      Then("promotion fails rather than preserving ambiguity")
      promoted should be_failure
    }

    "normalize record structure through Tree semantics" in {
      Given("a record with redundant nesting")
      val original =
        Record.data(
          "root" -> Record.data(
            "child" -> Record.data(
              "leaf" -> "L"
            )
          )
        )

      When("the record is promoted and relaxed")
      val promoted = Tree.fromRecord[String](original)

      promoted should be_success
      val relaxed = Tree.toRecord(promoted.take)
      Then("the resulting record reflects Tree semantics, not original shape")
      relaxed should not equal original
      relaxed.getString(PathName.parse("root/child/leaf")) shouldBe Some("L")
    }
  }
}
