package org.goldenport.observation

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

/*
 * @since   Apr. 29, 2026
 * @version Apr. 29, 2026
 * @author  ASAMI, Tomoharu
 */
class DescriptorValidationFacetSpec extends AnyWordSpec with Matchers {
  "validation descriptor facets" should {
    "serialize machine-readable validation details" in {
      val record = Descriptor(Vector(
        Descriptor.Facet.FieldPath("payload.byteSize"),
        Descriptor.Facet.Policy("blob.upload.max-byte-size"),
        Descriptor.Facet.Limit(10L),
        Descriptor.Facet.Expected("image/*"),
        Descriptor.Facet.Actual("application/octet-stream"),
        Descriptor.Facet.Algorithm("sha-256"),
        Descriptor.Facet.Reason("required-capability"),
        Descriptor.Facet.Capability("collection:blob:create"),
        Descriptor.Facet.Permission("execute"),
        Descriptor.Facet.Guard("abac-condition"),
        Descriptor.Facet.Relation("owner")
      )).toRecord

      record.getString("fieldPath") shouldBe Some("payload.byteSize")
      record.getString("policy") shouldBe Some("blob.upload.max-byte-size")
      record.getString("limit") shouldBe Some("10")
      record.getString("expected") shouldBe Some("image/*")
      record.getString("actual") shouldBe Some("application/octet-stream")
      record.getString("algorithm") shouldBe Some("sha-256")
      record.getString("reason") shouldBe Some("required-capability")
      record.getString("capability") shouldBe Some("collection:blob:create")
      record.getString("permission") shouldBe Some("execute")
      record.getString("guard") shouldBe Some("abac-condition")
      record.getString("relation") shouldBe Some("owner")
    }
  }
}
