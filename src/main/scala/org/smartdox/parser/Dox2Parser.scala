package org.smartdox.parser

import scala.util.matching.Regex
import org.goldenport.Consequence
import org.smartdox._

/*
 * Small SmartDox parser for CNCF content handling.
 *
 * It accepts the document grammar subset needed for safe content rendering:
 * headings, paragraphs, lists, pipe tables, image/link syntax, source blocks,
 * comments, and inert block macros. Executable SmartDox site features are not
 * evaluated here.
 *
 * @since   May.  4, 2026
 * @version May.  4, 2026
 * @author  ASAMI, Tomoharu
 */
final class Dox2Parser {
  def parse(text: String): Document =
    SmartDoxParser.parse(text)

  def parseC(text: String): Consequence[Document] =
    Consequence.success(parse(text))
}

object Dox2Parser {
  def parse(text: String): Document =
    SmartDoxParser.parse(text)

  def parseC(text: String): Consequence[Document] =
    Consequence.success(parse(text))
}

private final case class MappedText(
  text: String,
  offsets: Vector[Int]
) {
  def sourceOffset(index: Int): Option[Int] =
    offsets.lift(index)
}

object SmartDoxParser {
  private sealed trait Item
  private final case class Heading(level: Int, title: String, style: SectionStyle) extends Item
  private final case class BlockItem(block: Block) extends Item
  private final case class Annotation(caption: Option[String] = None, label: Option[String] = None) {
    def clear: Annotation =
      Annotation()
  }

  private final case class State(
    index: Int,
    items: Vector[Item],
    annotation: Annotation
  ) {
    def next: State =
      copy(index = index + 1)

    def add(item: Item): State =
      copy(items = items :+ item, annotation = annotation.clear)

    def addBlock(block: Block): State =
      add(BlockItem(block))

    def addHeading(level: Int, title: String, style: SectionStyle): State =
      add(Heading(level, title, style))
  }

  def parse(text: String): Document = {
    val lines = _lines(text)
    val state = _parse_lines(lines, State(0, Vector.empty, Annotation()))
    val blocks = _build_blocks(state.items, 0)._1
    val (head, body) = _extract_head(blocks)
    Document(head, Body(body))
  }

  def parseInlines(text: String): Vector[Inline] =
    InlineParser.parse(text)

  private def _lines(text: String): Vector[String] =
    Option(text).getOrElse("").replace("\r\n", "\n").replace('\r', '\n').split("\n", -1).toVector

  private def _parse_lines(
    lines: Vector[String],
    initial: State
  ): State = {
    var state = initial
    while (state.index < lines.length) {
      val line = lines(state.index)
      val trim = line.trim
      if (trim.isEmpty) {
        state = state.next
      } else if (_is_comment_block_start(trim)) {
        state = state.copy(index = _skip_until(lines, state.index + 1, "#+end_comment"))
      } else if (_is_line_comment(trim)) {
        state = state.next
      } else {
        _caption(trim) match {
          case Some(caption) =>
            state = state.copy(index = state.index + 1, annotation = state.annotation.copy(caption = Some(caption)))
          case None =>
            _label(trim) match {
              case Some(label) =>
                state = state.copy(index = state.index + 1, annotation = state.annotation.copy(label = Some(label)))
              case None =>
                state = _parse_block(lines, state)
            }
        }
      }
    }
    state
  }

  private def _parse_block(
    lines: Vector[String],
    state: State
  ): State = {
    val line = lines(state.index)
    val trim = line.trim
    _org_heading(trim) match {
      case Some((level, title)) =>
        state.addHeading(level, title, SectionStyle.Org).copy(index = state.index + 1)
      case None =>
        _atx_heading(trim) match {
          case Some((level, title)) =>
            state.addHeading(level, title, SectionStyle.Markdown).copy(index = state.index + 1)
          case None =>
            _setext_heading(lines, state.index) match {
              case Some((level, title)) =>
                state.addHeading(level, title, SectionStyle.Setext).copy(index = state.index + 2)
              case None =>
                if (_is_program_start(trim))
                  _parse_program(lines, state)
                else if (_is_horizontal_rule(trim))
                  state.addBlock(HorizontalRule()).copy(index = state.index + 1)
                else if (_is_pipe_table(trim))
                  _parse_table(lines, state)
                else if (_is_image_line(trim))
                  _parse_image(lines, state)
                else if (_is_block_macro(trim))
                  _parse_block_macro(lines, state)
                else if (_is_quote_line(trim))
                  _parse_quote(lines, state)
                else if (_is_definition_list(trim))
                  _parse_definition_list(lines, state)
                else if (_is_unordered_list(trim))
                  _parse_unordered_list(lines, state)
                else if (_is_ordered_list(trim))
                  _parse_ordered_list(lines, state)
                else
                  _parse_paragraph(lines, state)
            }
        }
    }
  }

