package org.goldenport.consequence

import scala.quoted.*
import org.goldenport.Consequence
import org.goldenport.Conclusion

/*
 * @since   Jan. 31, 2026
 * @version Feb.  4, 2026
 * @author  ASAMI, Tomoharu
 */
object Failures {
  inline def resourceInconsistency: Consequence.Failure[Nothing] =
    ${ _resource_inconsistency }

  private def _resource_inconsistency(using Quotes): Expr[Consequence.Failure[Nothing]] =
    '{
       val pos = ${ SourcePositionMacro.capture }
       Consequence.Failure(Conclusion.failResourceInconsistency(pos))
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
}
