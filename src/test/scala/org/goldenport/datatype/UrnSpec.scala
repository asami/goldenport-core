package org.goldenport.datatype

import java.net.URI
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

/*
 * @since   Apr.  9, 2026
 * @version Apr.  9, 2026
 * @author  ASAMI, Tomoharu
 */
class UrnSpec extends AnyWordSpec with Matchers {
  "Urn" should {
    "accept a valid urn string" in {
      val urn = Urn.parse("urn:isbn:9780306406157").TAKE
      urn.value shouldBe "urn:isbn:9780306406157"
      urn.uri shouldBe URI.create("urn:isbn:9780306406157")
    }

    "reject a non-urn uri" in {
      Urn.parse("https://example.com").toOption shouldBe None
    }

    "read from URI through ValueReader" in {
      val urn = Urn.given_ValueReader_Urn
        .readC(URI.create("urn:example:sample"))
        .TAKE
      urn.value shouldBe "urn:example:sample"
    }
  }
}
