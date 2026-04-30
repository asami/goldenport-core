package org.goldenport.datatype

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.util.zip.{ZipEntry, ZipInputStream, ZipOutputStream}
import org.goldenport.Consequence
import org.goldenport.bag.Bag
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Apr. 30, 2026
 * @version Apr. 30, 2026
 * @author  ASAMI, Tomoharu
 */
final class FileBundleSpec extends AnyWordSpec with Matchers {
  "FileBundle" should {
    "keep a directory path internally and expose root-relative view paths" in {
      val root = Files.createTempDirectory("filebundle-directory")
      Files.createDirectories(root.resolve("META-INF"))
      Files.writeString(root.resolve("META-INF").resolve("bundle.yaml"), "name: bundle\n", StandardCharsets.UTF_8)
      Files.writeString(root.resolve("index.html"), "<article>body</article>\n", StandardCharsets.UTF_8)

      val result = FileBundle.create("bundle", root)

      result shouldBe a[Consequence.Success[_]]
      result match {
        case Consequence.Success(bundle: FileBundle.Directory) =>
          bundle.root shouldBe root.toAbsolutePath.normalize()
          val view = bundle.toFileSystemView.TAKE
          view.listUnsafe.map(_.path.toString).toVector should contain theSameElementsAs Vector("META-INF", "index.html")
          view.getUnsafe(Paths.get("index.html")).map(_.path.toString) shouldBe Some("index.html")
          view.getUnsafe(Paths.get("META-INF").resolve("bundle.yaml")).map(_.path.toString) shouldBe Some("META-INF/bundle.yaml")
          val meta = view.getUnsafe(Paths.get("META-INF")).collect { case d: org.goldenport.vfs.FileSystemView.Node.Directory => d }
          meta.flatMap(_.getFileUnsafe(Paths.get("../index.html"))) shouldBe None
          view.getUnsafe(Paths.get("../outside.md")) shouldBe None
        case other =>
          fail(s"expected directory bundle: ${other}")
      }
    }

    "keep a single file path internally and expose a basename root entry" in {
      val file = Files.createTempFile("filebundle-single", ".html")
      Files.writeString(file, "<article>single</article>\n", StandardCharsets.UTF_8)

      val result = FileBundle.create("bundle", file)

      result shouldBe a[Consequence.Success[_]]
      result match {
        case Consequence.Success(bundle: FileBundle.SingleFile) =>
          bundle.file shouldBe file.toAbsolutePath.normalize()
          val view = bundle.toFileSystemView.TAKE
          view.listUnsafe.map(_.path.toString).toVector shouldBe Vector(file.getFileName.toString)
          view.getFileUnsafe(file.getFileName).map(_.filename) shouldBe Some(file.getFileName.toString)
          view.getUnsafe(Paths.get("../outside.md")) shouldBe None
        case other =>
          fail(s"expected single file bundle: ${other}")
      }
    }

    "keep an existing zip path internally and expose zip entries as view paths" in {
      val zip = Files.createTempFile("filebundle-existing", ".zip")
      val bytes = _zip_bytes(Vector("META-INF/bundle.yaml" -> "name: zip\n", "index.html" -> "<article>zip</article>\n"))
      Files.write(zip, bytes)

      val result = FileBundle.create("bundle", zip)

      result shouldBe a[Consequence.Success[_]]
      result match {
        case Consequence.Success(bundle: FileBundle.ZipFile) =>
          bundle.file shouldBe zip.toAbsolutePath.normalize()
          val view = bundle.toFileSystemView.TAKE
          view.listUnsafe.map(_.path.toString).toVector should contain theSameElementsAs Vector("META-INF", "index.html")
          view.getFileUnsafe(Paths.get("index.html")).map(_.filename) shouldBe Some("index.html")
          val meta = view.getUnsafe(Paths.get("META-INF")).collect { case d: org.goldenport.vfs.FileSystemView.Node.Directory => d }
          meta.flatMap(_.getFileUnsafe(Paths.get("../index.html"))) shouldBe None
        case other =>
          fail(s"expected zip file bundle: ${other}")
      }
    }

    "keep application zip MimeBody internally" in {
      val bytes = _zip_bytes(Vector("index.html" -> "<article>zip</article>\n"))
      val body = MimeBody(ContentType.APPLICATION_ZIP, Bag.binary(bytes))

      val result = FileBundle.create("bundle", body)

      result shouldBe a[Consequence.Success[_]]
      result match {
        case Consequence.Success(bundle: FileBundle.ZipMime) =>
          bundle.body.contentType shouldBe ContentType.APPLICATION_ZIP
          bundle.toMimeBody.map(x => _bag_bytes(x.value).toVector) shouldBe Consequence.success(bytes.toVector)
        case other =>
          fail(s"expected zip mime bundle: ${other}")
      }
    }

    "provide transport MimeBody conversion for directory and single file inputs" in {
      val root = Files.createTempDirectory("filebundle-transport-directory")
      Files.writeString(root.resolve("index.html"), "<article>body</article>\n", StandardCharsets.UTF_8)
      val file = Files.createTempFile("filebundle-transport-single", ".html")
      Files.writeString(file, "<article>single</article>\n", StandardCharsets.UTF_8)

      FileBundle.create("bundle", root).flatMap(_.toMimeBody).map(_zip_entries) shouldBe Consequence.success(Vector("index.html"))
      FileBundle.create("bundle", file).flatMap(_.toMimeBody).map(_zip_entries) shouldBe Consequence.success(Vector(file.getFileName.toString))
    }

    "provide the generated-code compatible Filebundle type alias" in {
      val body = MimeBody(ContentType.APPLICATION_ZIP, Bag.binary(_zip_bytes(Vector("index.html" -> "body"))))

      val bundle: Filebundle = FileBundle(body)

      bundle shouldBe FileBundle.ZipMime(body)
    }

    "reject invalid filebundle inputs deterministically" in {
      val invalid = Files.createTempFile("filebundle-invalid", ".zip")
      Files.writeString(invalid, "not a zip", StandardCharsets.UTF_8)
      val empty = Files.createTempDirectory("filebundle-empty")
      val nonZipBody = MimeBody(ContentType.TEXT_PLAIN, Bag.text("text"))
      val invalidZipBody = MimeBody(ContentType.APPLICATION_ZIP, Bag.text("not a zip"))
      val directoryOnlyZip = Files.createTempFile("filebundle-directory-only", ".zip")
      Files.write(directoryOnlyZip, _zip_entries_bytes(Vector("META-INF/" -> None)))
      val escapeZip = Files.createTempFile("filebundle-escape", ".zip")
      Files.write(escapeZip, _zip_bytes(Vector("../evil.txt" -> "evil")))
      val absoluteZip = Files.createTempFile("filebundle-absolute", ".zip")
      Files.write(absoluteZip, _zip_bytes(Vector("/evil.txt" -> "evil")))

      FileBundle.create("bundle", invalid) shouldBe a[Consequence.Failure[_]]
      FileBundle.create("bundle", empty) shouldBe a[Consequence.Failure[_]]
      FileBundle.create("bundle", nonZipBody) shouldBe a[Consequence.Failure[_]]
      FileBundle.create("bundle", invalidZipBody) shouldBe a[Consequence.Failure[_]]
      FileBundle.create("bundle", directoryOnlyZip) shouldBe a[Consequence.Failure[_]]
      FileBundle.create("bundle", escapeZip) shouldBe a[Consequence.Failure[_]]
      FileBundle.create("bundle", absoluteZip) shouldBe a[Consequence.Failure[_]]
      FileBundle(invalidZipBody).toFileSystemView shouldBe a[Consequence.Failure[_]]
    }
  }

  private def _zip_entries(body: MimeBody): Vector[String] = {
    val in = new ZipInputStream(body.value.openInputStream(), StandardCharsets.UTF_8)
    try {
      Iterator.continually(in.getNextEntry).takeWhile(_ != null).map(_.getName).toVector
    } finally {
      in.close()
    }
  }

  private def _bag_bytes(bag: Bag): Array[Byte] = {
    val in = bag.openInputStream()
    try {
      in.readAllBytes()
    } finally {
      in.close()
    }
  }

  private def _zip_bytes(entries: Vector[(String, String)]): Array[Byte] = {
    _zip_entries_bytes(entries.map { case (name, text) => name -> Some(text) })
  }

  private def _zip_entries_bytes(entries: Vector[(String, Option[String])]): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    val zip = new ZipOutputStream(out, StandardCharsets.UTF_8)
    try {
      entries.foreach { case (name, text) =>
        zip.putNextEntry(new ZipEntry(name))
        text.foreach(x => zip.write(x.getBytes(StandardCharsets.UTF_8)))
        zip.closeEntry()
      }
    } finally {
      zip.close()
    }
    out.toByteArray
  }
}
