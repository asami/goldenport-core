package org.goldenport.value

import org.goldenport.Consequence
import org.goldenport.convert.ValueReader
import org.goldenport.datatype.I18nText
import org.goldenport.record.Record
import org.goldenport.record.RecordPresentable
import org.goldenport.schema.XString

/*
 * SimpleEntity content body and derived content-reference index.
 *
 * @since   May.  3, 2026
 * @version May.  3, 2026
 * @author  ASAMI, Tomoharu
 */
case class ContentReferenceOccurrence(
  contentField: Option[String] = None,
  markup: Option[String] = None,
  elementKind: Option[String] = None,
  attributeName: Option[String] = None,
  occurrenceIndex: Int = 0,
  originalRef: Option[String] = None,
  normalizedRef: Option[String] = None,
  referenceKind: Option[String] = None,
  urn: Option[String] = None,
  targetEntityId: Option[String] = None,
  label: Option[String] = None,
  alt: Option[String] = None,
  title: Option[String] = None,
  rel: Option[String] = None,
  mediaType: Option[String] = None,
  sortOrder: Option[Int] = None
) extends RecordPresentable {
  def toRecord(): Record =
    Record.dataAuto(
      "content_field" -> contentField,
      "markup" -> markup,
      "element_kind" -> elementKind,
      "attribute_name" -> attributeName,
      "occurrence_index" -> occurrenceIndex,
      "original_ref" -> originalRef,
      "normalized_ref" -> normalizedRef,
      "reference_kind" -> referenceKind,
      "urn" -> urn,
      "target_entity_id" -> targetEntityId,
      "label" -> label,
      "alt" -> alt,
      "title" -> title,
      "rel" -> rel,
      "media_type" -> mediaType,
      "sort_order" -> sortOrder
    )
}

object ContentReferenceOccurrence {
  given ValueReader[ContentReferenceOccurrence] with {
    def readC(v: Any): Consequence[ContentReferenceOccurrence] = v match {
      case m: ContentReferenceOccurrence => Consequence.success(m)
      case m: Record => createC(m)
      case _ => Consequence.failValueInvalid(v, XString)
    }
  }

  def createC(record: Record): Consequence[ContentReferenceOccurrence] =
    Consequence.success(
      ContentReferenceOccurrence(
        contentField = _string(record, "contentField", "content_field"),
        markup = _string(record, "markup"),
        elementKind = _string(record, "elementKind", "element_kind"),
        attributeName = _string(record, "attributeName", "attribute_name"),
        occurrenceIndex = _int(record, "occurrenceIndex", "occurrence_index").getOrElse(0),
        originalRef = _string(record, "originalRef", "original_ref"),
        normalizedRef = _string(record, "normalizedRef", "normalized_ref"),
        referenceKind = _string(record, "referenceKind", "reference_kind"),
        urn = _string(record, "urn"),
        targetEntityId = _string(record, "targetEntityId", "target_entity_id"),
        label = _string(record, "label"),
        alt = _string(record, "alt"),
        title = _string(record, "title"),
        rel = _string(record, "rel"),
        mediaType = _string(record, "mediaType", "media_type"),
        sortOrder = _int(record, "sortOrder", "sort_order")
      )
    )

  private def _string(record: Record, names: String*): Option[String] =
    names.iterator.flatMap(record.getAny).map {
      case Some(v) => v.toString
      case v => v.toString
    }.map(_.trim).filter(_.nonEmpty).nextOption()

  private def _int(record: Record, names: String*): Option[Int] =
    names.iterator.flatMap(record.getAny).flatMap {
      case i: Int => Some(i)
      case i: java.lang.Integer => Some(i.intValue)
      case s: String => scala.util.Try(s.trim.toInt).toOption
      case other => scala.util.Try(other.toString.trim.toInt).toOption
    }.nextOption()
}

case class ContentAttributes(
  content: Option[I18nText] = None,
  mimeType: Option[String] = None,
  markup: Option[String] = None,
  references: Vector[ContentReferenceOccurrence] = Vector.empty
) extends RecordPresentable {
  def toRecord(): Record =
    Record.dataAuto(
      "content" -> content.map(_.toI18nString.displayMessage),
      "mime_type" -> mimeType,
      "markup" -> markup,
      "references" -> references.map(_.toRecord())
    )
}

