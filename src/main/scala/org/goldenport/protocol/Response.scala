package org.goldenport.protocol

import org.goldenport.protocol.scalar.ScalarValue
import org.goldenport.text.Presentable

/**
 * Response represents protocol-level output semantics.
 * It is independent of transport (CLI / REST / MCP).
 */
/*
 * @since   Jan.  1, 2026
 *  version Jan.  3, 2026
 * @version Jan. 20, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class Response extends Presentable

object Response {
  final case class Void() extends Response {
    def print = ""
  }

  /** JSON representation (protocol-level, transport-agnostic). */
  final case class Json(value: String) extends Response {
    def print = value
  }

  /**
   * Scalar response value.
   *
   * Intended only for Scala primitive-like / Predef types
   * such as Int, Long, Double, Boolean, String.
   *
   * Structured or semantic values MUST NOT use Scalar.
   */
  /**
   * Scalar wraps a value permitted by ScalarValue.
   *
   * It MUST be used only for atomic Scala values.
   * Structured, semantic, or document-like values are out of scope.
   */
  final case class Scalar[T: ScalarValue](value: T) extends Response {
    def print = Presentable.print(value)
  }

  final case class Opaque(value: Any) extends Response {
    def print = Presentable.print(value)
  }
}
