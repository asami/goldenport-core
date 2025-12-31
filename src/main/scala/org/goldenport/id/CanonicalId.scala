package org.goldenport.id

import java.nio.ByteBuffer
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.UUID

/*
 * @since   Dec. 31, 2025
 * @version Dec. 31, 2025
 * @author  ASAMI, Tomoharu
 */
abstract class CanonicalId protected (val value: String) {
  override def toString: String = value

  override def equals(obj: Any): Boolean =
    obj match {
      case that: CanonicalId => value == that.value
      case _ => false
    }

  override def hashCode(): Int = value.hashCode
}

trait CanonicalIdGenerator {
  def generate(
    service: String,
    operation: String,
    kind: String,
    clock: java.time.Clock
  ): CanonicalId
}

trait EntropySource {
  def next(): String
}

object EntropySource {
  object Default extends EntropySource {
    def next(): String = {
      val uuid = UUID.randomUUID()
      val bytes = _uuid_bytes(uuid)
      Base64.getUrlEncoder.withoutPadding.encodeToString(bytes)
    }

    private def _uuid_bytes(uuid: UUID): Array[Byte] = {
      val buffer = ByteBuffer.allocate(16)
      buffer.putLong(uuid.getMostSignificantBits)
      buffer.putLong(uuid.getLeastSignificantBits)
      buffer.array
    }
  }
}

final class DefaultCanonicalIdGenerator(
  entropy: EntropySource
) extends CanonicalIdGenerator {
  def generate(
    service: String,
    operation: String,
    kind: String,
    clock: java.time.Clock
  ): CanonicalId = {
    val timestamp = _format_timestamp(clock)
    val value = s"${service}-${operation}-${kind}-${timestamp}-${entropy.next()}"
    new CanonicalIdImpl(value)
  }

  private def _format_timestamp(clock: java.time.Clock): String = {
    val instant = clock.instant()
    DefaultCanonicalIdGenerator.TimestampFormatter.format(instant)
  }
}

private final class CanonicalIdImpl(
  value: String
) extends CanonicalId(value)

private object DefaultCanonicalIdGenerator {
  val TimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX").withZone(ZoneOffset.UTC)
}
