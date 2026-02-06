package org.goldenport.datatype

import java.nio.file.{Files, Path}
import org.goldenport.Consequence
import org.goldenport.bag.Bag

/*
 * Represents content that is semantically treated as a file.
 *
 * - name: logical/domain name of the content
 * - filename: actual file name including suffix
 * - content: raw data
 * - mimeType: resolved from filename suffix
 */
/*
 * @since   Feb.  6, 2026
 * @version Feb.  6, 2026
 * @author  ASAMI, Tomoharu
 */
final case class FileContent(
  filename: String,
  content: Bag,
  mimeType: Option[MimeType]
)

object FileContent {
  def create(path: Path): FileContent = {
    val filename = Option(path.getFileName).map(_.toString).getOrElse(path.toString)
    val mimeType =
    Option(filename)
      .flatMap(fname =>
        fname.lastIndexOf('.') match {
          case idx if idx >= 0 => MimeType.fromSuffix(fname.substring(idx + 1))
          case _ => None
        }
      )
    FileContent(filename, Bag.file(path), mimeType)
  }
}
