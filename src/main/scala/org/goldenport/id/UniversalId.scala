package org.goldenport.id

import java.time.{Clock, Instant, ZoneOffset}
import java.time.format.DateTimeFormatter
// import org.goldenport.text.Presentable
import org.goldenport.Consequence
import org.goldenport.datatype.Identifier

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
 * Stable identifier usage:
 *   Some identifiers are not operationally generated ids but stable semantic keys
 *   (for example collection ids). In those cases use the stable-id construction path
 *   so the canonical value is deterministic. The default stable parts are:
 *   - timestamp = Instant.EPOCH
 *   - entropy = "stable"
 *
 * Canonical key usage:
 *   Use `value` when a UniversalId is used as a string key for persistence, lookup,
 *   query parameters, map keys, joins, or other machine-level identity handling.
 *   `print` is for presentation, and `show` / `toString` are debugger-oriented summaries.
 *   Do not use `show` / `toString` as canonical identifier values.
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
 *  version Jan. 20, 2026
 *  version Feb. 25, 2026
 *  version Mar. 30, 2026
 * @version Mar. 31, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class UniversalId protected (
  major: String,
  minor: String,
  kind: String,
  subkind: Option[String],
  timestamp: Option[Instant],
  entropy: Option[String]
) extends Identifier {
  import UniversalId._

  // Auxiliary constructor for backward compatibility
  protected def this(
    major: String,
    minor: String,
    kind: String
  ) = this(major, minor, kind, None, None, None)

  protected def this(
    major: String,
    minor: String,
    kind: String,
    subkind: String
  ) = this(major, minor, kind, Some(subkind), None, None)

  protected def this(
    major: String,
    minor: String,
    kind: String,
    subkind: String,
    timestamp: Instant
  ) = this(major, minor, kind, Some(subkind), Some(timestamp), None)

  protected def this(
    major: String,
    minor: String,
    kind: String,
    subkind: String,
    timestamp: Option[Instant],
    entropy: Option[String]
  ) = this(major, minor, kind, Some(subkind), timestamp, entropy)

  protected def this(
    major: String,
    minor: String,
    kind: String,
    timestamp: Option[Instant],
    entropy: Option[String]
  ) = this(major, minor, kind, None, timestamp, entropy)

  override protected def do_validation_in_init = false

  private def _validate_label(name: String, value: String): Unit = {
    require(
      AllowedLabelPattern.matches(value),
      s"$name must match pattern [A-Za-z0-9_]+ but was: '$value'"
    )
  }

  _validate_label("major", major)
  _validate_label("minor", minor)
  _validate_label("kind", kind)
  subkind.foreach(_validate_label("subkind", _))

  private val _parts = UniversalId.Parts.create(major, minor, kind, subkind, timestamp, entropy)

  def value: String = _parts.value

  def parts: UniversalId.Parts = _parts

//  def print: String = value

  override def display: String = {
    val sk = subkind.map(sk => s"-$sk").getOrElse("")
    s"$major-$minor-$kind$sk"
  }

  // Keep show as a debugger-oriented summary instead of the canonical value.
  // Presentable.toString delegates to show, so IDE/debugger inspection will surface
  // kind/subkind plus timestamp-oriented operational hints without requiring callers
  // to explicitly call value/print.
  override def show: String = s"${display}-${_parts.timestampLabel}"

  override def equals(obj: Any): Boolean =
    obj match {
      case that: UniversalId => value == that.value
      case _ => false
    }

  override def hashCode(): Int = value.hashCode
}

object UniversalId {
  private val AllowedLabelPattern = "^[A-Za-z0-9_]+$".r
  val StableTimestamp: Instant = Instant.EPOCH
  val StableEntropy: String = "stable"

  def parse[T](
    value: String,
    expectedkind: String
  )(
    build: Parts => T
  ): Consequence[T] =
    parse_parts(value, expectedkind).map(build)

