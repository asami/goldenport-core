package org.goldenport.tree

import org.goldenport.Consequence
import org.goldenport.datatype.PathName
import org.goldenport.record.{Field, Record}
import scala.collection.mutable.LinkedHashMap

/*
 * Tree
 *
 * - Immutable
 * - Order-preserving
 * - PathName-based access
 * - Independent internal structure
 *
 * Tree is an execution-oriented IR.
 * Record projection is handled outside of this file.
 */
/*
 * @since   Jul. 27, 2008
 *  version Apr. 17, 2011
 *  version Feb. 21, 2012
 *  version Apr. 30, 2012
 *  version Nov.  2, 2012
 *  version Nov. 18, 2019
 *  version May.  4, 2020
 *  version Oct. 17, 2020
 *  version Nov. 16, 2020
 *  version Feb.  2, 2021
 *  version Mar. 19, 2022
 *  version Mar.  5, 2025
 *  version Apr. 23, 2025
 *  version May. 17, 2025
 *  version Aug. 22, 2025
 *  version Sep. 21, 2025
 *  version Nov. 19, 2025
 * @version Feb.  5, 2026
 * @author  ASAMI, Tomoharu
 */
final case class Tree[A](
  root: TreeDir[A],
  meta: TreeMeta = TreeMeta.empty
) {

  def get(path: PathName): Option[TreeNode[A]] = {
    def _loop_(
      node: TreeNode[A],
      segments: Vector[String]
    ): Option[TreeNode[A]] =
      segments match {
        case Vector() =>
          Some(node)

        case head +: tail =>
          node match {
            case TreeDir(children) =>
              children
                .find(_.name == head)
              .flatMap(e => _loop_(e.node, tail))

            case _: TreeLeaf[A] =>
              None
          }
      }

    _loop_(root, path.segments)
  }

  def getLeaf(path: PathName): Option[TreeLeaf[A]] =
    get(path).collect { case l: TreeLeaf[A] => l }

  def getValue(path: PathName): Option[A] =
    getLeaf(path).map(_.value)

  def put(path: PathName, value: A): Tree[A] = {
    def _loop_(
      dir: TreeDir[A],
      segments: Vector[String]
    ): TreeDir[A] =
      segments match {
        case Vector(last) =>
          val idx = dir.children.indexWhere(_.name == last)
          val entry = TreeEntry(last, TreeLeaf(value))
          if (idx < 0)
            TreeDir(dir.children :+ entry)
          else
            TreeDir(dir.children.updated(idx, entry))

        case head +: tail =>
          val idx = dir.children.indexWhere(_.name == head)
          if (idx < 0) {
            val newDir = TreeDir[A](Vector.empty)
            val updated = _loop_(newDir, tail)
            TreeDir(dir.children :+ TreeEntry(head, updated))
          } else {
            dir.children(idx).node match {
              case d: TreeDir[A] =>
                val updated = _loop_(d, tail)
                TreeDir(
                  dir.children.updated(
                    idx,
                    TreeEntry(head, updated)
                  )
                )

              case _: TreeLeaf[A] =>
                // promote leaf to dir
                val newDir = TreeDir[A](Vector.empty)
                val updated = _loop_(newDir, tail)
                TreeDir(
                  dir.children.updated(
                    idx,
                    TreeEntry(head, updated)
                  )
                )
            }
          }

        case Vector() =>
          dir
      }

    copy(root = _loop_(root, path.segments))
  }

  def remove(path: PathName): Tree[A] = {
    def _loop_(
      dir: TreeDir[A],
      segments: Vector[String]
    ): TreeDir[A] =
      segments match {
        case Vector(last) =>
          TreeDir(dir.children.filterNot(_.name == last))

        case head +: tail =>
          val idx = dir.children.indexWhere(_.name == head)
          if (idx < 0) {
            dir
          } else {
            dir.children(idx).node match {
              case d: TreeDir[A] =>
                val updated = _loop_(d, tail)
                TreeDir(
                  dir.children.updated(
                    idx,
                    TreeEntry(head, updated)
                  )
                )
              case _: TreeLeaf[A] =>
                dir
            }
          }

        case Vector() =>
          dir
      }

    copy(root = _loop_(root, path.segments))
  }

  def walk(f: (PathName, TreeNode[A]) => Unit): Unit = {
    def _loop_(
      node: TreeNode[A],
      path: PathName
    ): Unit = {
      f(path, node)
      node match {
        case TreeDir(children) =>
          children.foreach { e =>
            _loop_(e.node, path / e.name)
          }
        case _: TreeLeaf[A] =>
          ()
      }
    }

    _loop_(root, PathName.Root)
  }

  def map[B](f: A => B): Tree[B] = {
    def _loop_(node: TreeNode[A]): TreeNode[B] =
      node match {
        case TreeDir(children) =>
          TreeDir(
            children.map { e =>
              TreeEntry(e.name, _loop_(e.node))
            }
          )
        case TreeLeaf(v, m) =>
          TreeLeaf(f(v), m)
      }

    Tree(_loop_(root).asInstanceOf[TreeDir[B]], meta)
  }
}

