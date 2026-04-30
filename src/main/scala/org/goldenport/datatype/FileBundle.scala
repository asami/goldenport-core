package org.goldenport.datatype

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import java.util.zip.{ZipEntry, ZipInputStream, ZipOutputStream}
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal
import org.goldenport.Consequence
import org.goldenport.bag.Bag
import org.goldenport.vfs.FileSystemView

/*
 * @since   Apr. 30, 2026
 * @version Apr. 30, 2026
 * @author  ASAMI, Tomoharu
 */
sealed abstract class FileBundle extends DataType() {
  import FileBundle.*

  def kind: String
  def toMimeBody: Consequence[MimeBody]
  def toFileSystemView: Consequence[FileSystemView]

  override def display: String = s"FileBundle(${kind})"
  override def show: String = display
  override final def print: String = display
}

type Filebundle = FileBundle

object FileBundle {
  final case class Directory(root: Path) extends FileBundle {
    def kind: String = "directory"
    def toMimeBody: Consequence[MimeBody] =
      _zip_directory(root)
    def toFileSystemView: Consequence[FileSystemView] =
      Consequence.success(RootRelativeDirectoryFileSystemView(root))
  }

  final case class SingleFile(file: Path) extends FileBundle {
    def kind: String = "single-file"
    def toMimeBody: Consequence[MimeBody] =
      _single_file(file)
    def toFileSystemView: Consequence[FileSystemView] =
      Consequence.success(SingleFileSystemView(file))
  }

  final case class ZipFile(file: Path) extends FileBundle {
    def kind: String = "zip-file"
    def toMimeBody: Consequence[MimeBody] =
      _existing_zip(file)
    def toFileSystemView: Consequence[FileSystemView] =
      try {
        _zip_file_system_view(Files.readAllBytes(file))
      } catch {
        case NonFatal(e) =>
          Consequence.argumentInvalid(s"filebundle zip view failed: ${Option(e.getMessage).getOrElse(e.getClass.getSimpleName)}")
      }
  }

  final case class ZipMime(body: MimeBody) extends FileBundle {
    def kind: String = "zip-mime"
    def toMimeBody: Consequence[MimeBody] =
      _validate_mime_body(body).map(_ => body)
    def toFileSystemView: Consequence[FileSystemView] =
      try {
        _zip_file_system_view(_bag_bytes(body.value))
      } catch {
        case NonFatal(e) =>
          Consequence.argumentInvalid(s"filebundle zip view failed: ${Option(e.getMessage).getOrElse(e.getClass.getSimpleName)}")
      }
  }

  def apply(body: MimeBody): FileBundle =
    ZipMime(body)

  def create(
    name: String,
    value: Any
  ): Consequence[FileBundle] =
    value match {
      case bundle: FileBundle => Consequence.success(bundle)
      case body: MimeBody => _validate_mime_body(body).map(_ => ZipMime(body))
      case path: Path => create(name, path)
      case text: String => create(name, Paths.get(text))
      case other => Consequence.argumentInvalid(s"filebundle parameter ${name} must be a directory, file, or zip path: ${other}")
    }

  def create(
    name: String,
    path: Path
  ): Consequence[FileBundle] = {
    val root = path.toAbsolutePath.normalize()
    if (!Files.exists(root)) {
      Consequence.argumentInvalid(s"filebundle parameter ${name} path does not exist: ${root}")
    } else if (Files.isSymbolicLink(root)) {
      Consequence.argumentInvalid(s"filebundle parameter ${name} symbolic link is not supported: ${root}")
    } else if (Files.isDirectory(root)) {
      _validate_directory(name, root).map(Directory(_))
    } else if (Files.isRegularFile(root)) {
      if (_is_zip_path(root))
        _validate_zip_file(name, root).map(ZipFile(_))
      else
        Consequence.success(SingleFile(root))
    } else {
      Consequence.argumentInvalid(s"filebundle parameter ${name} must be a directory, file, or zip path: ${root}")
    }
  }

  def mimeBody(
    name: String,
    value: Any
  ): Consequence[MimeBody] =
    create(name, value).flatMap(_.toMimeBody)

  def mimeBody(
    name: String,
    path: Path
  ): Consequence[MimeBody] =
    create(name, path).flatMap(_.toMimeBody)

