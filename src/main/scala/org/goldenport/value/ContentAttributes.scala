package org.goldenport.value

import java.nio.charset.{Charset, StandardCharsets}
import scala.util.Try
import org.goldenport.Consequence
import org.goldenport.convert.ValueReader
import org.goldenport.datatype.{I18nText, MimeType}
import org.goldenport.record.Record
import org.goldenport.record.RecordPresentable
import org.goldenport.schema.XString

/*
 * SimpleEntity content body and derived content-reference index.
 *
 * @since   May.  3, 2026
 * @version May.  4, 2026
 * @author  ASAMI, Tomoharu
 */
final case class ContentBody(value: String) {
  def print: String = value
  override def toString: String = value
}

object ContentBody {
  given ValueReader[ContentBody] with {
    def readC(v: Any): Consequence[ContentBody] = v match {
      case m: ContentBody => Consequence.success(m)
      case m: I18nText => Consequence.success(ContentBody(m.toI18nString.displayMessage))
      case m: String => Consequence.success(ContentBody(m))
      case m: Record =>
        _string(m, "value", "content")
          .map(s => Consequence.success(ContentBody(s)))
          .getOrElse(Consequence.failValueInvalid(v, XString))
      case _ => Consequence.failValueInvalid(v, XString)
    }
  }

  private def _string(record: Record, names: String*): Option[String] =
    names.iterator.flatMap(record.getAny).map {
      case Some(v) => v.toString
      case v => v.toString
    }.nextOption()
}

enum ContentMarkup(val value: String) {
  case HtmlFragment extends ContentMarkup("html-fragment")
  case MarkdownGfm extends ContentMarkup("markdown-gfm")
  case SmartDox extends ContentMarkup("smartdox")

  def print: String = value
  override def toString: String = value
}

object ContentMarkup {
  def parseC(value: String): Consequence[ContentMarkup] =
    parseOption(value).map(Consequence.success).getOrElse(Consequence.failValueInvalid(value, XString))

  def parseOption(value: String): Option[ContentMarkup] =
    Option(value).map(_.trim.toLowerCase(java.util.Locale.ROOT)).flatMap {
      case "html-fragment" | "htmlfragment" | "html" => Some(HtmlFragment)
      case "markdown-gfm" | "markdowngfm" | "gfm" | "markdown" => Some(MarkdownGfm)
      case "smartdox" | "smart-dox" => Some(SmartDox)
      case _ => None
    }

  given ValueReader[ContentMarkup] with {
    def readC(v: Any): Consequence[ContentMarkup] = v match {
      case m: ContentMarkup => Consequence.success(m)
      case m: String => parseC(m)
      case m: Record =>
        m.getString("value").map(parseC).getOrElse(Consequence.failValueInvalid(v, XString))
      case _ => Consequence.failValueInvalid(v, XString)
    }
  }
}

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
  content: Option[ContentBody] = None,
  mimeType: Option[MimeType] = None,
  charset: Option[Charset] = None,
  markup: Option[ContentMarkup] = None,
  references: Vector[ContentReferenceOccurrence] = Vector.empty
) extends RecordPresentable {
  def toRecord(): Record =
    Record.dataAuto(
      "content" -> content.map(_.value),
      "mime_type" -> mimeType.map(_.print),
      "charset" -> charset.map(_.name()),
      "markup" -> markup.map(_.value),
      "references" -> references.map(_.toRecord())
    )

  def contentText: Option[String] =
    content.map(_.value)

  def effectiveCharset: Charset =
    charset.getOrElse(StandardCharsets.UTF_8)

  def effectiveMimeType: MimeType =
    mimeType.orElse(markup.flatMap {
      case ContentMarkup.HtmlFragment => Some(MimeType.TEXT_HTML)
      case ContentMarkup.MarkdownGfm => Some(MimeType.TEXT_MARKDOWN)
      case ContentMarkup.SmartDox => None
    }).getOrElse(MimeType.TEXT_PLAIN)
}

object ContentAttributes {
  val empty: ContentAttributes =
    ContentAttributes()

  trait Holder {
    protected def content_Attributes: ContentAttributes

    def content: Option[ContentBody] = content_Attributes.content
    def contentMimeType: Option[MimeType] = content_Attributes.mimeType
    def contentCharset: Option[Charset] = content_Attributes.charset
    def contentMarkup: Option[ContentMarkup] = content_Attributes.markup
    def contentReferences: Vector[ContentReferenceOccurrence] = content_Attributes.references
  }

