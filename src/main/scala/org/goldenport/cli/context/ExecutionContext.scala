package org.goldenport.cli.context

/*
 * @since   Dec. 22, 2025
 * @version Dec. 22, 2025
 * @author  ASAMI, Tomoharu
 */
case class ExecutionContextSnapshot(values: Map[String, String] = Map.empty)

object SnapshotBuilder:
  def build(): ExecutionContextSnapshot =
    ExecutionContextSnapshot()