  private def _parse_program(
    lines: Vector[String],
    state: State
  ): State = {
    val trim = lines(state.index).trim
    val ProgramStart(kind, language) = trim match {
      case BeginSrcPattern(lang) => ProgramStart("src", Option(lang).map(_.trim).filter(_.nonEmpty))
      case BeginExamplePattern() => ProgramStart("example", None)
      case BeginAnyPattern(name) => ProgramStart(name.toLowerCase, None)
      case _ => ProgramStart("verbatim", None)
    }
    val end = s"#+end_$kind"
    val buf = Vector.newBuilder[String]
    var i = state.index + 1
    var done = false
    while (i < lines.length && !done) {
      if (lines(i).trim.equalsIgnoreCase(end))
        done = true
      else {
        buf += lines(i)
        i += 1
      }
    }
    val next = if (done) i + 1 else i
    state.addBlock(Program(language, buf.result().mkString("\n"), kind)).copy(index = next)
  }

  private def _parse_table(
    lines: Vector[String],
    state: State
  ): State = {
    val rows = Vector.newBuilder[TableRow]
    var i = state.index
    while (i < lines.length && _is_pipe_table(lines(i).trim)) {
      val trim = lines(i).trim
      rows += TableRow(_table_cells(trim).map(InlineParser.parse), _is_table_separator(trim))
      i += 1
    }
    val caption = state.annotation.caption.map(InlineParser.parse)
    val label = state.annotation.label
    state.addBlock(Table(caption, label, rows.result())).copy(index = i)
  }

  private def _parse_image(
    lines: Vector[String],
    state: State
  ): State = {
    val line = lines(state.index)
    val trimStart = _trim_start(line)
    val trim = line.trim
    val matched = ImageLinePattern.findFirstMatchIn(trim)
    val target = matched.map(_.group(1).trim).getOrElse(trim)
    val sourceSpan = matched.flatMap { m =>
      val nodeStart = _line_start(lines, state.index) + trimStart + m.start
      val nodeEnd = _line_start(lines, state.index) + trimStart + m.end
      val rawTarget = m.group(1)
      val targetLeading = rawTarget.indexWhere(!_.isWhitespace) match {
        case -1 => 0
        case n => n
      }
      val targetTrailing = rawTarget.reverse.indexWhere(!_.isWhitespace) match {
        case -1 => rawTarget.length
        case n => rawTarget.length - n
      }
      Some(DoxReferenceSourceSpan(
        SourceSpan(nodeStart, nodeEnd),
        SourceSpan(
          _line_start(lines, state.index) + trimStart + m.start(1) + targetLeading,
          _line_start(lines, state.index) + trimStart + m.start(1) + targetTrailing
        )
      ))
    }
    val alt = state.annotation.caption
    val image = ImageRef(target, alt, sourceSpan = sourceSpan)
    val block = if (state.annotation.caption.isDefined || state.annotation.label.isDefined)
      Figure(image, state.annotation.caption.map(InlineParser.parse), state.annotation.label)
    else
      image
    state.addBlock(block).copy(index = state.index + 1)
  }

  private def _parse_block_macro(
    lines: Vector[String],
    state: State
  ): State = {
    val trim = lines(state.index).trim
    val block = trim match {
      case BlockMacroPattern(name, target, attrs) =>
        BlockMacro(name.toLowerCase, target.trim, _attributes(Option(attrs).getOrElse("")))
      case _ =>
        UnsupportedBlock("macro", trim)
    }
    state.addBlock(block).copy(index = state.index + 1)
  }

