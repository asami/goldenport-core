package org.goldenport.protocol

import org.goldenport.protocol.scalar.ScalarValue

/**
 * Response represents protocol-level output semantics.
 * It is independent of transport (CLI / REST / MCP).
 */
/*
 * @since   Jan.  1, 2026
 * @version Jan.  3, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class Response

object Response {
  case object Void extends Response

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
  final case class Scalar[T: ScalarValue](value: T) extends Response
}
