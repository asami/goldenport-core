package org.goldenport.http

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * Executable specification for redirect HTTP status support.
 *
 * @since   Apr. 26, 2026
 * @version Apr. 26, 2026
 * @author  ASAMI, Tomoharu
 */
final class HttpStatusRedirectSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen {
  "HttpStatus" should {
    "resolve redirect statuses used by Blob access" in {
      Given("redirect status codes")
      When("mapping from integers")
      Then("See Other and Temporary Redirect are first-class statuses")
      HttpStatus.fromInt(303) shouldBe Some(HttpStatus.SeeOther)
      HttpStatus.fromInt(307) shouldBe Some(HttpStatus.TemporaryRedirect)
    }
  }
}
