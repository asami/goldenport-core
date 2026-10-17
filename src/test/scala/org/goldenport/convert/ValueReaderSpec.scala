package org.goldenport.convert

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.GivenWhenThen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.goldenport.Consequence
import org.goldenport.record.io.RecordDecoder

/*
 * @since   Dec. 22, 2025
 * @version Oct. 14, 2026
 * @author  ASAMI, Tomoharu
 */
class ValueReaderSpec extends AnyWordSpec
  with ScalaCheckDrivenPropertyChecks
  with Matchers
  with GivenWhenThen {

  private def _metadata(example: String, rules: String) =
    afterWord(s"in spec:value-reader-json-integers, example:$example, rules:$rules, phase:sm-workflow-6")

  "Exact integer ValueReader intake" should {
    "N1 read whole decimals as exact Int values" must _metadata("N1", "R1,R2,R4") {
      "including signed properties, limits, scale and exponent notation" in {
        Given("Spec: docs/spec/value-reader-json-integers.md; Rules: R1,R2,R4; Example: N1; whole decimals and the existing native/trimmed integer inputs")
        val reader = summon[ValueReader[Int]]
        val rows = Vector(BigDecimal(0) -> 0, BigDecimal(-1) -> -1,
          BigDecimal(Int.MinValue) -> Int.MinValue, BigDecimal(Int.MaxValue) -> Int.MaxValue,
          BigDecimal("42.000") -> 42, BigDecimal("1E+3") -> 1000)

        forAll { (n: Int) =>
          When("a signed integral decimal and its existing native and string representations are read as Int")
          val results = Vector[Any](BigDecimal(n), n, n.toLong, s"  $n  ").map(reader.readC)

          Then("every representation yields the exact signed Int")
          results.foreach(_ shouldBe Consequence.success(n))
        }

        When("the visible Int boundary, scale and exponent rows are read")
        val results = rows.map((value, expected) => reader.readC(value) -> expected)

        Then("each row preserves its exact integer value")
        results.foreach((result, expected) => result shouldBe Consequence.success(expected))
      }
    }

    "N2 read whole decimals as exact Long values" must _metadata("N2", "R1,R2,R4") {
      "including signed properties, limits and values beyond the Double exact range" in {
        Given("Spec: docs/spec/value-reader-json-integers.md; Rules: R1,R2,R4; Example: N2; whole decimals including Long limits and integers beyond 2^53")
        val reader = summon[ValueReader[Long]]
        val rows = Vector(BigDecimal(0) -> 0L, BigDecimal(-1) -> -1L,
          BigDecimal(Long.MinValue) -> Long.MinValue, BigDecimal(Long.MaxValue) -> Long.MaxValue,
          BigDecimal("9007199254740993") -> 9007199254740993L,
          BigDecimal("-9007199254740993") -> -9007199254740993L,
          BigDecimal("42.000") -> 42L, BigDecimal("1E+3") -> 1000L)

        forAll { (n: Long) =>
          When("a signed integral decimal and its existing native and trimmed string representations are read as Long")
          val results = Vector[Any](BigDecimal(n), n, s"  $n  ").map(reader.readC)

          Then("every representation yields the exact signed Long without floating conversion")
          results.foreach(_ shouldBe Consequence.success(n))
        }

        When("the visible Long precision rows and existing native Int inputs are read")
        val results = rows.map((value, expected) => reader.readC(value) -> expected)
        val native = Vector(Int.MinValue, -1, 0, Int.MaxValue).map(n => reader.readC(n) -> n.toLong)

        Then("each row and native Int retains its exact Long value")
        (results ++ native).foreach((result, expected) => result shouldBe Consequence.success(expected))
      }
    }

    "N3 reject nonintegral, out-of-range and unsupported integer input" must _metadata("N3", "R2,R3") {
      "without rounding, defaulting or throwing" in {
        Given("Spec: docs/spec/value-reader-json-integers.md; Rules: R2,R3; Example: N3; signed fractions, exact range violations, malformed strings and unsupported inputs")
        val intreader = summon[ValueReader[Int]]
        val longreader = summon[ValueReader[Long]]
        val unsupported = Vector[Any](BigDecimal("0.5"), BigDecimal("-0.5"),
          "", "bad", "1.0", "-1.5", "1e3", 1.0d, 1.0f, true, false, null,
          BigInt(1), new java.math.BigDecimal("1"))
        val intinputs = unsupported ++ Vector(BigDecimal(Int.MinValue) - 1, BigDecimal(Int.MaxValue) + 1)
        val longinputs = unsupported ++ Vector(BigDecimal(Long.MinValue) - 1, BigDecimal(Long.MaxValue) + 1)

        forAll { (n: Long) =>
          When("a generated signed integral value plus exact 0.5 is read as either integer target")
          val fraction = BigDecimal(n) + BigDecimal("0.5")
          val intresult = intreader.readC(fraction)
          val longresult = longreader.readC(fraction)

          Then("both targets return a structured failure")
          intresult shouldBe a[Consequence.Failure[?]]
          longresult shouldBe a[Consequence.Failure[?]]
        }

        When("the explicit rejected input rows are read by their target reader")
        val intresults = intinputs.map(intreader.readC)
        val longresults = longinputs.map(longreader.readC)

        Then("all reads return structured failure rather than a default integer or exception")
        intresults.foreach(_ shouldBe a[Consequence.Failure[?]])
        longresults.foreach(_ shouldBe a[Consequence.Failure[?]])
      }
    }

    "N4 retain exact integers through strict JSON Record intake" must _metadata("N4", "R1,R2,R3,R5") {
      "while preserving invalid, missing and malformed input outcomes" in {
        Given("Spec: docs/spec/value-reader-json-integers.md; Rules: R1,R2,R3,R5; Example: N4; real JSON numeric fields decoded without string or native-integer substitution")
        val decoder = new RecordDecoder()
        val introws = Vector(0, Int.MinValue, Int.MaxValue)
        val longrows = Vector(0L, Long.MinValue, Long.MaxValue, 9007199254740993L)
        val intinvalid = Vector("0.5", "-0.5", "-2147483649", "2147483648")
        val longinvalid = Vector("0.5", "-0.5", "-9223372036854775809", "9223372036854775808")

        When("strict JSON numbers are consumed through ordinary Record integer reads")
        val intresults = introws.map(n => decoder.json(s"""{"value":$n}""").flatMap(_.getAsC[Int]("value")) -> n)
        val longresults = longrows.map(n => decoder.json(s"""{"value":$n}""").flatMap(_.getAsC[Long]("value")) -> n)
        val intrecords = intinvalid.map(n => decoder.json(s"""{"value":$n}"""))
        val longrecords = longinvalid.map(n => decoder.json(s"""{"value":$n}"""))
        val rejectedints = intrecords.map(_.flatMap(_.getAsC[Int]("value")))
        val rejectedlongs = longrecords.map(_.flatMap(_.getAsC[Long]("value")))
        val missingint = decoder.json("{}").flatMap(_.getAsC[Int]("value"))
        val missinglong = decoder.json("{}").flatMap(_.getAsC[Long]("value"))
        val malformed = decoder.json("{\"value\":")

        Then("present numbers retain exact Some values, valid nonintegers fail reads, missing fields remain None and malformed JSON fails decoding")
        intresults.foreach((result, expected) => result shouldBe Consequence.success(Some(expected)))
        longresults.foreach((result, expected) => result shouldBe Consequence.success(Some(expected)))
        (intrecords ++ longrecords).foreach(_.toOption should not be empty)
        rejectedints.foreach(_ shouldBe a[Consequence.Failure[?]])
        rejectedlongs.foreach(_ shouldBe a[Consequence.Failure[?]])
        missingint shouldBe Consequence.success(Option.empty[Int])
        missinglong shouldBe Consequence.success(Option.empty[Long])
        malformed shouldBe a[Consequence.Failure[?]]
      }
    }
  }
}
