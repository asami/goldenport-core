package org.smartdox

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.smartdox.parser.Dox2Parser
import org.smartdox.renderer.DoxHtmlRenderer

/*
 * @since   May.  4, 2026
 * @version May.  4, 2026
 * @author  ASAMI, Tomoharu
 */
class SmartDoxParserSpec extends AnyWordSpec with Matchers {
  "Dox2Parser" should {
    "parse headings, paragraphs, lists, tables, and source blocks" in {
      val doc = Dox2Parser.parse(
        """#+CAPTION: Table caption
          || name | value |
          || ---- | ----- |
          || a | 1 |
          |
          |# Section
          |
          |Paragraph with *bold* and [[https://example.com][link]].
          |
          |- first
          |- second
          |
          |#+begin_src scala
          |println("hello")
          |#+end_src
          |""".stripMargin
      )

      doc.body.blocks.collect { case x: Table => x }.head.caption.map(DoxText.plainText) shouldBe Some("Table caption")
      doc.body.blocks.collect { case x: Section => x }.head.level shouldBe 1
      doc.body.blocks.collect { case x: Section => x }.head.children.collect { case _: UnorderedList => true } should contain(true)
      doc.body.blocks.collect { case x: Section => x }.head.children.collect { case Program(Some("scala"), _, "src") => true } should contain(true)
    }

    "extract SmartDox image and link references" in {
      val source =
        """Intro [[images/a.png]] and [[https://example.com][external]].
          |
          |See [local.dox] and site:[guide/index.dox].
          |""".stripMargin
      val doc = Dox2Parser.parse(source)

      val refs = DoxReferenceExtractor.extract(doc)
      refs.map(x => x.elementKind -> x.ref) should contain("img" -> "images/a.png")
      refs.map(x => x.elementKind -> x.ref) should contain("a" -> "https://example.com")
      refs.map(x => x.referenceKind -> x.ref) should contain("site" -> "local.dox")
      refs.map(x => x.referenceKind -> x.ref) should contain("site" -> "guide/index.dox")
      val imageSpan = refs.find(_.ref == "images/a.png").flatMap(_.sourceSpan).get
      source.substring(imageSpan.node.start, imageSpan.node.end) shouldBe "[[images/a.png]]"
      source.substring(imageSpan.target.start, imageSpan.target.end) shouldBe "images/a.png"
      val linkSpan = refs.find(_.ref == "https://example.com").flatMap(_.sourceSpan).get
      source.substring(linkSpan.node.start, linkSpan.node.end) shouldBe "[[https://example.com][external]]"
      source.substring(linkSpan.target.start, linkSpan.target.end) shouldBe "https://example.com"
    }

    "keep source spans for block and multiline paragraph image references" in {
      val source =
        """[[images/block.png]]
          |
          |aaa
          |bbb [[images/inline.png]]
          |""".stripMargin
      val doc = Dox2Parser.parse(source)
      val refs = DoxReferenceExtractor.extract(doc)

      val block = refs.find(_.ref == "images/block.png").flatMap(_.sourceSpan).get
      val inline = refs.find(_.ref == "images/inline.png").flatMap(_.sourceSpan).get
      source.substring(block.node.start, block.node.end) shouldBe "[[images/block.png]]"
      source.substring(block.target.start, block.target.end) shouldBe "images/block.png"
      source.substring(inline.node.start, inline.node.end) shouldBe "[[images/inline.png]]"
      source.substring(inline.target.start, inline.target.end) shouldBe "images/inline.png"
    }

    "keep multiline XML and JSON as structured logical tokens" in {
      val doc = Dox2Parser.parse(
        """<a>
          |
          |  <b/>
          |</a>
          |
          |{
          |  "a": [
          |
          |    {"b": 1}
          |  ]
          |}
          |
          |aaa
          |bbb
          |""".stripMargin
      )

      val paragraphs = doc.body.blocks.collect { case x: Paragraph => x }
      paragraphs(0).inlines shouldBe Vector(StructuredToken("xml", "<a>\n\n  <b/>\n</a>"))
      paragraphs(1).inlines shouldBe Vector(StructuredToken("json", "{\n  \"a\": [\n\n    {\"b\": 1}\n  ]\n}"))
      DoxText.plainText(paragraphs(2).inlines) shouldBe "aaa bbb"
    }

    "not extract references from source blocks or structured tokens" in {
      val doc = Dox2Parser.parse(
        """#+begin_src text
          |[[images/source.png]]
          |#+end_src
          |
          |<a>
          |  [[images/xml.png]]
          |</a>
          |
          |{
          |  "image": "[[images/json.png]]"
          |}
          |
          |> #+begin_src text
          |> [[images/quoted-source.png]]
          |> #+end_src
          |
          |> <a>
          |>   [[images/quoted-xml.png]]
          |> </a>
          |
          |> {
          |>   "image": "[[images/quoted-json.png]]"
          |> }
          |
          |> #+begin_comment
          |> [[images/quoted-comment.png]]
          |> #+end_comment
          |""".stripMargin
      )

      DoxReferenceExtractor.extract(doc).map(_.ref) shouldBe Vector.empty
    }

    "keep original source spans for structured prose references" in {
      val source =
        """# [[images/title.png]]
          |
          |- [[images/list.png]]
          |1. [[images/ordered.png]]
          |- [[images/term.png]] :: [[images/definition.png]]
          |
          || [[images/table.png]] |
          |#+CAPTION: [[images/caption.png]]
          |[[images/figure.png]]
          |
          |> [[images/quote.png]]
          |
          |Marked *[[images/marked.png]]* but =[[images/code.png]]= and ~[[images/pre.png]]~ stay inert.
          |""".stripMargin
      val doc = Dox2Parser.parse(source)

      val refs = DoxReferenceExtractor.extract(doc)
      val expected = Vector(
        "images/title.png",
        "images/list.png",
        "images/ordered.png",
        "images/term.png",
        "images/definition.png",
        "images/table.png",
        "images/figure.png",
        "images/caption.png",
        "images/quote.png",
        "images/marked.png"
      )
      refs.map(_.ref) should contain allElementsOf expected
      refs.map(_.ref) should not contain "images/code.png"
      refs.map(_.ref) should not contain "images/pre.png"
      expected.foreach { ref =>
        val span = refs.find(_.ref == ref).flatMap(_.sourceSpan).get
        source.substring(span.node.start, span.node.end) should include (ref)
        source.substring(span.target.start, span.target.end) shouldBe ref
      }
    }

    "keep leading Markdown and legacy site links out of JSON token parsing" in {
      val doc = Dox2Parser.parse(
        """[label](https://example.com)
          |
          |[local.dox]
          |""".stripMargin
      )

      val refs = DoxReferenceExtractor.extract(doc)
      refs.map(x => x.referenceKind -> x.ref) should contain("external-url" -> "https://example.com")
      refs.map(x => x.referenceKind -> x.ref) should contain("site" -> "local.dox")
    }
  }

  "DoxHtmlRenderer" should {
    "render escaped HTML fragments" in {
      val doc = Dox2Parser.parse(
        """# Title
          |
          |Unsafe <script>alert(1)</script> and =code=.
          |
          || a | b |
          || 1 | 2 |
          |""".stripMargin
      )

      val html = DoxHtmlRenderer.renderFragment(doc)
      html should include("<h1>Title</h1>")
      html should include("&lt;script&gt;alert(1)&lt;/script&gt;")
      html should include("<code>code</code>")
      html should include("<table")
    }

    "render structured tokens escaped without splitting logical lines" in {
      val doc = Dox2Parser.parse(
        """<a>
          |  <b/>
          |</a>
          |""".stripMargin
      )

      val html = DoxHtmlRenderer.renderFragment(doc)
      html should include("""<code class="language-xml">&lt;a&gt;""")
      html should include("&lt;b/&gt;")
    }
  }
}