  private def _parse_quote(
    lines: Vector[String],
    state: State
  ): State = {
    val buf = Vector.newBuilder[String]
    var i = state.index
    while (i < lines.length && _is_quote_line(lines(i).trim)) {
      val trim = lines(i).trim
      buf += trim.dropWhile(_ == '>').trim
      i += 1
    }
    val nested = parse(buf.result().mkString("\n")).body.blocks
    state.addBlock(Quote(nested)).copy(index = i)
  }

  private def _parse_definition_list(
    lines: Vector[String],
    state: State
  ): State = {
    val items = Vector.newBuilder[DefinitionItem]
    var i = state.index
    while (i < lines.length && _is_definition_list(lines(i).trim)) {
      val DefinitionListPattern(term, desc) = lines(i).trim: @unchecked
      items += DefinitionItem(
        InlineParser.parse(term.trim),
        Vector(Paragraph(InlineParser.parse(desc.trim)))
      )
      i += 1
    }
    state.addBlock(DefinitionList(items.result())).copy(index = i)
  }

  private def _parse_unordered_list(
    lines: Vector[String],
    state: State
  ): State = {
    val items = Vector.newBuilder[ListItem]
    var i = state.index
    while (i < lines.length && _is_unordered_list(lines(i).trim) && !_is_definition_list(lines(i).trim)) {
      val UnorderedListPattern(text) = lines(i).trim: @unchecked
      items += ListItem(Vector(Paragraph(InlineParser.parse(text.trim))))
      i += 1
    }
    state.addBlock(UnorderedList(items.result())).copy(index = i)
  }

  private def _parse_ordered_list(
    lines: Vector[String],
    state: State
  ): State = {
    val items = Vector.newBuilder[ListItem]
    var i = state.index
    while (i < lines.length && _is_ordered_list(lines(i).trim)) {
      val OrderedListPattern(text) = lines(i).trim: @unchecked
      items += ListItem(Vector(Paragraph(InlineParser.parse(text.trim))))
      i += 1
    }
    state.addBlock(OrderedList(items.result())).copy(index = i)
  }

  private def _parse_paragraph(
    lines: Vector[String],
    state: State
  ): State =
    _parse_structured_token(lines, state).getOrElse {
    val buf = Vector.newBuilder[MappedText]
    var i = state.index
    var done = false
    while (i < lines.length && !done) {
      val trim = lines(i).trim
      if (trim.isEmpty || _is_special_line(lines, i))
        done = true
      else {
        if (!_is_line_comment(trim))
          buf += _mapped_trimmed_line(lines, i)
        i += 1
      }
    }
    state.addBlock(Paragraph(InlineParser.parseMapped(_join_mapped_text(buf.result(), " ")))).copy(index = i)
  }

  private def _parse_structured_token(
    lines: Vector[String],
    state: State
  ): Option[State] = {
    val line = lines(state.index)
    val trim = line.trim
    if (_looks_like_xml_start(trim))
      Some(_parse_xml_token(lines, state))
    else if (_looks_like_json_start(trim))
      Some(_parse_json_token(lines, state))
    else
      None
  }

  private def _parse_xml_token(
    lines: Vector[String],
    state: State
  ): State = {
    val buf = Vector.newBuilder[String]
    var i = state.index
    var stack = Vector.empty[String]
    var seenTag = false
    var done = false
    while (i < lines.length && !done) {
      val line = lines(i)
      buf += line
      val parsed = _xml_stack(line, stack)
      stack = parsed._1
      seenTag = seenTag || parsed._2
      i += 1
      done = seenTag && stack.isEmpty
    }
    state.addBlock(Paragraph(Vector(StructuredToken("xml", buf.result().mkString("\n"))))).copy(index = i)
  }

