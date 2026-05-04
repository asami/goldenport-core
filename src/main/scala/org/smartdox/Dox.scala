package org.smartdox

import java.util.Locale

/*
 * Minimal SmartDox AST used by CNCF content handling.
 *
 * This package intentionally carries only parser/render/reference primitives.
 * Full SmartDox site generation, executable includes, semantic metadata, and
 * external data expansion stay in the legacy SmartDox implementation.
 *
 * @since   May.  4, 2026
 * @version May.  4, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait Dox

sealed trait Block extends Dox

sealed trait Inline extends Dox

final case class SourceSpan(
  start: Int,
  end: Int
) {
  require(start >= 0, s"SourceSpan start must be non-negative: $start")
  require(end >= start, s"SourceSpan end must be >= start: $start-$end")
}

final case class DoxReferenceSourceSpan(
  node: SourceSpan,
  target: SourceSpan
)

final case class Document(
  head: DocumentHead = DocumentHead.empty,
  body: Body = Body.empty
) extends Dox

final case class DocumentHead(
  metadata: Map[String, String] = Map.empty
)

object DocumentHead {
  val empty: DocumentHead =
    DocumentHead()
}

final case class Body(
  blocks: Vector[Block] = Vector.empty
) extends Dox

object Body {
  val empty: Body =
    Body()
}

final case class Section(
  level: Int,
  title: Vector[Inline],
  children: Vector[Block] = Vector.empty,
  style: SectionStyle = SectionStyle.Markdown
) extends Block

enum SectionStyle {
  case Org
  case Markdown
  case Setext
}

final case class Paragraph(
  inlines: Vector[Inline]
) extends Block

final case class UnorderedList(
  items: Vector[ListItem]
) extends Block

final case class OrderedList(
  items: Vector[ListItem]
) extends Block

final case class DefinitionList(
  items: Vector[DefinitionItem]
) extends Block

final case class ListItem(
  blocks: Vector[Block]
)

final case class DefinitionItem(
  term: Vector[Inline],
  description: Vector[Block]
)

final case class Table(
  caption: Option[Vector[Inline]] = None,
  label: Option[String] = None,
  rows: Vector[TableRow]
) extends Block

final case class TableRow(
  cells: Vector[Vector[Inline]],
  separator: Boolean = false
)

final case class ImageRef(
  uri: String,
  alt: Option[String] = None,
  title: Option[String] = None,
  sourceSpan: Option[DoxReferenceSourceSpan] = None
) extends Block with Inline

final case class Figure(
  image: ImageRef,
  caption: Option[Vector[Inline]] = None,
  label: Option[String] = None
) extends Block

final case class Program(
  language: Option[String],
  body: String,
  kind: String = "src"
) extends Block

final case class HorizontalRule() extends Block

final case class Quote(
  blocks: Vector[Block]
) extends Block

final case class BlockMacro(
  name: String,
  target: String,
  attributes: Map[String, String] = Map.empty
) extends Block

final case class UnsupportedBlock(
  name: String,
  source: String
) extends Block

final case class Text(
  value: String
) extends Inline

final case class Bold(
  inlines: Vector[Inline]
) extends Inline

final case class Italic(
  inlines: Vector[Inline]
) extends Inline

final case class Underline(
  inlines: Vector[Inline]
) extends Inline

final case class Code(
  value: String
) extends Inline

final case class Pre(
  value: String
) extends Inline

final case class Delete(
  inlines: Vector[Inline]
) extends Inline

final case class Span(
  tag: String,
  inlines: Vector[Inline],
  attributes: Map[String, String] = Map.empty
) extends Inline

final case class Hyperlink(
  href: String,
  label: Vector[Inline] = Vector.empty,
  kind: LinkKind = LinkKind.Generic,
  sourceSpan: Option[DoxReferenceSourceSpan] = None
) extends Inline

final case class InlineMacro(
  name: String,
  value: String
) extends Inline

final case class StructuredToken(
  format: String,
  raw: String
) extends Inline

final case class I18NFragment(
  values: Map[Locale, Vector[Inline]]
) extends Inline

enum LinkKind {
  case Generic
  case Markdown
  case Org
  case AutoUrl
  case Site
  case LegacySite
}

final case class DoxReference(
  elementKind: String,
  attributeName: String,
  ref: String,
  label: Option[String] = None,
  alt: Option[String] = None,
  title: Option[String] = None,
  occurrenceIndex: Int,
  referenceKind: String,
  sourceSpan: Option[DoxReferenceSourceSpan] = None
)

object DoxReferenceExtractor {
  def extract(document: Document): Vector[DoxReference] =
    _extract_blocks(document.body.blocks).zipWithIndex.map {
      case (ref, index) => ref.copy(occurrenceIndex = index)
    }

  private def _extract_blocks(blocks: Vector[Block]): Vector[DoxReference] =
    blocks.flatMap(_extract_block)

  private def _extract_block(block: Block): Vector[DoxReference] =
    block match {
      case Section(_, title, children, _) =>
        _extract_inlines(title) ++ _extract_blocks(children)
      case Paragraph(inlines) =>
        _extract_inlines(inlines)
      case UnorderedList(items) =>
        items.flatMap(x => _extract_blocks(x.blocks))
      case OrderedList(items) =>
        items.flatMap(x => _extract_blocks(x.blocks))
      case DefinitionList(items) =>
        items.flatMap(x => _extract_inlines(x.term) ++ _extract_blocks(x.description))
      case Table(_, _, rows) =>
        rows.flatMap(row => row.cells.flatMap(_extract_inlines))
      case image: ImageRef =>
        Vector(_image_reference(image, 0))
      case Figure(image, caption, _) =>
        Vector(_image_reference(image, 0)) ++ caption.toVector.flatMap(_extract_inlines)
      case Program(_, _, _) =>
        Vector.empty
      case HorizontalRule() =>
        Vector.empty
      case Quote(blocks) =>
        _extract_blocks(blocks)
      case BlockMacro(name, target, _) if name == "link" || name == "xref" =>
        Vector(DoxReference("a", "href", target, occurrenceIndex = 0, referenceKind = name))
      case BlockMacro(_, _, _) =>
        Vector.empty
      case UnsupportedBlock(_, _) =>
        Vector.empty
    }

  private def _extract_inlines(inlines: Vector[Inline]): Vector[DoxReference] =
    inlines.flatMap(_extract_inline)

  private def _extract_inline(inline: Inline): Vector[DoxReference] =
    inline match {
      case Text(_) =>
        Vector.empty
      case Bold(inlines) =>
        _extract_inlines(inlines)
      case Italic(inlines) =>
        _extract_inlines(inlines)
      case Underline(inlines) =>
        _extract_inlines(inlines)
      case Code(_) =>
        Vector.empty
      case Pre(_) =>
        Vector.empty
      case Delete(inlines) =>
        _extract_inlines(inlines)
      case Span(_, inlines, _) =>
        _extract_inlines(inlines)
      case Hyperlink(href, label, kind, sourceSpan) =>
        Vector(DoxReference(
          elementKind = "a",
          attributeName = "href",
          ref = href,
          label = _plain_text_option(label),
          occurrenceIndex = 0,
          referenceKind = _link_kind(kind, href),
          sourceSpan = sourceSpan
        )) ++ _extract_inlines(label)
      case image: ImageRef =>
        Vector(_image_reference(image, 0))
      case InlineMacro(name, value) if name == "site" =>
        Vector(DoxReference("a", "href", value, Some(value), occurrenceIndex = 0, referenceKind = "site"))
      case InlineMacro(_, _) =>
        Vector.empty
      case StructuredToken(_, _) =>
        Vector.empty
      case I18NFragment(values) =>
        values.values.toVector.flatMap(_extract_inlines)
    }

  private def _image_reference(
    image: ImageRef,
    index: Int
  ): DoxReference =
    DoxReference(
      elementKind = "img",
      attributeName = "src",
      ref = image.uri,
      alt = image.alt,
      title = image.title,
      occurrenceIndex = index,
      referenceKind = "image",
      sourceSpan = image.sourceSpan
    )

  private def _plain_text_option(inlines: Vector[Inline]): Option[String] = {
    val text = DoxText.plainText(inlines).trim
    if (text.isEmpty) None else Some(text)
  }

  private def _link_kind(kind: LinkKind, href: String): String =
    kind match {
      case LinkKind.AutoUrl => "external-url"
      case LinkKind.Site => "site"
      case LinkKind.LegacySite => "site"
      case _ if href.startsWith("http://") || href.startsWith("https://") => "external-url"
      case _ if href.startsWith("urn:textus:") => "textus-urn"
      case _ => "link"
    }
}

object DoxText {
  def plainText(inlines: Vector[Inline]): String =
    inlines.map(plainText).mkString

  def plainText(inline: Inline): String =
    inline match {
      case Text(value) => value
      case Bold(xs) => plainText(xs)
      case Italic(xs) => plainText(xs)
      case Underline(xs) => plainText(xs)
      case Code(value) => value
      case Pre(value) => value
      case Delete(xs) => plainText(xs)
      case Span(_, xs, _) => plainText(xs)
      case Hyperlink(_, label, _, _) => plainText(label)
      case ImageRef(_, alt, _, _) => alt.getOrElse("")
      case InlineMacro(_, value) => value
      case StructuredToken(_, raw) => raw
      case I18NFragment(values) => values.values.headOption.map(plainText).getOrElse("")
    }
}
