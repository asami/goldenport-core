package org.goldenport

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.goldenport.observation.Descriptor
import org.goldenport.provisional.observation.Taxonomy
import org.goldenport.provisional.observation.Cause

/*
 * @since   Apr. 29, 2026
 * @version Apr. 29, 2026
 * @author  ASAMI, Tomoharu
 */
class ConsequenceValidationSpec extends AnyWordSpec with Matchers {
  "structured validation helpers" should {
    "create parameter validation failures without status detail codes" in {
      val result = Consequence.argumentPolicyViolation[String](
        "contentType",
        "blob.mime-kind",
        "image/*",
        "application/octet-stream"
      )
      val conclusion = _failure(result)
      val facets = conclusion.observation.cause.descriptor.facets

      conclusion.observation.taxonomy shouldBe Taxonomy.argumentInvalid
      conclusion.observation.cause.kind shouldBe Some(Cause.Kind.Policy)
      conclusion.status.detailCodes shouldBe Nil
      facets should contain (Descriptor.Facet.Parameter.argument("contentType"))
      facets should contain (Descriptor.Facet.Policy("blob.mime-kind"))
      facets should contain (Descriptor.Facet.Expected("image/*"))
      facets should contain (Descriptor.Facet.Actual("application/octet-stream"))
    }

    "create format failures" in {
      val result = Consequence.argumentFormatError[String](
        "contentType",
        "MIME type",
        "not-a-mime"
      )
      val conclusion = _failure(result)
      val facets = conclusion.observation.cause.descriptor.facets

      conclusion.observation.taxonomy shouldBe Taxonomy.argumentFormatError
      conclusion.observation.cause.kind shouldBe Some(Cause.Kind.Format)
      conclusion.status.detailCodes shouldBe Nil
      facets should contain (Descriptor.Facet.Parameter.argument("contentType"))
      facets should contain (Descriptor.Facet.Expected("MIME type"))
      facets should contain (Descriptor.Facet.Actual("not-a-mime"))
    }

    "create field-path limit failures" in {
      val result = Consequence.argumentFieldLimitExceeded[String](
        "payload.byteSize",
        10L,
        20L,
        "blob.upload.max-byte-size"
      )
      val conclusion = _failure(result)
      val facets = conclusion.observation.cause.descriptor.facets

      conclusion.observation.taxonomy shouldBe Taxonomy.argumentInvalid
      conclusion.observation.cause.kind shouldBe Some(Cause.Kind.Limit)
      conclusion.status.detailCodes shouldBe Nil
      facets should contain (Descriptor.Facet.FieldPath("payload.byteSize"))
      facets should contain (Descriptor.Facet.Policy("blob.upload.max-byte-size"))
      facets should contain (Descriptor.Facet.Limit(10L))
      facets should contain (Descriptor.Facet.Actual(20L))
      facets.collect { case x: Descriptor.Facet.Expected => x } shouldBe Nil
    }

    "create integrity mismatch failures" in {
      val result = Consequence.argumentIntegrityMismatch[String](
        "expectedDigest",
        "sha-256",
        "abc",
        "def"
      )
      val conclusion = _failure(result)
      val facets = conclusion.observation.cause.descriptor.facets

      conclusion.observation.taxonomy shouldBe Taxonomy.argumentInvalid
      conclusion.observation.cause.kind shouldBe Some(Cause.Kind.Inconsistency)
      conclusion.status.detailCodes shouldBe Nil
      facets should contain (Descriptor.Facet.Parameter.argument("expectedDigest"))
      facets should contain (Descriptor.Facet.Algorithm("sha-256"))
      facets should contain (Descriptor.Facet.Expected("abc"))
      facets should contain (Descriptor.Facet.Actual("def"))
    }
  }

  private def _failure[A](result: Consequence[A]): Conclusion = result match {
    case Consequence.Failure(conclusion) => conclusion
    case _ => fail("Expected Consequence.Failure")
  }
}
