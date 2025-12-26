package org.goldenport.bag

import cats.effect.IO
import org.goldenport.Consequence

import java.io.InputStream
import java.net.URL
import java.nio.file.{Files, Path}
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

// =======================================================
// Bag (Value Object)
// =======================================================

/**
 * Bag is an immutable value object representing materialized data.
 *
 * A Bag does NOT own lifecycle responsibility.
 * Lifecycle semantics are encoded by BagBackend.
 */
/*
 * @since   Jun.  8, 2014
 *  version Oct. 27, 2014
 *  version Nov. 12, 2014
 *  version Dec. 30, 2014
 *  version Sep. 29, 2015
 *  version Oct.  5, 2015
 *  version Feb. 29, 2016
 *  version Sep. 22, 2016
 *  version Jul. 24, 2017
 *  version Aug. 30, 2017
 *  version Oct.  5, 2018
 *  version Apr. 21, 2019
 *  version Jul. 18, 2025
 * @version Dec. 25, 2025
 * @author  ASAMI, Tomoharu
 */
/**
 * Bag is a container abstraction for binary data with explicit lifecycle
 * and interpretation semantics.
 *
 * The normative specification for Bag, TextBag, BinaryBag, and BagBuilder
 * semantics is defined in:
 *
 *   docs/spec/bag.md
 *
 * This Scaladoc intentionally contains only a brief summary.
 * Detailed behavioral contracts, error models, and promotion rules
 * are defined in the specification document.
 */
sealed trait Bag {
  def backend: BagBackend
  def metadata: BagMetadata

  /**
   * Opens a fresh InputStream for this Bag.
   *
   * Each invocation returns a new stream.
   *
   * Failure semantics:
   * This operation is treated as Quasi-Error I/O.
   *
   * If this method fails (e.g. the underlying resource cannot be opened),
   * it indicates a severely degraded runtime or system state
   * where meaningful recovery or branching is not expected.
   *
   * Therefore, this method is intentionally provided as
   * an exception-only API without a `Consequence` variant.
   */
  def openInputStream(): InputStream = {
    backend.openInputStream()
  }

  /**
   * Indicates whether this Bag may be subject to best-effort cleanup.
   */
  def cleanupPolicy: CleanupPolicy = {
    backend.cleanupPolicy
  }

  def promoteToBinary(): BinaryBag = {
    BinaryBag.Instance(this)
  }
}

object Bag {
  final case class Instance(
    backend: BagBackend,
    metadata: BagMetadata = BagMetadata()
  ) extends Bag

  def fromBytes(bytes: Array[Byte]): Bag = {
    Instance(
      backend = BagBackend.InMemory(bytes),
      metadata = BagMetadata(size = Some(bytes.length.toLong))
    )
  }

  def text(value: String, charset: Charset = StandardCharsets.UTF_8): TextBag = {
    TextBag.Instance(
      fromBytes(value.getBytes(charset)),
      charset
    )
  }

  def binary(bytes: Array[Byte]): BinaryBag = {
    BinaryBag.Instance(fromBytes(bytes))
  }
}

// =======================================================
// TextBag (Trait and Companion Object)
// =======================================================

/**
 * TextBag is a Bag whose text interpretation semantics are explicitly fixed.
 *
 * A TextBag always carries a Charset and guarantees stable, reproducible
 * text decoding without inference.
 *
 * The normative specification is defined in:
 *
 *   docs/spec/bag.md
 */
trait TextBag extends Bag {
  def bag: Bag
  def charset: Charset

  override def backend: BagBackend = bag.backend
  override def metadata: BagMetadata = bag.metadata

  def toText: String = {
    val in = bag.openInputStream()
    try {
      val bytes = in.readAllBytes()
      new String(bytes, charset)
    } finally {
      in.close()
    }
  }
}

object TextBag {
  final case class Instance(
    bag: Bag,
    charset: Charset
  ) extends TextBag
}

// =======================================================
// BinaryBag (Trait and Companion Object)
// =======================================================

/**
 * BinaryBag is a semantic marker indicating that a Bag has no text semantics.
 *
 * BinaryBag forbids text interpretation and exists solely to make this
 * constraint explicit at the type level.
 *
 * The normative specification is defined in:
 *
 *   docs/spec/bag.md
 */
trait BinaryBag extends Bag {
  def bag: Bag

  override def backend: BagBackend = bag.backend
  override def metadata: BagMetadata = bag.metadata
}

object BinaryBag {
  final case class Instance(
    bag: Bag
  ) extends BinaryBag
}

// =======================================================
// BagBackend (Closed Semantic Enum)
// =======================================================

/**
 * BagBackend represents the physical backing of a Bag.
 *
 * This enum is a closed semantic set encoding lifecycle
 * and deletion responsibility.
 */
enum BagBackend {

  /**
   * JVM memory–backed data.
   *
   * Lifecycle: JVM / GC
   * Deletion responsibility: none
   */
  case InMemory(bytes: Array[Byte])

  /**
   * OS-managed temporary file.
   *
   * Lifecycle: OS-managed
   * Deletion responsibility: best-effort only
   */
  case TempFile(path: Path)

  /**
   * Persistent local file.
   *
   * Lifecycle: operational / external
   * Deletion responsibility: none
   */
  case File(path: Path)

  /**
   * External resource (HTTP, S3, etc.).
   *
   * Lifecycle: external system
   * Deletion responsibility: none
   */
  case Url(url: URL)