  private def _parse_json_token(
    lines: Vector[String],
    state: State
  ): State = {
    val buf = Vector.newBuilder[String]
    var i = state.index
    var balance = 0
    var inString = false
    var escaped = false
    var done = false
    while (i < lines.length && !done) {
      val line = lines(i)
      buf += line
      line.foreach { ch =>
        if (escaped) {
          escaped = false
        } else if (inString) {
          if (ch == '\\')
            escaped = true
          else if (ch == '"')
            inString = false
        } else {
          ch match {
            case '"' => inString = true
            case '{' | '[' => balance += 1
            case '}' | ']' => balance = math.max(0, balance - 1)
            case _ =>
          }
        }
      }
      i += 1
      done = balance == 0 && !inString
    }
    state.addBlock(Paragraph(Vector(StructuredToken("json", buf.result().mkString("\n"))))).copy(index = i)
  }

  private def _build_blocks(
    items: Vector[Item],
    level: Int,
    start: Int = 0
  ): (Vector[Block], Int) = {
    val blocks = Vector.newBuilder[Block]
    var i = start
    while (i < items.length) {
      items(i) match {
        case Heading(l, _, _) if l <= level =>
          return (blocks.result(), i)
        case Heading(l, title, style) =>
          val (children, next) = _build_blocks(items, l, i + 1)
          blocks += Section(l, InlineParser.parse(title), children, style)
          i = next
        case BlockItem(block) =>
          blocks += block
          i += 1
      }
    }
    (blocks.result(), i)
  }

  private def _extract_head(
    blocks: Vector[Block]
  ): (DocumentHead, Vector[Block]) =
    blocks.headOption match {
      case Some(section @ Section(1, title, children, _)) if DoxText.plainText(title).trim.equalsIgnoreCase("HEAD") =>
        (DocumentHead(_head_metadata(children)), blocks.tail)
      case _ =>
        (DocumentHead.empty, blocks)
    }

  private def _head_metadata(blocks: Vector[Block]): Map[String, String] =
    blocks.collect {
      case Paragraph(inlines) =>
        DoxText.plainText(inlines).split("\\s+").toVector
    }.flatten.flatMap { token =>
      token.indexOf('=') match {
        case i if i > 0 && i < token.length - 1 =>
          Some(token.substring(0, i) -> token.substring(i + 1))
        case _ =>
          None
      }
    }.toMap

  private def _is_special_line(lines: Vector[String], index: Int): Boolean = {
    val trim = lines(index).trim
    _caption(trim).isDefined ||
      _label(trim).isDefined ||
      _org_heading(trim).isDefined ||
      _atx_heading(trim).isDefined ||
      _setext_heading(lines, index).isDefined ||
      _is_comment_block_start(trim) ||
      _is_program_start(trim) ||
      _is_horizontal_rule(trim) ||
      _is_pipe_table(trim) ||
      _is_image_line(trim) ||
      _is_block_macro(trim) ||
      _is_quote_line(trim) ||
      _is_definition_list(trim) ||
      _is_unordered_list(trim) ||
      _is_ordered_list(trim)
  }

  private def _skip_until(lines: Vector[String], start: Int, end: String): Int = {
    var i = start
    while (i < lines.length && !lines(i).trim.equalsIgnoreCase(end))
      i += 1
    if (i < lines.length) i + 1 else i
  }

  private def _line_start(lines: Vector[String], index: Int): Int =
    lines.take(index).foldLeft(0)((z, line) => z + line.length + 1)

  private def _trim_start(line: String): Int =
    line.indexWhere(!_.isWhitespace) match {
      case -1 => line.length
      case n => n
    }

  private def _trim_end(line: String): Int =
    line.lastIndexWhere(!_.isWhitespace) match {
      case -1 => 0
      case n => n + 1
    }

  private def _mapped_trimmed_line(
    lines: Vector[String],
    index: Int
  ): MappedText = {
    val line = lines(index)
    val start = _trim_start(line)
    val end = _trim_end(line)
    val text = if (end >= start) line.substring(start, end) else ""
    MappedText(
      text,
      (start until end).toVector.map(_line_start(lines, index) + _)
    )
  }

  private def _join_mapped_text(
    values: Vector[MappedText],
    separator: String
  ): MappedText = {
    val text = new StringBuilder
    val offsets = Vector.newBuilder[Int]
    values.zipWithIndex.foreach {
      case (value, index) =>
        if (index > 0) {
          val previousEnd = values(index - 1).offsets.lastOption.map(_ + 1).getOrElse(0)
          separator.foreach { ch =>
            text.append(ch)
            offsets += previousEnd
          }
        }
        text.append(value.text)
        offsets ++= value.offsets
    }
    MappedText(text.toString, offsets.result())
  }

