package org.goldenport.consequence

import scala.quoted.*
import org.goldenport.observation.SourcePosition

/*
 * @since   Jan. 31, 2026
 *  version Feb. 28, 2026
 * @version Apr. 11, 2026
 * @author  ASAMI, Tomoharu
 */
object SourcePositionMacro {
  inline def position(): SourcePosition = ${ capture }

  def capture(using Quotes): Expr[SourcePosition] = {
    import quotes.reflect.*

    val pos = Position.ofMacroExpansion
    val file = _relativize_path(pos.sourceFile.path)
    val line = pos.startLine + 1
    val col  = pos.startColumn + 1
    val fileExpr = Expr(file)
    val lineExpr = Expr(line)
    val colExpr = Expr(col)
    '{
       SourcePosition($fileExpr, $lineExpr, $colExpr)
     }
  }

  private def _relativize_path(path: String): String = {
    import java.nio.file.Paths

    val absolute = Paths.get(path).toAbsolutePath.normalize
    val base = Option(System.getProperty("user.dir"))
      .map(Paths.get(_).toAbsolutePath.normalize)
    val relative = base match {
      case Some(b) if absolute.startsWith(b) => b.relativize(absolute).toString
      case _ => path
    }
    relative.replace('\\', '/')
  }
}
