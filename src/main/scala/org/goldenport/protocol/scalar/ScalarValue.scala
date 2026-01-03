package org.goldenport.protocol.scalar

/*
 * @since   Jan.  3, 2026
 * @version Jan.  3, 2026
 * @author  ASAMI, Tomoharu
 */
sealed abstract class ScalarValue[T] private ()

/**
 * ScalarValue is the protocol-boundary whitelist for scalar values.
 *
 * It defines which Scala primitive-like / Predef types are allowed
 * to cross the Protocol boundary. It is intentionally closed and
 * MUST NOT be extended externally.
 *
 * Adding a new ScalarValue instance is a protocol design decision.
 */
object ScalarValue {
  /**
   * These givens are the complete, authoritative set.
   * External modules MUST NOT define additional ScalarValue givens.
   */
  given ScalarValue[Int]     = new ScalarValue[Int] {}
  given ScalarValue[Long]    = new ScalarValue[Long] {}
  given ScalarValue[Double]  = new ScalarValue[Double] {}
  given ScalarValue[Boolean] = new ScalarValue[Boolean] {}
  given ScalarValue[String]  = new ScalarValue[String] {}
}
