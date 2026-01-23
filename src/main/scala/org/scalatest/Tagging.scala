package org.scalatest

/*
 * Minimal Tagging facsimile for Scala 3 cross-building expectations.
 * ScalaTest 3.2 provided this trait; define a lightweight version so core
 * can depend on string tags without needing annotation-based helpers.
 */
trait Tagging {
  def tags: Map[String, Tag] = Map.empty
}
