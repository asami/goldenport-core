package org.goldenport.observation.calltree

import org.goldenport.tree.Tree

/*
 * CallTree is a first-class observation element for low-level execution tracing.
 *
 * It wraps the generic immutable Tree so CallTree-specific evolution does not leak
 * into the generic Tree model.
 *
 * @since   Feb.  7, 2026
 * @version Feb.  7, 2026
 * @author  ASAMI, Tomoharu
 */
final case class CallTree(
  tree: Tree[CallTreeNode]
)

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

  final case class Failure(
    label: String,
    message: String,
    attributes: Map[String, String] = Map.empty
  ) extends CallTreeNode
}