  private def _caption(line: String): Option[String] =
    line match {
      case CaptionPattern(value) => Some(value.trim)
      case _ => None
    }

  private def _label(line: String): Option[String] =
    line match {
      case LabelPattern(value) => Some(value.trim)
      case _ => None
    }

  private def _org_heading(line: String): Option[(Int, String)] =
    line match {
      case OrgHeadingPattern(stars, title) => Some(stars.length -> title.trim)
      case _ => None
    }

  private def _atx_heading(line: String): Option[(Int, String)] =
    line match {
      case AtxHeadingPattern(sharps, title) => Some(sharps.length -> title.replaceAll("\\s+#+\\s*$", "").trim)
      case _ => None
    }

  private def _setext_heading(lines: Vector[String], index: Int): Option[(Int, String)] =
    if (index + 1 >= lines.length)
      None
    else {
      val title = lines(index).trim
      val underline = lines(index + 1).trim
      if (title.nonEmpty && underline.matches("=+"))
        Some(1 -> title)
      else if (title.nonEmpty && underline.matches("-{3,}"))
        Some(2 -> title)
      else
        None
    }

  private def _is_comment_block_start(line: String): Boolean =
    line.equalsIgnoreCase("#+begin_comment")

  private def _is_line_comment(line: String): Boolean =
    line.startsWith("#") &&
      !line.startsWith("#+") &&
      _atx_heading(line).isEmpty

  private def _is_program_start(line: String): Boolean =
    BeginSrcPattern.matches(line) || BeginExamplePattern.matches(line) || BeginAnyPattern.matches(line)

  private def _is_horizontal_rule(line: String): Boolean =
    line == "---" || line == "----"

  private def _is_pipe_table(line: String): Boolean =
    line.startsWith("|") && line.endsWith("|") && line.length >= 2

  private def _is_table_separator(line: String): Boolean =
    line.filterNot(_ == '|').forall(ch => ch == '-' || ch == ':' || ch.isWhitespace)

  private def _table_cells(line: String): Vector[String] =
    line.stripPrefix("|").stripSuffix("|").split("\\|", -1).toVector.map(_.trim)

  private def _is_image_line(line: String): Boolean =
    ImageLinePattern.matches(line) && _is_image_path(ImageLinePattern.findFirstMatchIn(line).map(_.group(1)).getOrElse(""))

  private def _is_image_path(value: String): Boolean = {
    val lower = value.toLowerCase(java.util.Locale.ROOT)
    lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
      lower.endsWith(".gif") || lower.endsWith(".webp") || lower.startsWith("urn:textus:image:") ||
      lower.startsWith("urn:textus:blob:") || lower.contains("/web/blob/content/")
  }

  private def _is_block_macro(line: String): Boolean =
    BlockMacroPattern.matches(line)

  private def _is_quote_line(line: String): Boolean =
    line.startsWith(">")

  private def _is_definition_list(line: String): Boolean =
    DefinitionListPattern.matches(line)

  private def _is_unordered_list(line: String): Boolean =
    UnorderedListPattern.matches(line)

  private def _is_ordered_list(line: String): Boolean =
    OrderedListPattern.matches(line)

  private def _looks_like_xml_start(line: String): Boolean =
    XmlStartPattern.findFirstIn(line).isDefined

  private def _looks_like_json_start(line: String): Boolean =
    line.startsWith("{") || _looks_like_json_array_start(line)

  private def _looks_like_json_array_start(line: String): Boolean =
    if (!line.startsWith("[") || line.startsWith("[["))
      false
    else {
      line.drop(1).dropWhile(_.isWhitespace).headOption.exists {
        case '{' | '[' | '"' | ']' | '-' => true
        case ch if ch.isDigit => true
        case ch => ch == 't' || ch == 'f' || ch == 'n'
      }
    }

