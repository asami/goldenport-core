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
final class NameSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Name" should {
    "accept values at both generic name length boundaries" in {
      Given("one-character and maximum-length nonlocalized names")
      val minimum = "n"
      val maximum = "n" * Name.LENGTH_MAX

      When("the names are parsed")
      val minimumresult = Name.parse(minimum)
      val maximumresult = Name.parse(maximum)

      Then("both boundary values retain their exact representation")
      minimumresult.map(_.value) shouldBe Consequence.success(minimum)
      maximumresult.map(_.value) shouldBe Consequence.success(maximum)
    }

    "reject values outside the generic name length range" in {
      Given("an empty name and a name beyond the maximum length")
      val empty = ""
      val toolong = "n" * (Name.LENGTH_MAX + 1)

      When("the invalid names are parsed")
      val emptyresult = Name.parse(empty)
      val toolongresult = Name.parse(toolong)

      Then("both values fail as domain validation results")
      emptyresult shouldBe a[Consequence.Failure[?]]
      toolongresult shouldBe a[Consequence.Failure[?]]
    }

    "preserve a stable nonlocalized name without locale interpretation" in {
      Given("a generic symbolic name")
      val source = "account-primary"

      When("the Name value is constructed")
      val name = Name(source)

      Then("the source representation is preserved exactly")
      name.value shouldBe source
    }
  }
}
