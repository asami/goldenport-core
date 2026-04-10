package org.goldenport.observation.calltree

import org.goldenport.record.Record
import org.goldenport.tree.Tree

/*
 * CallTree is a first-class observation element for low-level execution tracing.
 *
 * It wraps the generic immutable Tree so CallTree-specific evolution does not leak
 * into the generic Tree model.
 *
 * @since   Feb.  7, 2026
 *  version Mar.  3, 2026
 * @version Apr. 11, 2026
 * @author  ASAMI, Tomoharu
 */
final case class CallTree(
  tree: Tree[CallTreeNode]
) {
  def toRecord: Record = {
    val projected = tree.map {
      case CallTreeNode.Enter(label, attributes) =>
        Record.data(
          "kind" -> "enter",
          "label" -> label,
          "attributes" -> Record.create(attributes)
        )
      case CallTreeNode.Exit(label, attributes) =>
        Record.data(
          "kind" -> "exit",
          "label" -> label,
          "attributes" -> Record.create(attributes)
        )
      case CallTreeNode.Active(label, attributes) =>
        Record.data(
          "kind" -> "active",
          "label" -> label,
          "attributes" -> Record.create(attributes)
        )
      case CallTreeNode.Failure(label, message, attributes) =>
        Record.data(
          "kind" -> "failure",
          "label" -> label,
          "message" -> message,
          "attributes" -> Record.create(attributes)
        )
    }
    Tree.toRecord(projected)
  }
}

sealed trait CallTreeNode {
  def label: String
}

object CallTreeNode {
  final case class Enter(
    label: String,
    attributes: Map[String, String] = Map.empty
  ) extends CallTreeNode

  final case class Exit(
    label: String,
    attributes: Map[String, String] = Map.empty
  ) extends CallTreeNode

  final case class Active(
    label: String,
    attributes: Map[String, String] = Map.empty
  ) extends CallTreeNode

  final case class Failure(
    label: String,
    message: String,
    attributes: Map[String, String] = Map.empty
  ) extends CallTreeNode
}
