package org.goldenport.observation.calltree

import org.goldenport.datatype.PathName
import org.goldenport.tree.{Tree, TreeDir, TreeEntry, TreeLeaf, TreeNode}
import scala.collection.mutable

/*
 * CallTreeBuilder is a mutable collector for CallTree.
 *
 * - Recording phase: mutable enter/exit/failure
 * - Finalization: build immutable CallTree(Tree[CallTreeNode])
 *
 * This is a draft scaffold; it is intentionally minimal and side-effect only.
 *
 * @since   Feb.  7, 2026
 * @version Feb.  7, 2026
 * @author  ASAMI, Tomoharu
 */
final class CallTreeBuilder {
  private val _root = new _builder_node
  private val _stack = mutable.Stack.empty[String]

  def enter(
    label: String,
    attributes: Map[String, String] = Map.empty
  ): Unit =
    _push(label, CallTreeNode.Enter(label, attributes))

  def exit(
    label: String,
    attributes: Map[String, String] = Map.empty
  ): Unit = {
    _append_current(CallTreeNode.Exit(label, attributes))
    if (_stack.nonEmpty)
      _stack.pop()
  }

  def failure(
    label: String,
    message: String,
    attributes: Map[String, String] = Map.empty
  ): Unit =
    _append_current(CallTreeNode.Failure(label, message, attributes))

  def build(): CallTree =
    CallTree(Tree(_root.to_tree_dir))

  private def _push(label: String, node: CallTreeNode): Unit = {
    _append_current(node)
    _stack.push(label)
  }

  private def _append_current(node: CallTreeNode): Unit = {
    val path = PathName(_stack.reverse.toVector)
    _root.insert(path.segments, node)
  }

  /*
   * Internal mutable tree builder.
   *
   * - children preserve insertion order
   * - leaves are stored as sequential entries under the node
   */
  private final class _builder_node {
    val children: mutable.LinkedHashMap[String, _builder_node] =
      mutable.LinkedHashMap.empty
    val leaves: mutable.ListBuffer[CallTreeNode] =
      mutable.ListBuffer.empty

    def insert(segments: Vector[String], value: CallTreeNode): Unit =
      segments match {
        case Vector() =>
          leaves += value
        case head +: tail =>
          val child = children.getOrElseUpdate(head, new _builder_node)
          child.insert(tail, value)
      }

    def to_tree_dir: TreeDir[CallTreeNode] =
      TreeDir(
        children.map { case (name, node) =>
          TreeEntry(name, node.to_tree_node)
        }.toVector ++ _leaf_entries
      )

    private def to_tree_node: TreeNode[CallTreeNode] =
      if (children.nonEmpty)
        TreeDir(
          children.map { case (name, node) =>
            TreeEntry(name, node.to_tree_node)
          }.toVector ++ _leaf_entries
        )
      else
        TreeDir(_leaf_entries)

    private def _leaf_entries: Vector[TreeEntry[CallTreeNode]] =
      leaves.zipWithIndex.map {
        case (v, i) => TreeEntry(i.toString, TreeLeaf(v))
      }.toVector
  }
}
