package org.goldenport.id

import java.nio.ByteBuffer
import java.time.{Clock, Instant, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.UUID

/**
 * CanonicalId extends UniversalId and represents identifiers that may
 * carry semantic or canonical meaning.
 *
 * The canonical identifier format is defined by UniversalId.
 */
/*
 * @since   Dec. 31, 2025
 * @version Jan.  4, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class CanonicalId protected (
  major: String,
  minor: String,
  kind: String
) extends UniversalId(major, minor, kind) {
}
