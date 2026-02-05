package org.goldenport.datatype

/*
 * PathName
 *
 * - Immutable
 * - Tree / FS / REST / logical path independent
 * - Minimal normalization
 * - No semantic interpretation
 *
 * PathName represents a structured name, not a location.
 */
/*
 * @since   Jan. 10, 2017
 *  version Apr. 16, 2017
 *  version May. 18, 2017
 *  version Jul. 25, 2017
 *  version Aug. 29, 2017
 *  version Nov.  6, 2017
 *  version Jan. 12, 2018
 *  version Mar. 13, 2018
 *  version Dec. 27, 2018
 *  version Apr. 14, 2020
 *  version May. 18, 2020
 *  version Jun.  6, 2020
 *  version Mar. 15, 2021
 *  version Jun. 29, 2021
 *  version Mar.  6, 2022
 *  version Dec. 30, 2022
 *  version Jan.  4, 2023
 *  version Sep. 26, 2023
 *  version Mar.  9, 2025
 *  version Aug. 17, 2025
 *  version Nov. 21, 2025
 * @version Feb.  5, 2026
 * @author  ASAMI, Tomoharu
 */
final case class PathName private (
  segments: Vector[String]
) {

  def isRoot: Boolean =
    segments.isEmpty

  def parent: Option[PathName] =
    if (segments.isEmpty) None
    else Some(PathName(segments.dropRight(1)))

  def /(child: String): PathName =
    if (child == null)
      this
    else {
      val c = child.trim
      if (c.isEmpty || c == ".")
        this
      else
        PathName(segments :+ c)
    }

  def asString(separator: Char = '/'): String =
    segments.mkString(separator.toString)

  override def toString: String =
    asString('/')
}

object PathName {

  val Root: PathName =
    new PathName(Vector.empty)

  def apply(segments: Seq[String]): PathName =
    new PathName(normalize(segments.toVector))

  def parse(
    s: String,
    separator: Char = '/'
  ): PathName =
    if (s == null || s.isEmpty)
      Root
    else
      PathName(normalize(s.split(separator).toVector))

  private def normalize(
    segments: Vector[String]
  ): Vector[String] =
    segments
      .map(_.trim)
      .filterNot(_.isEmpty)
      .filterNot(_ == ".")
}
