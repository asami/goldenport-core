package org.goldenport.protocol

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
class RequestRecordSpec extends AnyWordSpec with Matchers {
  "Request.toRecord" should {
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
      val address = record.asMap("address").asInstanceOf[org.goldenport.record.Record]
      val country = address.asMap("country").asInstanceOf[org.goldenport.record.Record]

      record.getString("name") shouldBe Some("alice")
      address.getString("street") shouldBe Some("1-2-3 Marunouchi")
      address.getString("city") shouldBe Some("Tokyo")
      country.getString("value") shouldBe Some("JP")
    }
  }
}
