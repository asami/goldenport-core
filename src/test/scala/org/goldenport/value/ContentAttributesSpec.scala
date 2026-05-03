package org.goldenport.value

import java.nio.charset.StandardCharsets
import org.goldenport.Consequence
import org.goldenport.datatype.MimeType
import org.goldenport.record.Record
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   May.  4, 2026
 * @version May.  4, 2026
 * @author  ASAMI, Tomoharu
 */
final class ContentAttributesSpec extends AnyWordSpec with Matchers {
  "ContentAttributes" should {
    "round-trip typed content metadata from a Record" in {
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

      val attributes = _success(ContentAttributes.createC(source))

      attributes.content shouldBe Some(ContentBody("<p>Hello</p>"))
      attributes.mimeType shouldBe Some(MimeType.TEXT_HTML)
      attributes.charset shouldBe Some(StandardCharsets.UTF_8)
      attributes.markup shouldBe Some(ContentMarkup.HtmlFragment)
      attributes.references shouldBe Vector(reference)

      val roundtrip = _success(ContentAttributes.createC(attributes.toRecord()))
      roundtrip shouldBe attributes
    }

    "reject an invalid charset deterministically" in {
      ContentAttributes.createC(Record.dataAuto("content" -> "x", "charset" -> "not-a-charset")) shouldBe a[Consequence.Failure[_]]
    }
  }

  private def _success[A](result: Consequence[A]): A =
    result match {
      case Consequence.Success(value) => value
      case Consequence.Failure(c) => fail(c.toString)
    }
}