  /**
   * Cleanup policy associated with this backend.
   */
  def cleanupPolicy: CleanupPolicy = {
    this match {
      case TempFile(_) => CleanupPolicy.BestEffort
      case _           => CleanupPolicy.None
    }
  }

  /**
   * Opens a fresh InputStream for this backend.
   */
  def openInputStream(): InputStream = {
    this match {
      case InMemory(bytes) =>
        new java.io.ByteArrayInputStream(bytes)

      case TempFile(path) =>
        Files.newInputStream(path)

      case File(path) =>
        Files.newInputStream(path)

      case Url(url) =>
        url.openStream()
    }
  }
}

// =======================================================
// CleanupPolicy (Semantic Enum)
// =======================================================

/**
 * CleanupPolicy describes how a BagBackend may be treated
 * regarding resource cleanup.
 */
enum CleanupPolicy {

  /**
   * No cleanup responsibility.
   */
  case None

  /**
   * Best-effort cleanup is allowed.
   */
  case BestEffort
}

// =======================================================
// BagMetadata (Value Object)
// =======================================================

/**
 * Metadata associated with a Bag.
 *
 * This object is purely descriptive and has no lifecycle semantics.
 */
final case class BagMetadata(
  name: Option[String] = None,
  mimeType: Option[String] = None,
  codec: Option[String] = None,
  size: Option[Long] = None
)

// =======================================================
// BagBuilder (Mutable during build, produces immutable Bag)
// =======================================================

/**
 * BagBuilder incrementally builds a Bag.
 *
 * - Starts with in-memory buffering
 * - Promotes to a temporary file when size exceeds threshold
 * - Produces an immutable Bag on completion
 *
 * This builder owns mutable state and MUST NOT escape its intended scope.
 *
 * The normative specification for BagBuilder construction semantics,
 * including build() and buildText(), is defined in:
 *
 *   docs/spec/bag.md
 */
final class BagBuilder(
  config: BagBuilder.Config = BagBuilder.Config()
) {

  private var size: Long = 0L
  private var inMemory: Option[java.io.ByteArrayOutputStream] =
    Some(new java.io.ByteArrayOutputStream())
  private var tempPath: Option[Path] = None
  private var out: java.io.OutputStream = inMemory.get

  /**
   * Writes bytes into this builder.
   */
  def write(bytes: Array[Byte]): Consequence[Unit] = {
    Consequence {
      _ensure_capacity(bytes.length)
      out.write(bytes)
      size += bytes.length.toLong
    }
  }

  /**
   * Writes data from an InputStream into this builder.
   *
   * The InputStream is NOT closed by this method.
   */
  def writeFrom(in: InputStream): Consequence[Unit] = {
    Consequence {
      val buffer = new Array[Byte](8192)
      var read = 0
      while ({ read = in.read(buffer); read != -1 }) {
        _ensure_capacity(read)
        out.write(buffer, 0, read)
        size += read.toLong
      }
    }
  }

  /**
   * Completes this builder and builds an immutable Bag.
   *
   * After calling this method, the builder MUST NOT be used.
   */
  def build(): Consequence[Bag] = {
    Consequence {
      tempPath match {
        case Some(path) =>
          out.flush()
          out.close()
          Bag.Instance(
            backend = BagBackend.TempFile(path),
            metadata = config.metadata.copy(size = Some(size))
          )

        case None =>
          val bytes = inMemory.get.toByteArray
          Bag.Instance(
            backend = BagBackend.InMemory(bytes),
            metadata = config.metadata.copy(size = Some(bytes.length.toLong))
          )
      }
    }
  }

  def buildText(): Consequence[TextBag] = {
    Consequence {
      build() match {
        case Consequence.Success(bag) =>
          bag match {
            case t: TextBag => t
            case _ =>
              config.charset match {
                case Some(cs) => TextBag.Instance(bag, cs)
                case None =>
                  throw new IllegalStateException(
                    "BagBuilder cannot produce TextBag without explicit charset"
                  )
              }
          }
        case Consequence.Failure(e) =>
          e.RAISE
      }
    }
  }

  private def _ensure_capacity(incoming: Int): Unit = {
    if (tempPath.isEmpty && (size + incoming.toLong) > config.thresholdBytes.toLong) {
      _promote_to_temp_file()
    }
  }

  private def _promote_to_temp_file(): Unit = {
    val path = Files.createTempFile("bag-", ".tmp")
    TempFileCleaner.register(path)

    val fileOut = Files.newOutputStream(path)
    inMemory.foreach { mem =>
      fileOut.write(mem.toByteArray)
    }

    inMemory = None
    tempPath = Some(path)
    out = fileOut
  }
}

object BagBuilder {

  final case class Config(
    thresholdBytes: Int = 1024 * 1024,
    metadata: BagMetadata = BagMetadata(),
    charset: Option[Charset] = None
  ) {
    require(thresholdBytes > 0, "thresholdBytes must be positive")
  }

}

// =======================================================
// Internal Helpers (Best-effort only)
// =======================================================

/**
 * Internal cleaner for temporary files.
 *
 * This is a best-effort safety net and MUST NOT be relied upon
 * for correctness.
 */
private[bag] object TempFileCleaner {

  private val cleaner = java.lang.ref.Cleaner.create()

  def register(path: Path): Unit = {
    cleaner.register(
      path,
      () => {
        try {
          Files.deleteIfExists(path)
        } catch {
          case _: Throwable => ()
        }
      }
    )
  }
}
