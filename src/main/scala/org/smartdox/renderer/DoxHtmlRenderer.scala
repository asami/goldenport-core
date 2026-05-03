package org.smartdox.renderer

import org.smartdox._

/*
 * Safe SmartDox HTML fragment renderer.
 *
 * Text is escaped by default. Executable or site-level constructs are rendered
 * as inert placeholders so content rendering never performs side effects.
 *
 * @since   May.  4, 2026
 * @version May.  4, 2026
 * @author  ASAMI, Tomoharu
 */
object DoxHtmlRenderer {
  def renderFragment(document: Document): String =
    document.body.blocks.map(renderBlock).mkString

  def renderBlock(block: Block): String =
    block match {
      case Section(level, title, children, _) =>
        val h = math.max(1, math.min(6, level))
        s"<section><h$h>${renderInlines(title)}</h$h>${children.map(renderBlock).mkString}</section>"
      case Paragraph(inlines) =>
        s"<p>${renderInlines(inlines)}</p>"
      case UnorderedList(items) =>
        s"<ul>${items.map(renderListItem).mkString}</ul>"
      case OrderedList(items) =>
        s"<ol>${items.map(renderListItem).mkString}</ol>"
      case DefinitionList(items) =>
        s"<dl>${items.map(renderDefinitionItem).mkString}</dl>"
      case table: Table =>
        renderTable(table)
      case image: ImageRef =>
        renderImage(image)
      case Figure(image, caption, label) =>
        val id = label.map(x => s""" id="${escapeAttribute(x)}"""").getOrElse("")
        val cap = caption.map(x => s"<figcaption>${renderInlines(x)}</figcaption>").getOrElse("")
        s"<figure$id>${renderImage(image)}$cap</figure>"
      case Program(language, body, _) =>
        val lang = language.map(x => s""" class="language-${escapeAttribute(x)}"""").getOrElse("")
        s"<pre><code$lang>${escape(body)}</code></pre>"
      case HorizontalRule() =>
        "<hr>"
      case Quote(blocks) =>
        s"<blockquote>${blocks.map(renderBlock).mkString}</blockquote>"
      case BlockMacro(name, target, _) if name == "link" || name == "xref" =>
        val href = escapeAttribute(target)
        s"""<p><a href="$href">${escape(target)}</a></p>"""
      case BlockMacro(name, target, _) =>
        s"""<div class="smartdox-placeholder" data-smartdox-macro="${escapeAttribute(name)}">${escape(target)}</div>"""
      case UnsupportedBlock(name, source) =>
        s"""<div class="smartdox-placeholder" data-smartdox-unsupported="${escapeAttribute(name)}">${escape(source)}</div>"""
    }

  def renderInlines(inlines: Vector[Inline]): String =
    inlines.map(renderInline).mkString

  def renderInline(inline: Inline): String =
    inline match {
      case Text(value) =>
        escape(value)
      case Bold(inlines) =>
        s"<strong>${renderInlines(inlines)}</strong>"
      case Italic(inlines) =>
        s"<em>${renderInlines(inlines)}</em>"
      case Underline(inlines) =>
        s"<u>${renderInlines(inlines)}</u>"
      case Code(value) =>
        s"<code>${escape(value)}</code>"
      case Pre(value) =>
        s"<code>${escape(value)}</code>"
      case Delete(inlines) =>
        s"<del>${renderInlines(inlines)}</del>"
      case Span(tag, inlines, attributes) =>
        val safeTag = _safe_tag(tag)
        val attrs = attributes.toVector
          .filter { case (k, _) => _safe_attribute(k) }
          .map { case (k, v) => s""" ${escapeAttribute(k)}="${escapeAttribute(v)}"""" }
          .mkString
        s"<$safeTag$attrs>${renderInlines(inlines)}</$safeTag>"
      case Hyperlink(href, label, _) =>
        val text = if (label.isEmpty) escape(href) else renderInlines(label)
        s"""<a href="${escapeAttribute(href)}">$text</a>"""
      case image: ImageRef =>
        renderImage(image)
      case InlineMacro(name, value) if name == "site" =>
        s"""<a href="${escapeAttribute(value)}">${escape(value)}</a>"""
      case InlineMacro(name, value) =>
        s"""<span class="smartdox-placeholder" data-smartdox-macro="${escapeAttribute(name)}">${escape(value)}</span>"""
      case StructuredToken(format, raw) =>
        s"""<code class="language-${escapeAttribute(format)}">${escape(raw)}</code>"""
      case I18NFragment(values) =>
        values.values.headOption.map(renderInlines).getOrElse("")
    }

  def renderImage(image: ImageRef): String = {
    val alt = image.alt.map(x => s""" alt="${escapeAttribute(x)}"""").getOrElse("")
    val title = image.title.map(x => s""" title="${escapeAttribute(x)}"""").getOrElse("")
    s"""<img src="${escapeAttribute(image.uri)}"$alt$title>"""
  }

  def renderTable(table: Table): String = {
    val caption = table.caption.map(x => s"<caption>${renderInlines(x)}</caption>").getOrElse("")
    val rows = table.rows.filterNot(_.separator)
    val body = rows.map { row =>
      s"<tr>${row.cells.map(cell => s"<td>${renderInlines(cell)}</td>").mkString}</tr>"
    }.mkString
    val id = table.label.map(x => s""" id="${escapeAttribute(x)}"""").getOrElse("")
    s"<table$id>$caption<tbody>$body</tbody></table>"
  }

  def renderListItem(item: ListItem): String =
    s"<li>${item.blocks.map(renderBlock).mkString}</li>"

  def renderDefinitionItem(item: DefinitionItem): String =
    s"<dt>${renderInlines(item.term)}</dt><dd>${item.description.map(renderBlock).mkString}</dd>"

  def escape(value: String): String =
    value.flatMap {
      case '&' => "&amp;"
      case '<' => "&lt;"
      case '>' => "&gt;"
      case '"' => "&quot;"
      case '\'' => "&#39;"
      case c => c.toString
    }

  def escapeAttribute(value: String): String =
    escape(value)

  private def _safe_tag(value: String): String =
    value.toLowerCase(java.util.Locale.ROOT) match {
      case "b" => "strong"
      case "strong" => "strong"
      case "i" => "em"
      case "em" => "em"
      case "u" => "u"
      case "code" => "code"
      case "pre" => "code"
      case "del" => "del"
      case "span" => "span"
      case _ => "span"
    }

  private def _safe_attribute(value: String): Boolean =
    value == "class" || value == "title" || value.startsWith("data-")
}
