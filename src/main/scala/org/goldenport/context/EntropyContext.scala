package org.goldenport.context

import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/*
 * @since   Jul. 15, 2026
 * @version Jul. 15, 2026
 * @author  ASAMI, Tomoharu
 */
trait EntropyContext {
  def bytes(purpose: String, size: Int): Array[Byte]

  def token(purpose: String, size: Int): String =
    Base64.getUrlEncoder.withoutPadding().encodeToString(bytes(purpose, size))
}

object EntropyContext {
  private trait EntropyStream {
    def bytes(size: Int): Array[Byte]
  }

  private final class SecureEntropyStream extends EntropyStream {
    private val _random = new SecureRandom()

    def bytes(size: Int): Array[Byte] = synchronized {
      val result = new Array[Byte](size)
      _random.nextBytes(result)
      result
    }
  }

  private final class RandomEntropyStream(random: RandomContext) extends EntropyStream {
    def bytes(size: Int): Array[Byte] = synchronized {
      Array.fill(size)(random.nextInt(256).toByte)
    }
  }

  private abstract class NamedEntropyContext extends EntropyContext {
    private val _streams = new ConcurrentHashMap[String, EntropyStream]()

    protected def create_stream(purpose: String): EntropyStream

    final def bytes(purpose: String, size: Int): Array[Byte] = {
      require(size >= 0, "size must not be negative")
      val normalized = RandomContext.normalizedPurpose(purpose)
      _streams.computeIfAbsent(normalized, create_stream).bytes(size)
    }
  }

  private final class SecureEntropyContext extends NamedEntropyContext {
    protected def create_stream(purpose: String): EntropyStream =
      new SecureEntropyStream()

    override def toString: String = "EntropyContext.Secure"
  }

  private final class DeterministicEntropyContext(random: RandomContext)
    extends NamedEntropyContext {
    protected def create_stream(purpose: String): EntropyStream =
      new RandomEntropyStream(random.stream(purpose))

    override def toString: String = "EntropyContext.Deterministic"
  }

  def secure(): EntropyContext = new SecureEntropyContext()

  def deterministic(seed: String): EntropyContext =
    new DeterministicEntropyContext(RandomContext.seeded(seed).stream("entropy"))
}
