package org.goldenport.datatype

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import java.util.zip.{ZipEntry, ZipInputStream, ZipOutputStream}
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal
import org.goldenport.Consequence
import org.goldenport.bag.Bag

/*
 * @since   Apr. 30, 2026
 * @version Apr. 30, 2026
 * @author  ASAMI, Tomoharu
 */
final case class FileBundle(
  body: MimeBody
) extends DataType() {
  override def display: String = s"FileBundle(${body.contentType.display})"
  override def show: String = s"FileBundle(${body.show})"
  override final def print: String = display
}

type Filebundle = FileBundle

object FileBundle {
  def mimeBody(
    name: String,
    value: Any
  ): Consequence[MimeBody] =
    value match {
      case body: MimeBody => _validate_mime_body(name, body)
      case path: Path => mimeBody(name, path)
      case text: String => mimeBody(name, Paths.get(text))
      case other => Consequence.argumentInvalid(s"filebundle parameter ${name} must be a directory, file, or zip path: ${other}")
    }

  def mimeBody(
    name: String,
    path: Path
  ): Consequence[MimeBody] = {
    val root = path.toAbsolutePath.normalize()
    if (!Files.exists(root)) {
      Consequence.argumentInvalid(s"filebundle parameter ${name} path does not exist: ${root}")
    } else if (Files.isSymbolicLink(root)) {
      Consequence.argumentInvalid(s"filebundle parameter ${name} symbolic link is not supported: ${root}")
    } else if (Files.isDirectory(root)) {
      _zip_directory(name, root)
    } else if (Files.isRegularFile(root)) {
      if (_is_zip_path(root))
        _existing_zip(name, root)
      else
        _single_file(name, root)
    } else {
      Consequence.argumentInvalid(s"filebundle parameter ${name} must be a directory, file, or zip path: ${root}")
    }
  }

  def apply(
    name: String,
    value: Any
  ): Consequence[FileBundle] =
    mimeBody(name, value).map(FileBundle(_))

  private def _validate_mime_body(
    name: String,
    body: MimeBody
  ): Consequence[MimeBody] =
    if (body.contentType.mimeType == MimeType.APPLICATION_ZIP) {
      try {
        val in = body.value.openInputStream()
        try {
          _validate_zip_bytes(name, in.readAllBytes()).map(_ => body)
        } finally {
          in.close()
        }
      } catch {
        case NonFatal(e) =>
          Consequence.argumentInvalid(s"filebundle parameter ${name} zip read failed: ${Option(e.getMessage).getOrElse(e.getClass.getSimpleName)}")
      }
    } else {
      Consequence.argumentInvalid(s"filebundle parameter ${name} must be application/zip: ${body.contentType.header}")
    }

  private def _zip_directory(
    name: String,
    root: Path
  ): Consequence[MimeBody] =
    try {
      val bytes = _zip_directory_bytes(root)
      Consequence.success(_zip_mime_body(bytes))
    } catch {
      case NonFatal(e) =>
        Consequence.argumentInvalid(s"filebundle parameter ${name} zip failed: ${Option(e.getMessage).getOrElse(e.getClass.getSimpleName)}")
    }

  private def _single_file(
    name: String,
    file: Path
  ): Consequence[MimeBody] =
    try {
      val bytes = _zip_single_file_bytes(file)
      Consequence.success(_zip_mime_body(bytes))
    } catch {
      case NonFatal(e) =>
        Consequence.argumentInvalid(s"filebundle parameter ${name} zip failed: ${Option(e.getMessage).getOrElse(e.getClass.getSimpleName)}")
    }

  private def _existing_zip(
    name: String,
    file: Path
  ): Consequence[MimeBody] =
    try {
      val bytes = Files.readAllBytes(file)
      _validate_zip_bytes(name, bytes).map(_ => _zip_mime_body(bytes))
    } catch {
      case NonFatal(e) =>
        Consequence.argumentInvalid(s"filebundle parameter ${name} zip read failed: ${Option(e.getMessage).getOrElse(e.getClass.getSimpleName)}")
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
    val files = all
      .filter(p => Files.isRegularFile(p))
      .sortBy(p => _zip_entry_name(root, p))
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

  private def _zip_entry_name(
    root: Path,
    file: Path
  ): String =
    root.relativize(file).iterator().asScala.map(_.toString).mkString("/")
}
