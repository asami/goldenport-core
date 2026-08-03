package org.goldenport.configuration

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Aug.  2, 2026
 * @version Aug.  2, 2026
 * @author  ASAMI, Tomoharu
 */
final class ConfigurationDocumentSpec
  extends AnyWordSpec
    with Matchers
    with GivenWhenThen {
  private val _e1 = afterWord(
    "in spec:phase-55-configuration-document, example:E1, rules:GCF04-R1, phase:55, slice:GCF-04"
  )

  "ConfigurationDocument" should {
    "preserve physical mapping occurrence order" which {
      "E1 retain repeated spellings instead of collapsing them into a Map" must _e1 {
        "when a source document contains repeated fields" in {
          Given("an occurrence-preserving mapping document")
          val document = ConfigurationDocument.Object(
            Vector(
              ConfigurationDocument.Field("textus.application.mode", ConfigurationDocument.Scalar(ConfigurationValue.StringValue("first"))),
              ConfigurationDocument.Field("textus.application.mode", ConfigurationDocument.Scalar(ConfigurationValue.StringValue("second")))
            )
          )

          When("the mapping is passed to candidate decoding")
          val fields = document.fields

          Then("both ordered physical occurrences remain observable")
          fields.map(_.name) shouldBe Vector("textus.application.mode", "textus.application.mode")
          fields.map(_.value) shouldBe Vector(
            ConfigurationDocument.Scalar(ConfigurationValue.StringValue("first")),
            ConfigurationDocument.Scalar(ConfigurationValue.StringValue("second"))
          )
        }
      }
    }
  }
}