/*
 * Internal tree model
 */

sealed trait TreeNode[A]

final case class TreeDir[A](
  children: Vector[TreeEntry[A]]
) extends TreeNode[A]

final case class TreeLeaf[A](
  value: A,
  meta: TreeNodeMeta = TreeNodeMeta.empty
) extends TreeNode[A]

final case class TreeEntry[A](
  name: String,
  node: TreeNode[A]
)

/*
 * Metadata
 */

final case class TreeMeta(
  hints: Map[String, String] = Map.empty
)

object TreeMeta {
  val empty: TreeMeta = TreeMeta()
}

final case class TreeNodeMeta(
  attrs: Map[String, String] = Map.empty
)

object TreeNodeMeta {
  val empty: TreeNodeMeta = TreeNodeMeta()
}

object Tree {

  /*
   * Record -> Tree
   * Promote a semi-structured Record into a structured Tree.
   * Promotion succeeds only if the Record is recursively tree-shaped.
   */
  def fromRecord[A](record: Record): Consequence[Tree[A]] =
    _from_record_dir[A](record).map(Tree(_))

  private def _from_record_dir[A](record: Record): Consequence[TreeDir[A]] = {
    val builder = new TreeBuilderNode
    val result =
      record.fields.foldLeft[Either[String, Unit]](Right(())) { (acc, field) =>
        acc.flatMap { _ =>
          field.value match {
            case Field.Value.Single(v) =>
              _insert_field(builder, PathName.parse(field.key).segments, v)
          }
        }
      }

    result match {
      case Left(err) => Consequence.failure(err)
      case Right(_) => Consequence.success(builder.toTreeDir[A])
    }
  }

  private def _insert_field(
    node: TreeBuilderNode,
    segments: Vector[String],
    value: Any
  ): Either[String, Unit] = {
    segments match {
      case Vector() =>
        Left("empty path segment is not allowed")

      case head +: tail =>
        val child = node.children.getOrElseUpdate(head, new TreeBuilderNode)
        if (tail.isEmpty) {
          value match {
            case r: Record =>
              if (child.leaf.isDefined)
                Left(_conflict_message(segments))
              else {
                child.isDirectory = true
                _insert_record_fields(child, r)
              }
            case other =>
              if (child.isDirectory || child.leaf.isDefined)
                Left(_conflict_message(segments))
              else {
                child.leaf = Some(other)
                Right(())
              }
          }
        } else {
          if (child.leaf.isDefined)
            Left(_conflict_message(segments))
          else {
            child.isDirectory = true
            _insert_field(child, tail, value)
          }
        }
    }
  }

  private def _insert_record_fields(node: TreeBuilderNode, record: Record): Either[String, Unit] =
    record.fields.foldLeft[Either[String, Unit]](Right(())) { (acc, field) =>
      acc.flatMap { _ =>
        field.value match {
          case Field.Value.Single(v) =>
            _insert_field(node, PathName.parse(field.key).segments, v)
        }
      }
    }

  private def _conflict_message(segments: Vector[String]): String =
    s"conflict while promoting record path '${segments.mkString("/")}'"

  /*
   * Tree -> Record
   * Relax a structured Tree into a semi-structured Record.
   */
  def toRecord[A](tree: Tree[A]): Record = {
    val entries = _collect_leaf_entries(tree.root, PathName.Root)
    Record.data(entries: _*)
  }

  private def _collect_leaf_entries[A](
    node: TreeNode[A],
    path: PathName
  ): Vector[(String, Any)] =
    node match {
      case TreeLeaf(value, _) =>
        Vector(path.asString('/') -> value)

      case TreeDir(children) =>
        children.foldLeft(Vector.empty[(String, Any)]) { (acc, entry) =>
          acc ++ _collect_leaf_entries(entry.node, path / entry.name)
        }
    }

  private final class TreeBuilderNode {
    val children: LinkedHashMap[String, TreeBuilderNode] = LinkedHashMap.empty
    var leaf: Option[Any] = None
    var isDirectory: Boolean = false

    def toTreeDir[A]: TreeDir[A] =
      TreeDir(children.map { case (name, node) => TreeEntry(name, node.toTreeNode[A]) }.toVector)

    def toTreeNode[A]: TreeNode[A] =
      if (children.nonEmpty || isDirectory)
        TreeDir(children.map { case (name, node) => TreeEntry(name, node.toTreeNode[A]) }.toVector)
      else
        TreeLeaf(leaf.get.asInstanceOf[A])
  }
}
