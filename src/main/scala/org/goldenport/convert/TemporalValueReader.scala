package org.goldenport.convert

import java.time._
import java.time.format.DateTimeParseException

import org.goldenport.Consequence
import org.goldenport.context.ExecutionContext

/*
 * Context-required value reader for temporal types.
 *
 * Temporal conversions may depend on execution assumptions such as timezone/clock/locale.
 */
/*
 * @since   Jan. 16, 2026
 * @version Jan. 16, 2026
 * @author  ASAMI, Tomoharu
 */
trait TemporalValueReader[T]:
  def read(value: Any)(using ec: ExecutionContext): Consequence[T]

object TemporalValueReader:
  private def _parse_fail[T](name: String, s: String, e: Throwable): Consequence[T] =
    Consequence.failure(s"$name parse failed: '$s' (${e.getClass.getSimpleName})")

  given TemporalValueReader[Instant] with
    def read(v: Any)(using ec: ExecutionContext): Consequence[Instant] = v match
      case i: Instant => Consequence.success(i)
      case s: String =>
        try Consequence.success(Instant.parse(s.trim))
        catch
          case e: DateTimeParseException => _parse_fail("Instant", s, e)
      case _ =>
        Consequence.failure("Instant value is invalid")

  given TemporalValueReader[LocalDate] with
    def read(v: Any)(using ec: ExecutionContext): Consequence[LocalDate] = v match
      case d: LocalDate => Consequence.success(d)
      case s: String =>
        try Consequence.success(LocalDate.parse(s.trim))
        catch
          case e: DateTimeParseException => _parse_fail("LocalDate", s, e)
      case _ =>
        Consequence.failure("LocalDate value is invalid")

  given TemporalValueReader[LocalDateTime] with
    def read(v: Any)(using ec: ExecutionContext): Consequence[LocalDateTime] = v match
      case dt: LocalDateTime => Consequence.success(dt)
      case s: String =>
        try Consequence.success(LocalDateTime.parse(s.trim))
        catch
          case e: DateTimeParseException => _parse_fail("LocalDateTime", s, e)
      case _ =>
        Consequence.failure("LocalDateTime value is invalid")

  given TemporalValueReader[ZonedDateTime] with
    def read(v: Any)(using ec: ExecutionContext): Consequence[ZonedDateTime] = v match
      case z: ZonedDateTime => Consequence.success(z)
      case s: String =>
        val ss = s.trim
        try
          // If the string contains zone/offset, parse as-is; otherwise assume execution timezone.
          if ss.contains("Z") || ss.contains("+") || ss.contains("-") && ss.contains("T") then
            Consequence.success(ZonedDateTime.parse(ss))
          else
            Consequence.success(LocalDateTime.parse(ss).atZone(ec.timezone))
        catch
          case e: DateTimeParseException => _parse_fail("ZonedDateTime", s, e)
      case _ =>
        Consequence.failure("ZonedDateTime value is invalid")

  given TemporalValueReader[Duration] with
    def read(v: Any)(using ec: ExecutionContext): Consequence[Duration] = v match
      case d: Duration => Consequence.success(d)
      case s: String =>
        try Consequence.success(Duration.parse(s.trim))
        catch
          case e: DateTimeParseException => _parse_fail("Duration", s, e)
      case _ =>
        Consequence.failure("Duration value is invalid")