  trait BareHolder {
    protected def content_Attributes: ContentAttributes

    def contentText: Option[String] = content_Attributes.contentText
    def contentMimeType: Option[MimeType] = content_Attributes.mimeType
    def contentCharset: Option[Charset] = content_Attributes.charset
    def contentMarkup: Option[ContentMarkup] = content_Attributes.markup
    def contentReferences: Vector[ContentReferenceOccurrence] = content_Attributes.references
  }

  case class Builder(
    contentAttributes: Option[ContentAttributes] = None,
    content: Option[ContentBody] = None,
    mimeType: Option[MimeType] = None,
    charset: Option[Charset] = None,
    markup: Option[ContentMarkup] = None,
    references: Option[Vector[ContentReferenceOccurrence]] = None
  ) {
    def withContentAttributes(p: ContentAttributes): Builder = copy(contentAttributes = Some(p))
    def withContent(p: ContentBody): Builder = copy(content = Some(p))
    def withContent(p: I18nText): Builder = withContent(ContentBody(p.toI18nString.displayMessage))
    def withContent(p: String): Builder = withContent(ContentBody(p))
    def withMimeType(p: MimeType): Builder = copy(mimeType = Some(p))
    def withMimeType(p: String): Builder = withMimeType(MimeType(p))
    def withContentType(p: String): Builder = withMimeType(p)
    def withCharset(p: Charset): Builder = copy(charset = Some(p))
    def withCharset(p: String): Builder = copy(charset = Some(Charset.forName(p)))
    def withMarkup(p: ContentMarkup): Builder = copy(markup = Some(p))
    def withMarkup(p: String): Builder = copy(markup = ContentMarkup.parseOption(p))
    def withReferences(p: Vector[ContentReferenceOccurrence]): Builder = copy(references = Some(p))

    def build(): ContentAttributes = {
      val base = contentAttributes.getOrElse(empty)
      base.copy(
        content = content.orElse(base.content),
        mimeType = mimeType.orElse(base.mimeType),
        charset = charset.orElse(base.charset),
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
      case s: String => Consequence.success(ContentAttributes(content = Some(ContentBody(s))))
      case _ => Consequence.failValueInvalid(v, XString)
    }
  }

  def createC(record: Record): Consequence[ContentAttributes] =
    for {
      refs <- _references(record)
      mime <- _mime_type(record, "contentMimeType", "content_mime_type", "mimeType", "mime_type", "contentType", "content_type")
      charset <- _charset(record, "contentCharset", "content_charset", "charset")
      markup <- _markup(record, "contentMarkup", "content_markup", "markup")
    } yield {
      ContentAttributes(
        content = _string(record, "content").map(ContentBody(_)),
        mimeType = mime,
        charset = charset,
        markup = markup,
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

  private def _mime_type(record: Record, names: String*): Consequence[Option[MimeType]] =
    names.iterator.flatMap(record.getAny).map {
      case Some(v) => v
      case v => v
    }.nextOption() match {
      case Some(m: MimeType) => Consequence.success(Some(m))
      case Some(s) => Consequence.success(Some(MimeType(s.toString.trim)))
      case None => Consequence.success(None)
    }

  private def _charset(record: Record, names: String*): Consequence[Option[Charset]] =
    names.iterator.flatMap(record.getAny).map {
      case Some(v) => v
      case v => v
    }.nextOption() match {
      case Some(m: Charset) => Consequence.success(Some(m))
      case Some(s) =>
        Try(Charset.forName(s.toString.trim)).toOption match {
          case Some(cs) => Consequence.success(Some(cs))
          case None => Consequence.failValueInvalid(s, XString)
        }
      case None => Consequence.success(None)
    }

  private def _markup(record: Record, names: String*): Consequence[Option[ContentMarkup]] =
    names.iterator.flatMap(record.getAny).map {
      case Some(v) => v
      case v => v
    }.nextOption() match {
      case Some(m: ContentMarkup) => Consequence.success(Some(m))
      case Some(s) =>
        ContentMarkup.parseOption(s.toString.trim) match {
          case Some(markup) => Consequence.success(Some(markup))
          case None => Consequence.failValueInvalid(s, XString)
        }
      case None => Consequence.success(None)
    }
}