  def parseParts(
    value: String,
    expectedkind: String
  ): Consequence[Parts] =
    parse_parts(value, expectedkind)

  final case class Parts(
    major: String,
    minor: String,
    kind: String,
    subkind: Option[String],
    timestamp: Instant,
    entropy: String
  ) {
    val timestampLabel = timestamp.toEpochMilli.toString

    val value =
      subkind match {
        case Some(sk) => s"${major}-${minor}-${kind}-${sk}-${timestampLabel}-${entropy}"
        case None     => s"${major}-${minor}-${kind}-${timestampLabel}-${entropy}"
      }
  }

  object Parts {
    // // The timestamp format is digits-only. It is intended for identification/debugging, not time arithmetic.
    // // Timezone offset is encoded without sign.
    // private val _timestamp_formatter =
    //   DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSZZ")
    //     .withZone(ZoneOffset.UTC)

    // private def _timestamep_format_ymd(p: Option[Instant]): String = p match {
    //   case Some(s) =>
    //     if (s == Instant.EPOCH)
    //       "0"
    //     else
    //       _timestamp_formatter.format(s)
    //   case None => _timestamp_formatter.format(Instant.now())
    // }

    // private def _timestamp_formatter(p: Option[Instant]): String = p match {
    //   case Some(s) => _timestamp_formatter(s)
    //   case None => _timestamp_formatter(Instant.now())
    // }

    // private def _timestamp_formatter(p: Instant): String =
    //   p.toEpochMilli.toString

    def create(
      major: String,
      minor: String,
      kind: String,
      subkind: Option[String],
      timestamp: Option[Instant],
      entropy: Option[String]
    ): Parts = {
      Parts(
        major,
        minor,
        kind,
        subkind,
        timestamp getOrElse Instant.now(),
        entropy getOrElse CompactUuid.generateString()
      )
    }

    def stable(
      major: String,
      minor: String,
      kind: String,
      subkind: Option[String]
    ): Parts =
      Parts(
        major,
        minor,
        kind,
        subkind,
        StableTimestamp,
        StableEntropy
      )
  }

  private def parse_parts(
    value: String,
    expectedkind: String
  ): Consequence[Parts] = {
    val tokens = value.split("-").toVector

    tokens match {
      case Vector(major, minor, kind, timestamplabel, entropy) =>
        _build_parts(
          major,
          minor,
          kind,
          None,
          timestamplabel,
          entropy,
          expectedkind
        )
      case Vector(major, minor, kind, subkind, timestamplabel, entropy) =>
        _build_parts(
          major,
          minor,
          kind,
          Some(subkind),
          timestamplabel,
          entropy,
          expectedkind
        )
      case _ =>
        Consequence.failure(s"Invalid UniversalId format: '$value'")
    }
  }

  private def _build_parts(
    major: String,
    minor: String,
    kind: String,
    subkind: Option[String],
    timestamplabel: String,
    entropy: String,
    expectedkind: String
  ): Consequence[Parts] = {
    if (kind != expectedkind)
      Consequence.failure(s"Invalid kind: expected '$expectedkind' but was '$kind'")
    else if (!_valid_label(major))
      Consequence.failure(s"Invalid label in major: '$major'")
    else if (!_valid_label(minor))
      Consequence.failure(s"Invalid label in minor: '$minor'")
    else if (!_valid_label(kind))
      Consequence.failure(s"Invalid label in kind: '$kind'")
    else if (subkind.exists(x => !_valid_label(x)))
      Consequence.failure(s"Invalid label in subkind: '${subkind.get}'")
    else if (entropy.isEmpty)
      Consequence.failure("Entropy must not be empty")
    else
      Consequence(timestamplabel.toLong)
        .map(ts => Instant.ofEpochMilli(ts))
        .map(ts => Parts(major, minor, kind, subkind, ts, entropy))
  }

  private def _valid_label(value: String): Boolean =
    AllowedLabelPattern.matches(value)
}