  private def _validate_directory(
    name: String,
    root: Path
  ): Consequence[Path] =
    try {
      val files = _directory_files(root)
      if (files.isEmpty)
        Consequence.argumentInvalid(s"filebundle parameter ${name} directory is empty")
      else
        Consequence.success(root)
    } catch {
      case NonFatal(e) =>
        Consequence.argumentInvalid(s"filebundle parameter ${name} directory validation failed: ${Option(e.getMessage).getOrElse(e.getClass.getSimpleName)}")
    }

  private def _validate_zip_file(
    name: String,
    file: Path
  ): Consequence[Path] =
    try {
      _validate_zip_bytes(name, Files.readAllBytes(file)).map(_ => file)
    } catch {
      case NonFatal(e) =>
        Consequence.argumentInvalid(s"filebundle parameter ${name} zip read failed: ${Option(e.getMessage).getOrElse(e.getClass.getSimpleName)}")
    }

  private def _validate_mime_body(
    body: MimeBody
  ): Consequence[Unit] =
    if (body.contentType.mimeType == MimeType.APPLICATION_ZIP) {
      try {
        _validate_zip_bytes("body", _bag_bytes(body.value))
      } catch {
        case NonFatal(e) =>
          Consequence.argumentInvalid(s"filebundle parameter body zip read failed: ${Option(e.getMessage).getOrElse(e.getClass.getSimpleName)}")
      }
    } else {
      Consequence.argumentInvalid(s"filebundle parameter body must be application/zip: ${body.contentType.header}")
    }

  private def _zip_directory(
    root: Path
  ): Consequence[MimeBody] =
    try {
      Consequence.success(_zip_mime_body(_zip_directory_bytes(root)))
    } catch {
      case NonFatal(e) =>
        Consequence.argumentInvalid(s"filebundle parameter zip failed: ${Option(e.getMessage).getOrElse(e.getClass.getSimpleName)}")
    }

  private def _single_file(
    file: Path
  ): Consequence[MimeBody] =
    try {
      Consequence.success(_zip_mime_body(_zip_single_file_bytes(file)))
    } catch {
      case NonFatal(e) =>
        Consequence.argumentInvalid(s"filebundle parameter zip failed: ${Option(e.getMessage).getOrElse(e.getClass.getSimpleName)}")
    }

  private def _existing_zip(
    file: Path
  ): Consequence[MimeBody] =
    try {
      val bytes = Files.readAllBytes(file)
      _validate_zip_bytes("body", bytes).map(_ => _zip_mime_body(bytes))
    } catch {
      case NonFatal(e) =>
        Consequence.argumentInvalid(s"filebundle parameter zip read failed: ${Option(e.getMessage).getOrElse(e.getClass.getSimpleName)}")
    }

  private def _zip_mime_body(bytes: Array[Byte]): MimeBody =
    MimeBody(ContentType.APPLICATION_ZIP, Bag.binary(bytes))

  private def _is_zip_path(path: Path): Boolean =
    Option(path.getFileName).map(_.toString.toLowerCase(java.util.Locale.ROOT).endsWith(".zip")).getOrElse(false)