  private def _xml_stack(
    line: String,
    stack: Vector[String]
  ): (Vector[String], Boolean) = {
    var current = stack
    var seen = false
    XmlTagPattern.findAllMatchIn(line).foreach { m =>
      val raw = m.matched
      if (!raw.startsWith("<?") && !raw.startsWith("<!")) {
        seen = true
        val closing = Option(m.group(1)).contains("/")
        val name = m.group(2)
        val body = Option(m.group(3)).getOrElse("")
        val selfClosing = body.trim.endsWith("/") || raw.endsWith("/>")
        if (closing) {
          current = current.reverse.dropWhile(_ != name).drop(1).reverse
        } else if (!selfClosing) {
          current = current :+ name
        }
      }
    }
    (current, seen)
  }

  private def _attributes(value: String): Map[String, String] =
    value.split(",").toVector.flatMap { entry =>
      val p = entry.trim
      val i = p.indexOf('=')
      if (i > 0 && i < p.length - 1)
        Some(p.substring(0, i).trim -> p.substring(i + 1).trim)
      else
        None
    }.toMap

  private final case class ProgramStart(kind: String, language: Option[String])

  private val CaptionPattern: Regex = """(?i)#\+caption:\s*(.*)""".r
  private val LabelPattern: Regex = """(?i)#\+label:\s*(.*)""".r
  private val OrgHeadingPattern: Regex = """^(\*+)\s+(.+)$""".r
  private val AtxHeadingPattern: Regex = """^(#{1,6})\s+(.+)$""".r
  private val BeginSrcPattern: Regex = """(?i)#\+begin_src(?:\s+(.+))?""".r
  private val BeginExamplePattern: Regex = """(?i)#\+begin_example\s*""".r
  private val BeginAnyPattern: Regex = """(?i)#\+begin_([A-Za-z0-9_-]+).*""".r
  private val ImageLinePattern: Regex = """^\[\[([^\]]+)\]\]$""".r
  private val BlockMacroPattern: Regex = """^([A-Za-z][A-Za-z0-9_-]*)::([^\[]+)(?:\[(.*)\])?$""".r
  private val DefinitionListPattern: Regex = """^-\s+(.+?)\s+::\s+(.+)$""".r
  private val UnorderedListPattern: Regex = """^-\s+(.+)$""".r
  private val OrderedListPattern: Regex = """^\d+\.\s+(.+)$""".r
  private val XmlStartPattern: Regex = """^<[A-Za-z_][A-Za-z0-9_.:-]*(?:\s|>|/)""".r
  private val XmlTagPattern: Regex = """<(/?)([A-Za-z_][A-Za-z0-9_.:-]*)([^>]*)>""".r
}

private object InlineParser {
  private val OrgLinkPattern: Regex = """\[\[([^\]]+?)(?:\]\[([^\]]+))?\]\]""".r
  private val MarkdownLinkPattern: Regex = """\[([^\]]+)\]\(([^\s\)]+)\)""".r
  private val SitePattern: Regex = """site:\[([^\]]+)\]""".r
  private val LegacySitePattern: Regex = """\[([A-Za-z0-9_./:-]+\.dox)\]""".r
  private val AutoUrlPattern: Regex = """https?://[^\s<>\]\)]+""".r

  def parse(text: String): Vector[Inline] =
    _parse_links(Option(text).getOrElse(""), _ => None)

  def parseMapped(text: MappedText): Vector[Inline] =
    _parse_links(text.text, index => text.sourceOffset(index))

  private def _parse_links(
    text: String,
    offsetAt: Int => Option[Int]
  ): Vector[Inline] = {
    val out = Vector.newBuilder[Inline]
    var consumed = 0
    var rest = text
    while (rest.nonEmpty) {
      _first_match(rest, index => offsetAt(consumed + index)) match {
        case Some(found) =>
          if (found.start > 0)
            out ++= _parse_marks(rest.substring(0, found.start))
          out += found.inline
          consumed += found.end
          rest = rest.substring(found.end)
        case None =>
          out ++= _parse_marks(rest)
          rest = ""
      }
    }
    out.result().filterNot {
      case Text(value) => value.isEmpty
      case _ => false
    }
  }

