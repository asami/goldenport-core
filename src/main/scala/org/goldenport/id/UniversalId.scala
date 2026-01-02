package org.goldenport.id

import java.time.{Clock, ZoneOffset}
import java.time.format.DateTimeFormatter

/*
 * @since   Dec. 31, 2025
 * @version Jan. 1, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class UniversalId protected (val value: String) {
  override def toString: String = value

  override def equals(obj: Any): Boolean =
    obj match {
      case that: UniversalId => value == that.value
      case _ => false
    }

  override def hashCode(): Int = value.hashCode
}

trait UniversalIdGenerator {
  def generate(
    service: String,
    operation: String,
    kind: String,
    clock: Clock
  ): UniversalId
}

final class DefaultUniversalIdGenerator(
  entropy: EntropySource
) extends UniversalIdGenerator {
  def generate(
    service: String,
    operation: String,
    kind: String,
    clock: Clock
  ): UniversalId = {
    val timestamp = _format_timestamp(clock)
    val value = s"${service}-${operation}-${kind}-${timestamp}-${entropy.next()}"
    new UniversalIdImpl(value)
  }

  private def _format_timestamp(clock: Clock): String = {
    val instant = clock.instant()
    DefaultUniversalIdGenerator.TimestampFormatter.format(instant)
  }
}

private final class UniversalIdImpl(
  value: String
) extends UniversalId(value)

private object DefaultUniversalIdGenerator {
  val TimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX").withZone(ZoneOffset.UTC)
}