  private def _zip_single_file_bytes(file: Path): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    val zip = new ZipOutputStream(out, StandardCharsets.UTF_8)
    try {
      val entry = new ZipEntry(file.getFileName.toString)
      zip.putNextEntry(entry)
      Files.copy(file, zip)
      zip.closeEntry()
    } finally {
      zip.close()
    }
    out.toByteArray
  }

  private def _validate_zip_bytes(
    name: String,
    bytes: Array[Byte]
  ): Consequence[Unit] = {
    val in = new ZipInputStream(new java.io.ByteArrayInputStream(bytes), StandardCharsets.UTF_8)
    try {
      var hasFile = false
      var entry = in.getNextEntry
      while (entry != null) {
        _zip_entry_name_error(entry.getName) match {
          case Some(message) =>
            return Consequence.argumentInvalid(s"filebundle parameter ${name} ${message}: ${entry.getName}")
          case None =>
            ()
        }
        if (!entry.isDirectory)
          hasFile = true
        in.closeEntry()
        entry = in.getNextEntry
      }
      if (hasFile)
        Consequence.success(())
      else
        Consequence.argumentInvalid(s"filebundle parameter ${name} zip must contain at least one file")
    } catch {
      case NonFatal(e) =>
        Consequence.argumentInvalid(s"filebundle parameter ${name} zip is invalid: ${Option(e.getMessage).getOrElse(e.getClass.getSimpleName)}")
    } finally {
      in.close()
    }
  }

  private def _zip_entry_name_error(
    entryName: String
  ): Option[String] = {
    val normalized = Option(entryName).getOrElse("").replace('\\', '/')
    val segments = normalized.split('/').toVector.filter(_.nonEmpty)
    if (normalized.trim.isEmpty)
      Some("zip entry name is empty")
    else if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*"))
      Some("zip entry must be relative")
    else if (segments.exists(_ == ".."))
      Some("zip entry escapes bundle root")
    else
      None
  }

  private def _zip_directory_bytes(root: Path): Array[Byte] = {
    val files = _directory_files(root)
    if (files.isEmpty)
      throw new IllegalArgumentException("filebundle directory is empty")
    val out = new ByteArrayOutputStream()
    val zip = new ZipOutputStream(out, StandardCharsets.UTF_8)
    try {
      files.foreach { file =>
        val entry = new ZipEntry(_zip_entry_name(root, file))
        zip.putNextEntry(entry)
        Files.copy(file, zip)
        zip.closeEntry()
      }
    } finally {
      zip.close()
    }
    out.toByteArray
  }

  private def _directory_files(root: Path): Vector[Path] = {
    val all = {
      val stream = Files.walk(root)
      try {
        stream.iterator().asScala.toVector
      } finally {
        stream.close()
      }
    }
    all.find(Files.isSymbolicLink(_)).foreach { link =>
      throw new IllegalArgumentException(s"symbolic link is not supported: ${link}")
    }
    all.filter(p => Files.isRegularFile(p)).sortBy(p => _zip_entry_name(root, p))
  }

  private def _zip_entry_name(
    root: Path,
    file: Path
  ): String =
    root.relativize(file).iterator().asScala.map(_.toString).mkString("/")

  private def _bag_bytes(bag: Bag): Array[Byte] = {
    val in = bag.openInputStream()
    try {
      in.readAllBytes()
    } finally {
      in.close()
    }
  }

  private def _zip_file_system_view(bytes: Array[Byte]): Consequence[FileSystemView] =
    try {
      _validate_zip_bytes("body", bytes).map(_ => ZipFileBundleView(_read_zip_entries(bytes)))
    } catch {
      case NonFatal(e) =>
        Consequence.argumentInvalid(s"filebundle zip view failed: ${Option(e.getMessage).getOrElse(e.getClass.getSimpleName)}")
    }

  private def _read_zip_entries(bytes: Array[Byte]): Map[Path, Array[Byte]] = {
    val in = new ZipInputStream(new java.io.ByteArrayInputStream(bytes), StandardCharsets.UTF_8)
    try {
      val builder = Map.newBuilder[Path, Array[Byte]]
      var entry = in.getNextEntry
      while (entry != null) {
        if (!entry.isDirectory) {
          val out = new ByteArrayOutputStream()
          val buffer = new Array[Byte](8192)
          var read = in.read(buffer)
          while (read != -1) {
            out.write(buffer, 0, read)
            read = in.read(buffer)
          }
          builder += Paths.get(entry.getName.replace('\\', '/')) -> out.toByteArray
        }
        in.closeEntry()
        entry = in.getNextEntry
      }
      builder.result()
    } finally {
      in.close()
    }
  }

  private def _safe_relative(path: Path): Option[Path] = {
    val normalized = path.normalize()
    val parts = normalized.iterator().asScala.map(_.toString).toVector
    if (normalized.isAbsolute || parts.exists(_ == ".."))
      None
    else
      Some(normalized)
  }

  private def _safe_child_relative(path: Path): Option[Path] = {
    val rawParts = path.iterator().asScala.map(_.toString).toVector
    val normalized = path.normalize()
    val normalizedParts = normalized.iterator().asScala.map(_.toString).toVector
    if (path.isAbsolute || rawParts.exists(_ == "..") || normalizedParts.exists(_ == ".."))
      None
    else
      Some(normalized)
  }

  private def _file_content(path: Path, bytes: Array[Byte]): FileContent = {
    val filename = Option(path.getFileName).map(_.toString).getOrElse(path.toString)
    val mimeType = filename.lastIndexOf('.') match {
      case idx if idx >= 0 => MimeType.fromSuffix(filename.substring(idx + 1))
      case _ => None
    }
    FileContent(filename, Bag.binary(bytes), mimeType)
  }

  private final case class RootRelativeDirectoryFileSystemView(root: Path) extends FileSystemView {
    import FileSystemView.Node

    override def list: Consequence[Iterator[Node]] =
      Consequence {
        _list_directory(root, Paths.get(""))
      }

    override def get(path: Path): Consequence[Option[Node]] =
      Consequence {
        _safe_relative(path).flatMap { relative =>
          val abs = root.resolve(relative).normalize()
          if (!abs.startsWith(root.normalize())) None
          else if (Files.isDirectory(abs)) Some(DirectoryNode(root, relative))
          else if (Files.isRegularFile(abs)) Some(Node.File(relative, FileContent.create(abs)))
          else None
        }
      }

    override def getFile(path: Path): Consequence[Option[FileContent]] =
      Consequence {
        _safe_relative(path).flatMap { relative =>
          val abs = root.resolve(relative).normalize()
          if (abs.startsWith(root.normalize()) && Files.isRegularFile(abs))
            Some(FileContent.create(abs))
          else
            None
        }
      }

    override def exists(path: Path): Consequence[Boolean] =
      Consequence {
        _safe_relative(path).exists { relative =>
          val abs = root.resolve(relative).normalize()
          abs.startsWith(root.normalize()) && Files.exists(abs)
        }
      }
  }

  private final case class DirectoryNode(root: Path, path: Path) extends FileSystemView.Node.Directory {
    import FileSystemView.Node

    override def list: Consequence[Iterator[Node]] =
      Consequence {
        _list_directory(root, path)
      }

    override def get(subpath: Path): Consequence[Option[Node]] =
      Consequence {
        _safe_child_relative(subpath).flatMap(child => _safe_relative(path.resolve(child))).flatMap { relative =>
          val abs = root.resolve(relative).normalize()
          if (!abs.startsWith(root.normalize())) None
          else if (Files.isDirectory(abs)) Some(DirectoryNode(root, relative))
          else if (Files.isRegularFile(abs)) Some(Node.File(relative, FileContent.create(abs)))
          else None
        }
      }

    override def getFile(subpath: Path): Consequence[Option[FileContent]] =
      Consequence {
        _safe_child_relative(subpath).flatMap(child => _safe_relative(path.resolve(child))).flatMap { relative =>
          val abs = root.resolve(relative).normalize()
          if (abs.startsWith(root.normalize()) && Files.isRegularFile(abs))
            Some(FileContent.create(abs))
          else
            None
        }
      }

    override def exists(subpath: Path): Consequence[Boolean] =
      Consequence {
        _safe_child_relative(subpath).flatMap(child => _safe_relative(path.resolve(child))).exists { relative =>
          val abs = root.resolve(relative).normalize()
          abs.startsWith(root.normalize()) && Files.exists(abs)
        }
      }
  }

  private def _list_directory(root: Path, relative: Path): Iterator[FileSystemView.Node] = {
    import FileSystemView.Node
    val abs = root.resolve(relative).normalize()
    if (!abs.startsWith(root.normalize()) || !Files.isDirectory(abs)) {
      Iterator.empty
    } else {
      val ds = Files.newDirectoryStream(abs)
      try {
        ds.iterator().asScala.toVector.sortBy(_.getFileName.toString).iterator.flatMap { child =>
          val childRelative = root.relativize(child).normalize()
          if (Files.isDirectory(child))
            Some(DirectoryNode(root, childRelative))
          else if (Files.isRegularFile(child))
            Some(Node.File(childRelative, FileContent.create(child)))
          else
            None
        }
      } finally {
        ds.close()
      }
    }
  }

  private final case class SingleFileSystemView(file: Path) extends FileSystemView {
    import FileSystemView.Node

    private val logicalPath = file.getFileName

    override def list: Consequence[Iterator[Node]] =
      Consequence.success(Iterator.single(Node.File(logicalPath, FileContent.create(file))))

    override def get(path: Path): Consequence[Option[Node]] =
      Consequence.success {
        _safe_relative(path).filter(_ == logicalPath).map(_ => Node.File(logicalPath, FileContent.create(file)))
      }

    override def getFile(path: Path): Consequence[Option[FileContent]] =
      Consequence.success {
        _safe_relative(path).filter(_ == logicalPath).map(_ => FileContent.create(file))
      }

    override def exists(path: Path): Consequence[Boolean] =
      Consequence.success(_safe_relative(path).contains(logicalPath))
  }

  private final case class ZipFileBundleView(files: Map[Path, Array[Byte]]) extends FileSystemView {
    import FileSystemView.Node

    override def list: Consequence[Iterator[Node]] =
      Consequence.success(_zip_children(Paths.get("")).iterator)

    override def get(path: Path): Consequence[Option[Node]] =
      Consequence.success {
        _safe_relative(path).flatMap(_zip_node)
      }

    override def getFile(path: Path): Consequence[Option[FileContent]] =
      Consequence.success {
        _safe_relative(path).flatMap(files.get).map(bytes => _file_content(path, bytes))
      }

    override def exists(path: Path): Consequence[Boolean] =
      Consequence.success(_safe_relative(path).exists(p => files.contains(p) || _has_descendant(p)))

    private def _zip_node(path: Path): Option[Node] =
      files.get(path).map(bytes => Node.File(path, _file_content(path, bytes))).
        orElse(Option.when(_has_descendant(path))(ZipDirectoryNode(this, path)))

    private[FileBundle] def children(path: Path): Vector[Node] =
      _zip_children(path)

    private[FileBundle] def file(path: Path): Option[FileContent] =
      files.get(path).map(bytes => _file_content(path, bytes))

    private[FileBundle] def node(path: Path): Option[Node] =
      _zip_node(path)

    private[FileBundle] def existsPath(path: Path): Boolean =
      files.contains(path) || _has_descendant(path)

    private def _zip_children(parent: Path): Vector[Node] = {
      val parentParts = parent.iterator().asScala.map(_.toString).toVector.filter(_.nonEmpty)
      val children = files.keys.toVector.flatMap { path =>
        val parts = path.iterator().asScala.map(_.toString).toVector
        if (parts.take(parentParts.length) != parentParts || parts.length <= parentParts.length)
          None
        else {
          val child = Paths.get(parts.take(parentParts.length + 1).mkString("/"))
          if (parts.length == parentParts.length + 1)
            files.get(child).map(bytes => Node.File(child, _file_content(child, bytes)))
          else
            Some(ZipDirectoryNode(this, child))
        }
      }
      children.groupBy(_.path).values.map(_.head).toVector.sortBy(_.path.toString)
    }

    private def _has_descendant(path: Path): Boolean = {
      val parts = path.iterator().asScala.map(_.toString).toVector.filter(_.nonEmpty)
      files.keys.exists { file =>
        val fileParts = file.iterator().asScala.map(_.toString).toVector
        fileParts.length > parts.length && fileParts.take(parts.length) == parts
      }
    }
  }

  private final case class ZipDirectoryNode(view: ZipFileBundleView, path: Path) extends FileSystemView.Node.Directory {
    import FileSystemView.Node

    override def list: Consequence[Iterator[Node]] =
      Consequence.success(view.children(path).iterator)

    override def get(subpath: Path): Consequence[Option[Node]] =
      Consequence.success(_safe_child_relative(subpath).flatMap(child => _safe_relative(path.resolve(child))).flatMap(view.node))

    override def getFile(subpath: Path): Consequence[Option[FileContent]] =
      Consequence.success(_safe_child_relative(subpath).flatMap(child => _safe_relative(path.resolve(child))).flatMap(view.file))

    override def exists(subpath: Path): Consequence[Boolean] =
      Consequence.success(_safe_child_relative(subpath).flatMap(child => _safe_relative(path.resolve(child))).exists(view.existsPath))
  }
}
