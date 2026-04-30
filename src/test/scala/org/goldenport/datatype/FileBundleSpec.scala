package org.goldenport.datatype

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
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
    "create an application zip MimeBody from a directory" in {
      val root = Files.createTempDirectory("filebundle-directory")
      Files.createDirectories(root.resolve("META-INF"))
      Files.writeString(root.resolve("META-INF").resolve("bundle.yaml"), "name: bundle\n", StandardCharsets.UTF_8)
      Files.writeString(root.resolve("index.html"), "<article>body</article>\n", StandardCharsets.UTF_8)

      val result = FileBundle.mimeBody("bundle", root)

      result shouldBe a[Consequence.Success[_]]
      result match {
        case Consequence.Success(body) =>
          body.contentType shouldBe ContentType.APPLICATION_ZIP
          _zip_entries(body) should contain theSameElementsAs Vector("META-INF/bundle.yaml", "index.html")
        case _ => fail("expected success")
      }
    }

    "create a one-entry zip MimeBody from a single file" in {
      val file = Files.createTempFile("filebundle-single", ".html")
      Files.writeString(file, "<article>single</article>\n", StandardCharsets.UTF_8)

      val result = FileBundle.mimeBody("bundle", file)

      result shouldBe a[Consequence.Success[_]]
      result match {
        case Consequence.Success(body) =>
          body.contentType shouldBe ContentType.APPLICATION_ZIP
          _zip_entries(body) shouldBe Vector(file.getFileName.toString)
        case _ => fail("expected success")
      }
    }

    "reuse an existing zip MimeBody without rezipping" in {
      val zip = Files.createTempFile("filebundle-existing", ".zip")
      val bytes = _zip_bytes(Vector("index.html" -> "<article>zip</article>\n"))
      Files.write(zip, bytes)

      val result = FileBundle.mimeBody("bundle", zip)

      result shouldBe a[Consequence.Success[_]]
      result match {
        case Consequence.Success(body) =>
          body.contentType shouldBe ContentType.APPLICATION_ZIP
          _bag_bytes(body.value).toVector shouldBe bytes.toVector
        case _ => fail("expected success")
      }
    }

    "provide the generated-code compatible Filebundle type alias" in {
      val body = MimeBody(ContentType.APPLICATION_ZIP, Bag.binary(_zip_bytes(Vector("index.html" -> "body"))))

      val bundle: Filebundle = FileBundle(body)

      bundle shouldBe FileBundle(body)
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

      FileBundle.mimeBody("bundle", invalid) shouldBe a[Consequence.Failure[_]]
      FileBundle.mimeBody("bundle", empty) shouldBe a[Consequence.Failure[_]]
      FileBundle.mimeBody("bundle", nonZipBody) shouldBe a[Consequence.Failure[_]]
      FileBundle.mimeBody("bundle", invalidZipBody) shouldBe a[Consequence.Failure[_]]
      FileBundle.mimeBody("bundle", directoryOnlyZip) shouldBe a[Consequence.Failure[_]]
      FileBundle.mimeBody("bundle", escapeZip) shouldBe a[Consequence.Failure[_]]
      FileBundle.mimeBody("bundle", absoluteZip) shouldBe a[Consequence.Failure[_]]
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
