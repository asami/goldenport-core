package org.goldenport.consequence

import scala.quoted.*
import org.goldenport.Consequence
import org.goldenport.Conclusion

/*
 * @since   Jan. 31, 2026
 * @version Feb.  1, 2026
 * @author  ASAMI, Tomoharu
 */
object Failures {
  inline def unreachableReached: Consequence.Failure[Nothing] =
    ${ _unreachable_reached }

  private def _unreachable_reached(using Quotes): Expr[Consequence.Failure[Nothing]] =
    '{
       val pos = ${ SourcePositionMacro.capture }
       Consequence.Failure(Conclusion.failUnreachableReached(pos))
     }

  inline def notImplemented: Consequence.Failure[Nothing] =
    ${ _not_implemented }

  private def _not_implemented(using Quotes): Expr[Consequence.Failure[Nothing]] =
    '{
       val pos = ${ SourcePositionMacro.capture }
       Consequence.Failure(Conclusion.failNotImplemented(pos))
     }
}
