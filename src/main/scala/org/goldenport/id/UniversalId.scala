package org.goldenport.id

import java.time.{Clock, Instant, ZoneOffset}
import java.time.format.DateTimeFormatter

/**
 * UniversalId is an opaque, value-based operational identifier with a canonical string format.
 *
 * The canonical string format is:
 *   <major>-<minor>-<kind>-<timestamp>-<entropy>
 *
 * Components:
 *   - major: ASCII alphanumeric plus underscore label identifying the major category
 *   - minor: ASCII alphanumeric plus underscore label identifying the minor category
 *   - kind: ASCII alphanumeric plus underscore label identifying the kind of identifier
 *   - timestamp: digits-only timestamp in UTC timezone with format yyyyMMddHHmmssSSZZ, used for identification/debugging only
 *   - entropy: a string providing randomness/uniqueness to avoid collisions
 *
 * Timestamp format:
 *   Digits-only pattern "yyyyMMddHHmmssSSZZ" in UTC; not intended for time arithmetic.
 *
 * Entropy:
 *   Provides uniqueness to the identifier; typically a compact UUID string.
 *
 * Equality and hashCode:
 *   Based solely on the full string value representation.
 *
 * Extension:
 *   Upper layers may subclass UniversalId to fix the 'kind' component (e.g., JobId, TaskId),
 *   but must not alter the canonical string format.
 *
 * Character constraints:
 *   major, minor, and kind must match the pattern [A-Za-z0-9_]+, i.e., ASCII alphanumeric plus underscore.
 *   Characters such as '-', '.', spaces, or non-ASCII characters are disallowed.
 */
/*
 * @since   Dec. 31, 2025
 *  version Jan.  1, 2026
 * @version Jan.  6, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class UniversalId protected (
  major: String,
  minor: String,
  kind: String,
  subkind: Option[String]
) {
  // Auxiliary constructor for backward compatibility
  protected def this(
    major: String,
    minor: String,
    kind: String
  ) =
    this(major, minor, kind, None)
  import UniversalId._

  private def validateLabel(name: String, value: String): Unit = {
    require(
      AllowedLabelPattern.matches(value),
      s"$name must match pattern [A-Za-z0-9_]+ but was: '$value'"
    )
  }

  validateLabel("major", major)
  validateLabel("minor", minor)
  validateLabel("kind", kind)
  subkind.foreach(validateLabel("subkind", _))

  private val _parts = UniversalId.Parts.create(major, minor, kind, subkind)

  def value: String = _parts.value

  override def toString: String = value

  override def equals(obj: Any): Boolean =
    obj match {
      case that: UniversalId => value == that.value
      case _ => false
    }

  override def hashCode(): Int = value.hashCode
}

object UniversalId {
  private val AllowedLabelPattern = "^[A-Za-z0-9_]+$".r

  final case class Parts(
    major: String,
    minor: String,
    kind: String,
    subkind: Option[String],
    timestamp: String,
    entropy: String
  ) {
    val value =
      subkind match {
        case Some(sk) => s"${major}-${minor}-${kind}-${sk}-${timestamp}-${entropy}"
        case None     => s"${major}-${minor}-${kind}-${timestamp}-${entropy}"
      }
  }

  object Parts {
    // The timestamp format is digits-only. It is intended for identification/debugging, not time arithmetic.
    // Timezone offset is encoded without sign.
    private val _timestamp_formatter =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSZZ")
        .withZone(ZoneOffset.UTC)
    def create(major: String, minor: String, kind: String, subkind: Option[String]): Parts = {
      val now = Instant.now()
      val formattedTimestamp = _timestamp_formatter.format(now)
      Parts(
        major,
        minor,
        kind,
        subkind,
        formattedTimestamp,
        CompactUuid.generateString()
      )
    }
  }
}