  private def _first_match(
    text: String,
    offsetAt: Int => Option[Int]
  ): Option[Found] = {
    val matches = Vector(
      OrgLinkPattern.findFirstMatchIn(text).map { m =>
        val uri = m.group(1)
        val label = Option(m.group(2)).map(parse).getOrElse(Vector.empty)
        val sourceSpan = _reference_span(m.start, m.end, m.start(1), m.end(1), offsetAt)
        val inline =
          if (label.isEmpty && _is_image_path(uri))
            ImageRef(uri, sourceSpan = sourceSpan)
          else
            Hyperlink(uri, if (label.isEmpty) Vector(Text(uri)) else label, LinkKind.Org, sourceSpan)
        Found(m.start, m.end, inline)
      },
      MarkdownLinkPattern.findFirstMatchIn(text).map { m =>
        Found(m.start, m.end, Hyperlink(m.group(2), parse(m.group(1)), LinkKind.Markdown, _reference_span(m.start, m.end, m.start(2), m.end(2), offsetAt)))
      },
      SitePattern.findFirstMatchIn(text).map { m =>
        Found(m.start, m.end, Hyperlink(m.group(1), Vector(Text(m.group(1))), LinkKind.Site, _reference_span(m.start, m.end, m.start(1), m.end(1), offsetAt)))
      },
      LegacySitePattern.findFirstMatchIn(text).map { m =>
        Found(m.start, m.end, Hyperlink(m.group(1), Vector(Text(m.group(1))), LinkKind.LegacySite, _reference_span(m.start, m.end, m.start(1), m.end(1), offsetAt)))
      },
      AutoUrlPattern.findFirstMatchIn(text).map { m =>
        val url = m.matched
        Found(m.start, m.end, Hyperlink(url, Vector(Text(url)), LinkKind.AutoUrl, _reference_span(m.start, m.end, m.start, m.end, offsetAt)))
      }
    ).flatten
    matches.sortBy(_.start).headOption
  }

  private def _reference_span(
    nodeStart: Int,
    nodeEnd: Int,
    targetStart: Int,
    targetEnd: Int,
    offsetAt: Int => Option[Int]
  ): Option[DoxReferenceSourceSpan] =
    for {
      ns <- _source_span(nodeStart, nodeEnd, offsetAt)
      ts <- _source_span(targetStart, targetEnd, offsetAt)
    } yield DoxReferenceSourceSpan(ns, ts)

  private def _source_span(
    start: Int,
    end: Int,
    offsetAt: Int => Option[Int]
  ): Option[SourceSpan] =
    if (end <= start)
      None
    else
      for {
        s <- offsetAt(start)
        last <- offsetAt(end - 1)
      } yield SourceSpan(s, last + 1)

  private def _parse_marks(text: String): Vector[Inline] =
    if (text.isEmpty)
      Vector.empty
    else
      _first_mark(text) match {
        case Some((start, end, marker)) =>
          val before = text.substring(0, start)
          val inner = text.substring(start + 1, end)
          val after = text.substring(end + 1)
          _text(before) ++ Vector(_marked(marker, inner)) ++ _parse_marks(after)
        case None =>
          _text(text)
      }

  private def _first_mark(text: String): Option[(Int, Int, Char)] =
    Vector('*', '/', '_', '=', '~', '+').flatMap { marker =>
      val start = text.indexOf(marker)
      if (start >= 0) {
        val end = text.indexOf(marker, start + 1)
        if (end > start + 1) Some((start, end, marker)) else None
      } else {
        None
      }
    }.sortBy(_._1).headOption

  private def _marked(marker: Char, text: String): Inline =
    marker match {
      case '*' => Bold(parse(text))
      case '/' => Italic(parse(text))
      case '_' => Underline(parse(text))
      case '=' => Code(text)
      case '~' => Pre(text)
      case '+' => Delete(parse(text))
      case _ => Text(text)
    }

  private def _text(text: String): Vector[Inline] =
    if (text.isEmpty) Vector.empty else Vector(Text(text))

  private def _is_image_path(value: String): Boolean = {
    val lower = value.toLowerCase(java.util.Locale.ROOT)
    lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
      lower.endsWith(".gif") || lower.endsWith(".webp") || lower.startsWith("urn:textus:image:") ||
      lower.startsWith("urn:textus:blob:") || lower.contains("/web/blob/content/")
  }

  private final case class Found(start: Int, end: Int, inline: Inline)
}
