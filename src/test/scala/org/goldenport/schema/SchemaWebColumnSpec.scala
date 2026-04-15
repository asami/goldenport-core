package org.goldenport.schema

import org.goldenport.value.BaseContent
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Apr. 16, 2026
 * @version Apr. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class SchemaWebColumnSpec extends AnyWordSpec with Matchers {
  "Schema Web column metadata" should {
    "carry portable Web hints without replacing the value domain" in {
      val column = Column(
        BaseContent.simple("body"),
        ValueDomain(datatype = XString, multiplicity = Multiplicity.One),
        web = WebColumn(
          controlType = Some("textarea"),
          required = Some(true),
          placeholder = Some("message"),
          help = Some("Write the notice body.")
        )
      )

      column.name.value shouldBe "body"
      column.domain.datatype shouldBe XString
      column.domain.multiplicity shouldBe Multiplicity.One
      column.web.controlType shouldBe Some("textarea")
      column.web.required shouldBe Some(true)
      column.web.placeholder shouldBe Some("message")
      column.web.help shouldBe Some("Write the notice body.")
    }

    "carry validation hints through Web column metadata" in {
      val hints = WebValidationHints(
        minLength = Some(1),
        maxLength = Some(200),
        pattern = Some("^[A-Z0-9-]+$")
      )
      val column = Column(
        BaseContent.simple("code"),
        ValueDomain(datatype = XString, multiplicity = Multiplicity.One),
        web = WebColumn(validation = hints)
      )

      column.web.validation.minLength shouldBe Some(1)
      column.web.validation.maxLength shouldBe Some(200)
      column.web.validation.pattern shouldBe Some("^[A-Z0-9-]+$")
      column.web.validation.isEmpty shouldBe false
      WebValidationHints.empty.isEmpty shouldBe true
    }

    "treat the default Web column metadata as empty" in {
      Column(BaseContent.simple("title"), ValueDomain()).web.isEmpty shouldBe true
      WebColumn(controlType = Some("text")).isEmpty shouldBe false
      WebColumn(validation = WebValidationHints(min = Some(BigDecimal(0)))).isEmpty shouldBe false
    }
  }
}
