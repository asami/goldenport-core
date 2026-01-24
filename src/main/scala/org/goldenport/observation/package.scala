package org.goldenport.observation

import org.goldenport.id.UniversalId

/*
 * @since   Jul. 13, 2025
 *  version Jul. 23, 2025
 * @version Jan. 25, 2026
 * @author  ASAMI, Tomoharu
 */
case class TraceId(
  major: String,
  minor: String
) extends UniversalId(major, minor, "trace")

case class SpanId(
  major: String,
  minor: String
) extends UniversalId(major, minor, "span")
