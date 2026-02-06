package org.goldenport.vfs

import java.io.InputStream
import java.nio.file.{Files, Path}
import org.goldenport.Consequence
import org.goldenport.datatype.{FileContent, MimeType}
import org.goldenport.bag.Bag
import scala.util.control.NonFatal

/**
 * Read-only, lazy view of a filesystem.
 *
 * This is NOT an OS-level FileSystem.
 * It represents a view over execution results
 * (shell, docker, remote, etc.).
 */
/*
 * @since   Feb.  6, 2026
 * @version Feb.  6, 2026
 * @author  ASAMI, Tomoharu
 */
trait FileSystemView {
  import FileSystemView._

  /** Lists entries directly under this directory. */
  def list: Consequence[Iterator[Node]]

  def listUnsafe: Iterator[Node] = list.TAKE

  /** Opens a file as a stream. */
  def get(path: Path): Consequence[Option[Node]]

  def getUnsafe(path: Path): Option[Node] = get(path).TAKE

  def getCollapse(path: Path): Option[Node] = get(path).collapseOption

  def getFile(path: Path): Consequence[Option[FileContent]]

  def getFileUnsafe(path: Path): Option[FileContent] = getFile(path).TAKE

  def getFileCollapse(path: Path): Option[FileContent] = getFile(path).collapseOption

  /** Checks existence of a path in this view. */
  def exists(path: Path): Consequence[Boolean]

  def existsUnsafe(path: Path): Boolean = exists(path).TAKE

  def existsCollapse(path: Path): Boolean = exists(path).getOrElse(false)
}

object FileSystemView {
  trait Node {
    def path: Path
  }
  object Node {
    case class File(path: Path, file: FileContent) extends Node()
    trait Directory extends Node {
      /** Lists entries directly under this directory. */
      def list: Consequence[Iterator[Node]]

      def listUnsafe: Iterator[Node] = list.TAKE

      /** Opens a file as a stream. */
      def get(path: Path): Consequence[Option[Node]]

      def getUnsafe(path: Path): Option[Node] = get(path).TAKE

      def getCollapse(path: Path): Option[Node] = get(path).collapseOption

      def getFile(path: Path): Consequence[Option[FileContent]]

      def getFileUnsafe(path: Path): Option[FileContent] = getFile(path).TAKE

      def getFileCollapse(path: Path): Option[FileContent] = getFile(path).collapseOption

      /** Checks existence of a path in this view. */
      def exists(path: Path): Consequence[Boolean]

      def existsUnsafe(path: Path): Boolean = exists(path).TAKE

      def existsCollapse(path: Path): Boolean = exists(path).getOrElse(false)
    }
  }
}

import scala.jdk.CollectionConverters._

case class DirectoryFileSystemView(root: Path) extends FileSystemView {
  import FileSystemView._
  import DirectoryFileSystemView._

  /** Lists entries directly under this directory (root). */
  override def list: Consequence[Iterator[Node]] = Consequence {
      if (!Files.isDirectory(root)) Iterator.empty
      else {
        val ds = Files.newDirectoryStream(root)
        try {
          ds.iterator().asScala.map { child =>
            if (Files.isDirectory(child)) DirectoryDirectory(child)
            else if (Files.isRegularFile(child)) Node.File(child, FileContent.create(child))
            else null
          }.filter(_ != null)
        } finally {
          ds.close()
        }
      }
  }

  /** Gets a file or directory node at the given path (relative to root). */
  override def get(path: Path): Consequence[Option[Node]] = Consequence {
    val abs = root.resolve(path).normalize()
    if (!abs.startsWith(root.normalize())) None
    else if (Files.isDirectory(abs)) Some(DirectoryDirectory(abs))
    else if (Files.isRegularFile(abs)) Some(Node.File(abs, FileContent.create(abs)))
    else None
  }

  override def getFile(path: Path): Consequence[Option[FileContent]] = Consequence {
    val abs = root.resolve(path).normalize()
    if (!abs.startsWith(root.normalize())) None
    else if (Files.isRegularFile(abs)) Some(FileContent.create(abs))
    else None
  }

  override def exists(path: Path): Consequence[Boolean] = Consequence {
    val abs = root.resolve(path).normalize()
    abs.startsWith(root.normalize()) && Files.exists(abs)
  }
}

object DirectoryFileSystemView {
  case class DirectoryDirectory(path: Path) extends FileSystemView.Node.Directory {
    import FileSystemView.Node
    import DirectoryFileSystemView.DirectoryDirectory
    import scala.jdk.CollectionConverters._
    /** Lists entries directly under this directory. */
    override def list: Consequence[Iterator[Node]] = Consequence {
        if (!Files.isDirectory(path)) Iterator.empty
        else {
          val ds = Files.newDirectoryStream(path)
          try {
            ds.iterator().asScala.map { child =>
              if (Files.isDirectory(child)) DirectoryDirectory(child)
              else if (Files.isRegularFile(child)) Node.File(child, FileContent.create(child))
              else null
            }.filter(_ != null)
          } finally {
            ds.close()
          }
        }
    }

    /** Gets a file or directory node at the given path (relative to this directory). */
    override def get(subpath: Path): Consequence[Option[Node]] = Consequence {
      val abs = path.resolve(subpath).normalize()
      if (!abs.startsWith(path.normalize())) None
    else if (Files.isDirectory(abs)) Some(DirectoryDirectory(abs))
    else if (Files.isRegularFile(abs)) Some(Node.File(abs, FileContent.create(abs)))
      else None
    }

    override def getFile(subpath: Path): Consequence[Option[FileContent]] = Consequence {
      val abs = path.resolve(subpath).normalize()
      if (!abs.startsWith(path.normalize())) None
    else if (Files.isRegularFile(abs)) Some(FileContent.create(abs))
      else None
    }

    override def exists(subpath: Path): Consequence[Boolean] = Consequence {
      val abs = path.resolve(subpath).normalize()
      abs.startsWith(path.normalize()) && Files.exists(abs)
    }
  }
}
