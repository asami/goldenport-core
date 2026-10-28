package org.goldenport.protocol

import org.scalatest.GivenWhenThen
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

/*
 * @since   Mar. 31, 2026
 *  version Jul. 10, 2026
 * @version Oct. 28, 2026
 * @author  ASAMI, Tomoharu
 */
class RequestRecordSpec extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckDrivenPropertyChecks {
  "Request.toRecord" should {
    "retain explicitly carried collections independently of repeated scalar parameters" in {
      forAll(Gen.listOf(Gen.oneOf(" 雪 ", "e\u0301", "é", "🧭/%", "", "\n"))) { sample =>
        Given("explicit Vector Arguments and Properties including empty and singleton collections")
        val vectors = Vector(Vector.empty[String], Vector(" 雪/e\u0301🧭 "), sample.toVector)
        When("Request is represented as a Record with dotted property nesting")
        val results = vectors.map { values =>
          val req = Request.of("fixture", "material", "create").copy(
            arguments = List(Argument("sources", values, None)),
            properties = List(Property("selection.resources", values, None)))
          values -> req.toRecord
        }
        Then("each exact Vector survives without singleton collapse or empty omission")
        results.foreach { case (values, record) =>
          record.getAny("sources") shouldBe Some(values)
          record.getRecord("selection").get.getAny("resources") shouldBe Some(values)
        }
      }
    }

    "nest dotted argument and property names into nested records" in {
      val req = Request(
        component = Some("sample"),
        service = Some("person"),
        operation = "create",
        arguments = List(
          Argument("name", "alice", None)
        ),
        switches = Nil,
        properties = List(
          Property("address.street", "1-2-3 Marunouchi", None),
          Property("address.city", "Tokyo", None),
          Property("address.country.value", "JP", None)
        )
      )

      val record = req.toRecord
      val address = record.getRecord("address").get
      val country = address.getRecord("country").get

      record.getString("name") shouldBe Some("alice")
      address.getString("street") shouldBe Some("1-2-3 Marunouchi")
      address.getString("city") shouldBe Some("Tokyo")
      country.getString("value") shouldBe Some("JP")
    }

    "preserve repeated argument and property values as vectors" in {
      val req = Request(
        component = Some("sample"),
        service = Some("facility"),
        operation = "update",
        arguments = Nil,
        switches = Nil,
        properties = List(
          Property("fetch_methods", "official_driver", None),
          Property("fetch_methods", "museum_or_jp", None),
          Property("metadata.tags", "official", None),
          Property("metadata.tags", "curated", None)
        )
      )

      val record = req.toRecord
      val metadata = record.getRecord("metadata").get

      record.getAny("fetch_methods") shouldBe Some(Vector("official_driver", "museum_or_jp"))
      metadata.getAny("tags") shouldBe Some(Vector("official", "curated"))
    }
  }
}
