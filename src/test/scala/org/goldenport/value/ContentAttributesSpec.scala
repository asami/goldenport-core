package org.goldenport.value

import java.nio.charset.StandardCharsets
import org.goldenport.Consequence
import org.goldenport.convert.ValueReader
import org.goldenport.datatype.{I18nText, MimeType}
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   May.  4, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class ContentAttributesSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "ContentAttributes" should {
    "round-trip typed content metadata from a Record" in {
      Given("one document body with explicit content metadata and a reference")
      val reference = ContentReferenceOccurrence(
        contentField = Some("content"),
        markup = Some("html-fragment"),
        elementKind = Some("img"),
        attributeName = Some("src"),
        occurrenceIndex = 0,
        referenceKind = Some("image"),
        urn = Some("urn:textus:image:abc")
      )
      val source = Record.dataAuto(
        "content" -> "<p>Hello</p>",
        "content_mime_type" -> "text/html",
        "content_charset" -> "UTF-8",
        "content_markup" -> "html-fragment",
        "content_references" -> Vector(reference.toRecord())
      )

      When("the Record is decoded and encoded through the content contract")
      val attributes = _success(ContentAttributes.createC(source))
      val roundtrip = _success(ContentAttributes.createC(attributes.toRecord()))

      Then("the document body and every typed metadata field are preserved")
      attributes.content shouldBe Some(ContentBody("<p>Hello</p>"))
      attributes.mimeType shouldBe Some(MimeType.TEXT_HTML)
      attributes.charset shouldBe Some(StandardCharsets.UTF_8)
      attributes.markup shouldBe Some(ContentMarkup.HtmlFragment)
      attributes.references shouldBe Vector(reference)
      roundtrip shouldBe attributes
    }

    "reject an invalid charset deterministically" in {
      Given("a document Record with an invalid charset")
      val source = Record.dataAuto("content" -> "x", "charset" -> "not-a-charset")

      When("the content contract decodes the Record")
      val result = ContentAttributes.createC(source)

      Then("the invalid charset is rejected")
      result shouldBe a[Consequence.Failure[_]]
    }

    "reject locale-aware text at the single-document body boundary" in {
      Given("locale-aware narrative text and an explicit single document")
      val localized = I18nText("localized narrative")

      When("both values cross the ContentBody boundary")
      val localizedresult = summon[ValueReader[ContentBody]].readC(localized)
      val explicitresult = ContentAttributes.Builder().withContent("explicit document body").build()

      Then("implicit locale collapse is rejected and explicit content is retained")
      localizedresult shouldBe a[Consequence.Failure[_]]
      explicitresult.content shouldBe
        Some(ContentBody("explicit document body"))
    }
  }

  private def _success[A](result: Consequence[A]): A =
    result match {
      case Consequence.Success(value) => value
      case Consequence.Failure(c) => fail(c.toString)
    }
}
