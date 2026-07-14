package org.goldenport.datatype

import org.goldenport.Consequence
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Dec. 22, 2025
 * @version Jul. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class TextSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Text" should {
    "accept values at both current runtime length boundaries" in {
      Given("an empty text and a text at the maximum length")
      val empty = ""
      val maximum = "t" * Text.LENGTH_MAX

      When("the texts are parsed")
      val emptyresult = Text.parse(empty)
      val maximumresult = Text.parse(maximum)

      Then("both boundary values retain their exact representation")
      emptyresult.map(_.value) shouldBe Consequence.success(empty)
      maximumresult.map(_.value) shouldBe Consequence.success(maximum)
    }

    "reject overflow and non-printable control characters" in {
      Given("a text beyond the maximum length and a text containing a newline")
      val toolong = "t" * (Text.LENGTH_MAX + 1)
      val withnewline = "first line\nsecond line"

      When("the invalid texts are parsed")
      val toolongresult = Text.parse(toolong)
      val newlineresult = Text.parse(withnewline)

      Then("both values fail as domain validation results")
      toolongresult shouldBe a[Consequence.Failure[?]]
      newlineresult shouldBe a[Consequence.Failure[?]]
    }

    "preserve stable printable nonlocalized text" in {
      Given("printable text containing Japanese characters and spaces")
      val source = "通知 本文"

      When("the Text value is constructed")
      val text = Text(source)

      Then("the source representation is preserved exactly")
      text.value shouldBe source
    }
  }
}
