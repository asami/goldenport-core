package org.goldenport

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.goldenport.consequence.SourcePositionMacro
import org.goldenport.observation.Descriptor
import org.goldenport.observation.SourcePosition
import org.goldenport.record.Record

/*
 * @since   Apr.  8, 2026
 *  version Apr.  8, 2026
 * @version Apr. 14, 2026
 * @author  ASAMI, Tomoharu
 */
class ConsequenceSourcePositionSpec extends AnyFlatSpec with Matchers {
  "Consequence.successOrPropertyNotFound" should "attach the caller source position" in {
    val (expected, result) = _property_not_found_failure()
    val actual = _source_position(result)

    actual.file should endWith ("ConsequenceSourcePositionSpec.scala")
    actual.line should be (expected.line)
  }

  it should "attach the caller source position for record lookup failure" in {
    val rec = Record.data("present" -> "value")
    val (expected, result) = _record_not_found_failure(rec)
    val actual = _source_position(result)

    actual.file should endWith ("ConsequenceSourcePositionSpec.scala")
    actual.line should be (expected.line)
  }

  "Consequence.recordNotFound" should "attach the caller source position" in {
    val rec = Record.data("present" -> "value")
    val (expected, result) = _fail_record_not_found(rec)
    val actual = _source_position(result)

    actual.file should endWith ("ConsequenceSourcePositionSpec.scala")
    actual.line should be (expected.line)
  }

  private def _source_position[T](p: Consequence[T]): SourcePosition =
    p match {
      case Consequence.Failure(conclusion) =>
        conclusion.observation.cause.descriptor.facets.collectFirst {
          case Descriptor.Facet.SrcPos(pos) => pos
        }.getOrElse(fail("Source position facet is missing."))
      case _ =>
        fail("Expected failure consequence.")
    }

  private inline def _property_not_found_failure(): (SourcePosition, Consequence[String]) =
    (SourcePositionMacro.position(), Consequence.successOrPropertyNotFound[String]("missing", None))

  private inline def _record_not_found_failure(rec: Record): (SourcePosition, Consequence[String]) =
    (SourcePositionMacro.position(), Consequence.successOrRecordNotFound[String]("missing", rec))

  private inline def _fail_record_not_found(rec: Record): (SourcePosition, Consequence.Failure[Nothing]) =
    (SourcePositionMacro.position(), Consequence.recordNotFound("missing", rec))
}
