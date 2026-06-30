package org.goldenport.record.io

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import org.goldenport.record.Record

/*
 * @since   Jul.  1, 2026
 * @version Jul.  1, 2026
 * @author  ASAMI, Tomoharu
 */
class RecordEncoderSpec extends AnyWordSpec with GivenWhenThen with Matchers {
  "RecordEncoder.yaml" should {
    "preserve Unicode text in YAML output" in {
      Given("a record with Japanese text")
      val record = Record.data("message" -> "こんにちは")

      When("encoding the record as YAML")
      val yaml = RecordEncoder.yaml(record)

      Then("the YAML contains the original Unicode text")
      yaml should include ("こんにちは")
      yaml should not include ("\\u3053")
    }

    "render nested record collections as YAML sequences" in {
      Given("a record with a nested record collection")
      val record = Record.data(
        "events" -> Vector(
          Record.data("name" -> "job.submitted"),
          Record.data("name" -> "job.succeeded")
        )
      )

      When("encoding the record as YAML")
      val yaml = RecordEncoder.yaml(record)

      Then("the nested collection is rendered as a sequence of records")
      yaml should include ("events:")
      yaml should include ("- name: job.submitted")
      yaml should include ("- name: job.succeeded")
      yaml should not include ("IterableWrapper")
    }
  }

  "RecordEncoder.xml" should {
    "normalize unsafe record field names into XML element names" in {
      Given("a record with names that are not legal XML element names")
      val record = Record.data(
        "first name" -> "Taro",
        "1code" -> "A1",
        "@id" -> "p1"
      )

      When("encoding the record as XML")
      val xml = RecordEncoder.xml(record)

      Then("the encoder produces deterministic XML-safe element names")
      xml should include ("<first_name>Taro</first_name>")
      xml should include ("<_1code>A1</_1code>")
      xml should include ("<_id>p1</_id>")
      xml should not include ("<first name>")
    }

    "render nested record collections as XML item elements" in {
      Given("a record with a nested record collection")
      val record = Record.data(
        "events" -> Vector(
          Record.data("name" -> "job.submitted"),
          Record.data("name" -> "job.succeeded")
        )
      )

      When("encoding the record as XML")
      val xml = RecordEncoder.xml(record)

      Then("the nested collection is rendered under item elements")
      xml should include ("<record>")
      xml should include ("<events>")
      xml should include ("<item><name>job.submitted</name></item>")
      xml should include ("<item><name>job.succeeded</name></item>")
      xml should not include ("IterableWrapper")
    }
  }

}
