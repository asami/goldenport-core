package org.goldenport.datatype

import org.goldenport.Consequence
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 15, 2026
 * @version Jul. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class IpAddressSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "IpAddress" should {
    "parse IPv4 and IPv6 literals without accepting host names" in {
      Given("canonical IPv4 and compressed IPv6 literals")
      val ipv4 = "192.0.2.10"
      val ipv6 = "2001:db8::1"

      When("the literals are parsed")
      val ipv4result = IpAddress.parse(ipv4)
      val ipv6result = IpAddress.parse(ipv6)

      Then("both values are retained as canonical address literals")
      ipv4result.map(_.value) shouldBe Consequence.success(ipv4)
      ipv6result.map(_.value) shouldBe Consequence.success("2001:db8:0:0:0:0:0:1")
    }

    "reject malformed literals and DNS host names" in {
      Given("an out-of-range IPv4 address and a host name")
      val malformed = "192.0.2.999"
      val hostname = "example.com"

      When("the invalid values are parsed")
      val malformedresult = IpAddress.parse(malformed)
      val hostnameresult = IpAddress.parse(hostname)

      Then("neither value is accepted as an IP address")
      malformedresult shouldBe a[Consequence.Failure[?]]
      hostnameresult shouldBe a[Consequence.Failure[?]]
    }

    "reject ambiguous IPv4 literals with leading zeroes" in {
      Given("an IPv4 literal whose octet could be interpreted as octal")
      val source = "192.168.001.1"

      When("the literal is parsed")
      val result = IpAddress.parse(source)

      Then("the ambiguous representation is rejected")
      result shouldBe a[Consequence.Failure[?]]
    }
  }
}
