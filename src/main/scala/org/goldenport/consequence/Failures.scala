package org.goldenport.consequence

import scala.quoted.*
import org.goldenport.Consequence
import org.goldenport.Conclusion
import org.goldenport.provisional.observation.Observation
import org.goldenport.provisional.observation.Taxonomy
import org.goldenport.provisional.observation.Cause
import org.goldenport.observation.Descriptor
import org.goldenport.observation.SourcePosition
import org.goldenport.record.Record

/*
 * @since   Jan. 31, 2026
 *  version Feb. 28, 2026
 * @version Mar. 10, 2026
 * @author  ASAMI, Tomoharu
 */
object Failures {
  inline def fail(o: Observation): Consequence.Failure[Nothing] =
    ${ _fail('o) }

  private def _fail(o: Expr[Observation])(using Quotes): Expr[Consequence.Failure[Nothing]] =
    '{
       val pos = ${ SourcePositionMacro.capture }
       Consequence.Failure(Conclusion.failure(pos, $o))
     }

  inline def fail(taxonomy: Taxonomy, cause: Cause): Consequence.Failure[Nothing] =
    ${ _fail('taxonomy, 'cause) }

  private def _fail(
    taxonomy: Expr[Taxonomy],
    cause: Expr[Cause]
  )(using Quotes): Expr[Consequence.Failure[Nothing]] =
    '{
       val pos = ${ SourcePositionMacro.capture }
       Consequence.Failure(Conclusion.failure(pos, $taxonomy, $cause))
     }

  inline def fail(
    taxonomy: Taxonomy,
    facets: Seq[Descriptor.Facet]
  ): Consequence.Failure[Nothing] =
    ${ _fail_facet('taxonomy, 'facets) }

  private def _fail_facet(
    taxonomy: Expr[Taxonomy],
    facets: Expr[Seq[Descriptor.Facet]]
  )(using Quotes): Expr[Consequence.Failure[Nothing]] =
    '{
       val pos = ${ SourcePositionMacro.capture }
       Consequence.Failure(Conclusion.failure(pos, $taxonomy, $facets))
     }

  inline def fail(
    taxonomy: Taxonomy
  ): Consequence.Failure[Nothing] =
    ${ _fail_facet('taxonomy, 'Nil) }

  // inline def fail(
  //   taxonomy: Taxonomy,
  //   e: Throwable,
  //   facets: Seq[Descriptor.Facet]
  // ): Consequence.Failure[Nothing] =
  //   ${ _fail('taxonomy, 'e, 'facets) }

  // private def _fail(
  //   taxonomy: Expr[Taxonomy],
  //   e: Expr[Throwable],
  //   facets: Expr[Seq[Descriptor.Facet]]
  // )(using Quotes): Expr[Consequence.Failure[Nothing]] =
  //   '{
  //      val pos = ${ SourcePositionMacro.capture }
  //      val exceptionFacet = Descriptor.Facet.Exception($e)
  //      Consequence.Failure(Conclusion.failure(pos, $taxonomy, exceptionFacet +: $facets))
  //    }

  inline def fail(c: Conclusion): Consequence.Failure[Nothing] = {
    val pos = SourcePositionMacro.position()
    fail(c, pos)
  }

  def fail(c: Conclusion, pos: SourcePosition): Consequence.Failure[Nothing] =
    Consequence.Failure(c.withSourcePosition(pos))

  inline def resourceInconsistency: Consequence.Failure[Nothing] =
    ${ _resource_inconsistency }

  private def _resource_inconsistency(using Quotes): Expr[Consequence.Failure[Nothing]] =
    '{
       val pos = ${ SourcePositionMacro.capture }
       Consequence.Failure(Conclusion.failResourceInconsistency(pos))
     }

  inline def recordNotFound(key: String, rec: Record): Consequence.Failure[Nothing] =
    ${ recordNotFound('key, 'rec) }

  private def recordNotFound(key: Expr[String], rec: Expr[Record])(using Quotes): Expr[Consequence.Failure[Nothing]] =
    '{
       val pos = ${ SourcePositionMacro.capture }
       Consequence.Failure(Conclusion.failRecordNotFound(pos, ${key}, ${rec}))
     }

  inline def operationNotFound(name: String): Consequence.Failure[Nothing] =
    ${ operationNotFound('name) }

  private def operationNotFound(name: Expr[String])(using Quotes): Expr[Consequence.Failure[Nothing]] =
    '{
       val pos = ${ SourcePositionMacro.capture }
       Consequence.Failure(Conclusion.failOperationNotFound(pos, ${name}))
     }

  inline def unreachableReached: Consequence.Failure[Nothing] =
    ${ _unreachable_reached }

  private def _unreachable_reached(using Quotes): Expr[Consequence.Failure[Nothing]] =
    '{
       val pos = ${ SourcePositionMacro.capture }
       Consequence.Failure(Conclusion.failUnreachableReached(pos))
     }

  inline def uninitializedState: Consequence.Failure[Nothing] =
    ${ _uninitialized_state }

  private def _uninitialized_state(using Quotes): Expr[Consequence.Failure[Nothing]] =
    '{
       val pos = ${ SourcePositionMacro.capture }
       Consequence.Failure(Conclusion.failUninitializedState(pos))
     }

  inline def uninitializedState(c: Conclusion): Consequence.Failure[Nothing] =
    ${ _uninitialized_state('c) }

  private def _uninitialized_state(c: Expr[Conclusion])(using Quotes): Expr[Consequence.Failure[Nothing]] =
    '{
       val pos = ${ SourcePositionMacro.capture }
       Consequence.Failure(
         Conclusion.failUninitializedState(pos) ++ ${c}
       )
     }

  inline def notImplemented: Consequence.Failure[Nothing] =
    ${ _not_implemented }

  private def _not_implemented(using Quotes): Expr[Consequence.Failure[Nothing]] =
    '{
       val pos = ${ SourcePositionMacro.capture }
       Consequence.Failure(Conclusion.failNotImplemented(pos))
     }

  // inline def notImplemented(msg: String): Consequence.Failure[Nothing] =
  //   ${ _not_implemented('msg) }

  // private def _not_implemented(msg: Expr[String])(using Quotes): Expr[Consequence.Failure[Nothing]] =
  //   '{
  //      val pos = ${ SourcePositionMacro.capture }
  //      Consequence.Failure(Conclusion.notImplemented(pos, msg))
  //    }
}
