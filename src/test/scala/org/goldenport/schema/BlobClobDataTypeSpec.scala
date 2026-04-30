package org.goldenport.schema

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * Executable specification for binary and character large object datatypes.
 *
 * @since   Apr. 26, 2026
 * @version Apr. 30, 2026
 * @author  ASAMI, Tomoharu
 */
final class BlobClobDataTypeSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen {
  "Blob and Clob datatypes" should {
    "have stable schema names" in {
      Given("the core large-object datatypes")
      When("their schema names are read")
      Then("Blob and Clob have stable public names")
      XBlob.name shouldBe "blob"
      XBlob.print shouldBe "blob"
      XFileBundle.name shouldBe "filebundle"
      XFileBundle.print shouldBe "filebundle"
      XClob.name shouldBe "clob"
      XClob.print shouldBe "clob"
    }
  }
}
