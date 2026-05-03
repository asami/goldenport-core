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
      val doc = Dox2Parser.parse(
        """Intro [[images/a.png]] and [[https://example.com][external]].
          |
          |See [local.dox] and site:[guide/index.dox].
          |""".stripMargin
      )

      val refs = DoxReferenceExtractor.extract(doc)
      refs.map(x => x.elementKind -> x.ref) should contain("img" -> "images/a.png")
      refs.map(x => x.elementKind -> x.ref) should contain("a" -> "https://example.com")
      refs.map(x => x.referenceKind -> x.ref) should contain("site" -> "local.dox")
      refs.map(x => x.referenceKind -> x.ref) should contain("site" -> "guide/index.dox")
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
