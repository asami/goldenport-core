package org.goldenport.id

import java.nio.ByteBuffer
import java.util.Base64
import java.util.UUID

/*
 * @since   Dec. 31, 2025
 * @version Dec. 31, 2025
 * @author  ASAMI, Tomoharu
 */
/**
 * CanonicalId extends UniversalId and represents identifiers that may
 * carry semantic or canonical meaning.
 *
 * The canonical identifier format is defined by UniversalId and
 * UniversalIdGenerator.
 */
abstract class CanonicalId protected (value: String)
  extends UniversalId(value)

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
  delegate: UniversalIdGenerator
) extends CanonicalIdGenerator {
  def generate(
    service: String,
    operation: String,
    kind: String,
    clock: java.time.Clock
  ): CanonicalId = {
    val uid = delegate.generate(service, operation, kind, clock)
    new CanonicalIdImpl(uid.value)
  }
}

private final class CanonicalIdImpl(
  value: String
) extends CanonicalId(value)
