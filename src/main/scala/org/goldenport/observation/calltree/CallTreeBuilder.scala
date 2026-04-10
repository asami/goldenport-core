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
 *  version Feb.  7, 2026
 * @version Apr. 11, 2026
 * @author  ASAMI, Tomoharu
 */
final class CallTreeBuilder {
  private val _root = new _builder_node
  private val _stack = mutable.Stack.empty[_frame]

  def enter(
    label: String,
    attributes: Map[String, String] = Map.empty
  ): Unit =
    _push(label, CallTreeNode.Enter(label, attributes + ("started_at_nanos" -> _now_nanos.toString)))

  def exit(
    label: String,
    attributes: Map[String, String] = Map.empty
  ): Unit = {
    val ended = _now_nanos
    val attrs = _stack.headOption match {
      case Some(frame) =>
        attributes ++ _timing_attributes(frame.startedAtNanos, ended, "ended_at_nanos")
      case None =>
        attributes + ("ended_at_nanos" -> ended.toString)
    }
    _append_current(CallTreeNode.Exit(label, attrs))
    if (_stack.nonEmpty)
      _stack.pop()
  }

  def failure(
    label: String,
    message: String,
    attributes: Map[String, String] = Map.empty
  ): Unit =
    _append_current(CallTreeNode.Failure(label, message, attributes + ("failed_at_nanos" -> _now_nanos.toString)))

  def build(): CallTree =
    CallTree(Tree(_root.to_tree_dir(_active_nodes(_now_nanos))))

  private def _push(label: String, node: CallTreeNode): Unit = {
    _append_current(node)
    _stack.push(_frame(label, _enter_started_at_nanos(node)))
  }

  private def _append_current(node: CallTreeNode): Unit = {
    val path = PathName(_stack.reverse.map(_.label).toVector)
    _root.insert(path.segments, node)
  }

  private def _enter_started_at_nanos(
    node: CallTreeNode
  ): Long =
    node match {
      case CallTreeNode.Enter(_, attributes) =>
        attributes.get("started_at_nanos").flatMap(x => scala.util.Try(x.toLong).toOption).getOrElse(_now_nanos)
      case _ =>
        _now_nanos
    }

  private def _now_nanos: Long =
    System.nanoTime()

  private def _active_nodes(
    now: Long
  ): Vector[(Vector[String], CallTreeNode)] = {
    val frames = _stack.reverse.toVector
    frames.zipWithIndex.map {
      case (frame, index) =>
        val path = frames.take(index).map(_.label)
        path -> CallTreeNode.Active(
          frame.label,
          _timing_attributes(frame.startedAtNanos, now, "sampled_at_nanos")
        )
    }
  }

  private def _timing_attributes(
    started: Long,
    ended: Long,
    endedKey: String
  ): Map[String, String] = {
    val duration = math.max(0L, ended - started)
    Map(
      "started_at_nanos" -> started.toString,
      endedKey -> ended.toString,
      "duration_nanos" -> duration.toString,
      "duration_micros" -> (duration / 1000L).toString,
      "duration_millis" -> (duration / 1000000L).toString
    )
  }

  private final case class _frame(
    label: String,
    startedAtNanos: Long
  )

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
      if (segments.isEmpty) {
        leaves += value
      } else {
        val child = children.getOrElseUpdate(segments.head, new _builder_node)
        child.insert(segments.tail, value)
      }

    def to_tree_dir: TreeDir[CallTreeNode] =
      _to_tree_dir

    def to_tree_dir(
      activeNodes: Vector[(Vector[String], CallTreeNode)]
    ): TreeDir[CallTreeNode] = {
      val snapshot = copy_node
      activeNodes.foreach {
        case (path, node) => snapshot.insert(path, node)
      }
      snapshot.to_tree_dir
    }

    private def copy_node: _builder_node = {
      val r = new _builder_node
      children.foreach {
        case (name, child) => r.children += (name -> child.copy_node)
      }
      r.leaves ++= leaves
      r
    }

    private def _to_tree_dir: TreeDir[CallTreeNode] =
      TreeDir(_tree_entries)

    private def to_tree_node: TreeNode[CallTreeNode] =
      if (children.nonEmpty)
        TreeDir(_tree_entries)
      else
        TreeDir(_leaf_entries)

    private def _tree_entries: Vector[TreeEntry[CallTreeNode]] =
      children.map { case (name, node) =>
        TreeEntry(name, node.to_tree_node)
      }.toVector ++ _leaf_entries

    private def _leaf_entries: Vector[TreeEntry[CallTreeNode]] =
      leaves.zipWithIndex.map {
        case (v, i) => TreeEntry(i.toString, TreeLeaf(v))
      }.toVector
  }
}