object ContentAttributes {
  val empty: ContentAttributes =
    ContentAttributes()

  trait Holder {
    protected def content_Attributes: ContentAttributes

    def content: Option[I18nText] = content_Attributes.content
    def contentMimeType: Option[String] = content_Attributes.mimeType
    def contentMarkup: Option[String] = content_Attributes.markup
    def contentReferences: Vector[ContentReferenceOccurrence] = content_Attributes.references
  }

  trait BareHolder {
    protected def content_Attributes: ContentAttributes

    def contentText: Option[String] = content_Attributes.content.map(_.toI18nString.displayMessage)
    def contentMimeType: Option[String] = content_Attributes.mimeType
    def contentMarkup: Option[String] = content_Attributes.markup
    def contentReferences: Vector[ContentReferenceOccurrence] = content_Attributes.references
  }

  case class Builder(
    contentAttributes: Option[ContentAttributes] = None,
    content: Option[I18nText] = None,
    mimeType: Option[String] = None,
    markup: Option[String] = None,
    references: Option[Vector[ContentReferenceOccurrence]] = None
  ) {
    def withContentAttributes(p: ContentAttributes): Builder = copy(contentAttributes = Some(p))
    def withContent(p: I18nText): Builder = copy(content = Some(p))
    def withContent(p: String): Builder = copy(content = Some(I18nText(p)))
    def withMimeType(p: String): Builder = copy(mimeType = Some(p))
    def withContentType(p: String): Builder = withMimeType(p)
    def withMarkup(p: String): Builder = copy(markup = Some(p))
    def withReferences(p: Vector[ContentReferenceOccurrence]): Builder = copy(references = Some(p))

    def build(): ContentAttributes = {
      val base = contentAttributes.getOrElse(empty)
      base.copy(
        content = content.orElse(base.content),
        mimeType = mimeType.orElse(base.mimeType),
        markup = markup.orElse(base.markup),
        references = references.getOrElse(base.references)
      )
    }
  }

  object Builder {
    def apply(p: ContentAttributes): Builder =
      Builder(contentAttributes = Some(p))
  }

  given ValueReader[ContentAttributes] with {
    def readC(v: Any): Consequence[ContentAttributes] = v match {
      case m: ContentAttributes => Consequence.success(m)
      case m: Record => createC(m)
      case s: String => Consequence.success(ContentAttributes(content = Some(I18nText(s))))
      case _ => Consequence.failValueInvalid(v, XString)
    }
  }

  def createC(record: Record): Consequence[ContentAttributes] =
    _references(record).map { refs =>
      ContentAttributes(
        content = _string(record, "content").map(I18nText(_)),
        mimeType = _string(record, "mimeType", "mime_type", "contentType", "content_type"),
        markup = _string(record, "markup"),
        references = refs
      )
    }

  private def _references(record: Record): Consequence[Vector[ContentReferenceOccurrence]] =
    record.getAny("references")
        .orElse(record.getAny("contentReferences"))
        .orElse(record.getAny("content_references")) match {
      case Some(xs: Seq[?]) => _reference_seq(xs)
      case Some(xs: Array[?]) => _reference_seq(xs.toVector)
      case Some(r: Record) => ContentReferenceOccurrence.createC(r).map(Vector(_))
      case Some(_) => Consequence.failValueInvalid(record, XString)
      case None => Consequence.success(Vector.empty)
    }

  private def _reference_seq(xs: Seq[?]): Consequence[Vector[ContentReferenceOccurrence]] =
    xs.foldLeft(Consequence.success(Vector.empty[ContentReferenceOccurrence])) { (z, x) =>
      z.flatMap { values =>
        x match {
          case r: Record => ContentReferenceOccurrence.createC(r).map(values :+ _)
          case o: ContentReferenceOccurrence => Consequence.success(values :+ o)
          case _ => Consequence.failValueInvalid(x, XString)
        }
      }
    }

  private def _string(record: Record, names: String*): Option[String] =
    names.iterator.flatMap(record.getAny).map {
      case Some(v) => v.toString
      case v => v.toString
    }.map(_.trim).filter(_.nonEmpty).nextOption()
}
