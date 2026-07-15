package org.goldenport.context

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.{MessageDigest, SecureRandom}
import java.util.{Locale, Random}
import java.util.concurrent.ConcurrentHashMap

/*
 * @since   Dec. 30, 2025
 * @version Jul. 15, 2026
 * @author  ASAMI, Tomoharu
 */
trait RandomContext {
  def stream(purpose: String): RandomContext = {
    RandomContext.normalizedPurpose(purpose)
    this
  }
  def nextInt(): Int
  def nextInt(bound: Int): Int
  def nextLong(): Long
  def nextDouble(): Double
  def nextBoolean(): Boolean
}

object RandomContext {
  private case object FixedRandomContext extends RandomContext {
    def nextInt(): Int = 0
    def nextInt(bound: Int): Int = {
      require(bound > 0, "bound must be positive")
      0
    }
    def nextLong(): Long = 0L
    def nextDouble(): Double = 0.0d
    def nextBoolean(): Boolean = false

    override def toString: String = "RandomContext.Fixed"
  }

  private final class SystemRandomContext(
    streams: ConcurrentHashMap[String, RandomContext],
    purposepath: String,
    random: SecureRandom
  ) extends RandomContext {
    override def stream(purpose: String): RandomContext = {
      val normalized = normalizedPurpose(purpose)
      val path = if (purposepath.isEmpty) normalized else s"${purposepath}.${normalized}"
      streams.computeIfAbsent(path, _ => new SystemRandomContext(streams, path, new SecureRandom()))
    }

    def nextInt(): Int = random.nextInt()
    def nextInt(bound: Int): Int = random.nextInt(bound)
    def nextLong(): Long = random.nextLong()
    def nextDouble(): Double = random.nextDouble()
    def nextBoolean(): Boolean = random.nextBoolean()

    override def toString: String = "RandomContext.System"
  }

  private final class SeededRandomContext(
    seedmaterial: Array[Byte],
    purposepath: String,
    streams: ConcurrentHashMap[String, RandomContext]
  ) extends RandomContext {
    private val _random = new Random(_derive_seed(seedmaterial, purposepath))

    override def stream(purpose: String): RandomContext = {
      val normalized = normalizedPurpose(purpose)
      val path = if (purposepath.isEmpty) normalized else s"${purposepath}.${normalized}"
      streams.computeIfAbsent(path, _ => new SeededRandomContext(seedmaterial, path, streams))
    }

    def nextInt(): Int = synchronized(_random.nextInt())
    def nextInt(bound: Int): Int = synchronized(_random.nextInt(bound))
    def nextLong(): Long = synchronized(_random.nextLong())
    def nextDouble(): Double = synchronized(_random.nextDouble())
    def nextBoolean(): Boolean = synchronized(_random.nextBoolean())

    override def toString: String = "RandomContext.Seeded"
  }

  def fixed: RandomContext = FixedRandomContext

  def system(): RandomContext =
    new SystemRandomContext(new ConcurrentHashMap[String, RandomContext](), "", new SecureRandom())

  def seeded(seed: String): RandomContext = {
    require(Option(seed).exists(_.nonEmpty), "seed must not be empty")
    new SeededRandomContext(
      seed.getBytes(StandardCharsets.UTF_8),
      "",
      new ConcurrentHashMap[String, RandomContext]()
    )
  }

  def from(name: String): RandomContext =
    Option(name).map(_.trim.toLowerCase(Locale.ROOT)).getOrElse("") match {
      case "fixed" => FixedRandomContext
      case "deterministic" => FixedRandomContext
      case "system" => system()
      case "random" => system()
      case _ => FixedRandomContext
    }

  private[context] def normalizedPurpose(purpose: String): String = {
    val source = Option(purpose).map(_.trim.toLowerCase(Locale.ROOT)).getOrElse("")
    require(source.nonEmpty, "purpose must not be empty")
    source.map {
      case c if c >= 'a' && c <= 'z' => c
      case c if c >= '0' && c <= '9' => c
      case c @ ('.' | '-' | '_') => c
      case _ => '_'
    }
  }

  private def _derive_seed(seedmaterial: Array[Byte], purposepath: String): Long = {
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(seedmaterial)
    digest.update(0.toByte)
    digest.update(purposepath.getBytes(StandardCharsets.UTF_8))
    ByteBuffer.wrap(digest.digest()).getLong()
  }
}
