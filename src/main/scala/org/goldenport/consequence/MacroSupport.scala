package org.goldenport.consequence

import scala.quoted.*
import org.goldenport.observation.SourcePosition

/*
 * @since   Jan. 31, 2026
 * @version Feb. 28, 2026
 * @author  ASAMI, Tomoharu
 */
object SourcePositionMacro {
  inline def position(): SourcePosition = ${ capture }

  def capture(using Quotes): Expr[SourcePosition] = {
    import quotes.reflect.*

    val pos = Position.ofMacroExpansion
    val file = pos.sourceFile.jpath.toString
    val line = pos.startLine + 1
    val col  = pos.startColumn + 1
    val fileExpr = Expr(file)
    val lineExpr = Expr(line)
    val colExpr = Expr(col)
    '{
       SourcePosition($fileExpr, $lineExpr, $colExpr)
     }
  }
}
