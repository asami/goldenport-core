package org.goldenport.process

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, IOException, InputStream, OutputStream}
import java.net.ConnectException
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import org.goldenport.{Conclusion, Consequence}
import org.goldenport.conclusion.Interpretation
import org.goldenport.observation.{Cause, Descriptor, Taxonomy}

/*
 * @since   Aug. 10, 2026
 * @version Aug. 10, 2026
 * @author  ASAMI, Tomoharu
 */
class ShellCommandExecutorSpec
  extends AnyWordSpec
    with GivenWhenThen
    with Matchers
    with ScalaCheckDrivenPropertyChecks {

  "LocalShellCommandExecutor" should {
    "preserve its public no-argument JVM constructor" in {
      Given("the public executor class")

      When("its no-argument constructor is requested reflectively")
      val constructor = classOf[LocalShellCommandExecutor].getConstructor()
      val executor = constructor.newInstance()

      Then("Java callers can construct the production executor without an injection seam")
      executor shouldBe a[LocalShellCommandExecutor]
    }

    "drain both process streams before waiting for completion" in {
      Given("a process whose wait completes only after both streams begin reading")
      val process = new ObservedProcess("stdout", "stderr")
      val executor = ShellCommandExecutor.forProcessStarter(_ => process)

      When("the bounded command is executed")
      val result = _success(executor.execute(ShellCommand(Vector("tool"))))

      Then("both drain tasks have consumed output before waitFor and return terminal bags")
      process.waitObservedDrains shouldBe true
      result.stdout.asStringUnsafe() shouldBe "stdout"
      result.stderr.asStringUnsafe() shouldBe "stderr"
    }

    "consume large deterministic output without wait-before-drain backpressure" in {
      Given("a process with bounded large stdout and stderr payloads")
      val payload = "x" * (512 * 1024)
      val executor = ShellCommandExecutor.forProcessStarter(_ => _process(0, payload, payload))

      When("the command exits after concurrent drains start")
      val result = _success(executor.execute(ShellCommand(Vector("tool"))))

      Then("both output bags retain every byte without a deadlock ordering")
      result.stdout.asStringUnsafe().length shouldBe payload.length
      result.stderr.asStringUnsafe().length shouldBe payload.length
    }

    "clean up owned process and drain tasks when wait is interrupted" in {
      Given("a process that interrupts its wait lifecycle")
      val process = new InterruptingProcess
      val executor = ShellCommandExecutor.forProcessStarter(_ => process)

      When("the command wait is interrupted")
      try {
        val thrown = intercept[InterruptedException] {
          executor.execute(ShellCommand(Vector("tool")))
        }

        Then("the actual interruption is rethrown after destroying and closing owned resources")
        thrown.getMessage shouldBe "cancelled"
        process.destroyed shouldBe true
        process.forceDestroyed shouldBe true
        process.stdoutClosed shouldBe true
        process.stderrClosed shouldBe true
        Thread.currentThread().isInterrupted shouldBe true
      } finally {
        Thread.interrupted()
      }
    }

    "translate only explicit Docker daemon diagnostics into structured service failures" which {
      "the executable basename is exactly docker" should {
        "accept only the explicit daemon-unavailable phrases" in {
          Given("generated Docker executable path variants and daemon diagnostics")
          val dockerpathgen = Gen.oneOf("docker", "/usr/local/bin/docker", "C:\\bin\\docker")
          val diagnosticgen = Gen.oneOf(
            "Cannot connect to the Docker daemon at unix:///var/run/docker.sock.",
            "Is the docker daemon running?",
            "Docker daemon is not running"
          )

          When("each nonzero synthetic command is classified")
          forAll(dockerpathgen, diagnosticgen) { (dockerpath, diagnostic) =>
            val executor = ShellCommandExecutor.forProcessStarter(_ => _process(1, stderr = diagnostic))
            val conclusion = _failure(executor.execute(ShellCommand(Vector(dockerpath, "ps"))))

            Then("the result has the Docker-daemon service facet and default endpoint")
            conclusion.observation.taxonomy shouldBe Taxonomy.serviceUnavailable
            conclusion.observation.cause.kind shouldBe Some(Cause.Kind.NotRunning)
            conclusion.observation.cause.descriptor.facets should contain(Descriptor.Facet.Endpoint("docker://daemon"))
            conclusion.observation.cause.descriptor.facets should contain(Descriptor.Facet.Service("docker-daemon"))
          }
        }
      }

      "a nonblank DOCKER_HOST is supplied" should {
        "use that host as the endpoint facet" in {
          Given("a Docker daemon-unavailable process and explicit remote endpoint")
          val executor = ShellCommandExecutor.forProcessStarter(_ => _process(1, stderr = "Docker daemon is not running"))

          When("the Docker command is classified")
          val conclusion = _failure(executor.execute(ShellCommand(Vector("docker", "ps"), env = Map("DOCKER_HOST" -> "tcp://docker.example.test:2376"))))

          Then("the explicit endpoint is preserved without broadening the diagnostic predicate")
          conclusion.observation.cause.descriptor.facets should contain(Descriptor.Facet.Endpoint("tcp://docker.example.test:2376"))
        }
      }
    }

    "leave ordinary Docker and non-Docker failures as ordinary command results" in {
      Given("generated non-daemon diagnostics, lookalike executables, and remote transport text")
      val commandgen = Gen.oneOf("docker", "podman", "docker-compose")
      val diagnosticgen = Gen.oneOf(
        "error during connect",
        "remote TLS certificate validation failed",
        "remote DNS lookup failed",
        "authentication required",
        "ordinary command error"
      )

      When("each nonzero process exits")
      forAll(commandgen, diagnosticgen) { (executable, diagnostic) =>
        val executor = ShellCommandExecutor.forProcessStarter(_ => _process(7, stderr = diagnostic))
        val result = _success(executor.execute(ShellCommand(Vector(executable, "ps"), env = Map("DOCKER_HOST" -> "tcp://remote.example.test"))))

        Then("no false Docker-daemon service failure is created")
        result.exitCode shouldBe 7
        result.stderr.asStringUnsafe() shouldBe diagnostic
      }
    }

    "convert a known process-start availability throwable before the generic fallback" in {
      Given("a deterministic process starter that reports connection refusal")
      val exception = new ConnectException("Connection refused")
      val executor = ShellCommandExecutor.forProcessStarter(_ => throw exception)

      When("the shell boundary starts the command")
      val conclusion = _failure(executor.execute(ShellCommand(Vector("curl", "https://api.example.test/"))))
      val facets = conclusion.observation.cause.descriptor.facets

      Then("the availability failure keeps network semantics, endpoint, component, and exception")
      conclusion.observation.taxonomy shouldBe Taxonomy.networkUnavailable
      conclusion.observation.cause.kind shouldBe Some(Cause.Kind.ConnectionRefused)
      facets should contain(Descriptor.Facet.Endpoint("command://curl"))
      facets should contain(Descriptor.Facet.Component("shell-command-executor"))
      facets should contain(Descriptor.Facet.Exception(exception))
    }

    "retain generic Throwable conversion for a missing executable" in {
      Given("a starter that cannot locate the requested executable")
      val executor = ShellCommandExecutor.forProcessStarter(_ => throw new IOException("missing executable"))

      When("the shell boundary starts the command")
      val conclusion = _failure(executor.execute(ShellCommand(Vector("missing-tool"))))

      Then("the fallback remains generic rather than Docker or availability-specific")
      conclusion.observation.cause.kind shouldBe None
      conclusion.observation.taxonomy should not be Taxonomy.serviceUnavailable
    }
  }

  private def _process(
    exitcode: Int,
    stdout: String = "",
    stderr: String = ""
  ): Process =
    new Process {
      override def getOutputStream: OutputStream = new ByteArrayOutputStream()
      override def getInputStream: InputStream = new ByteArrayInputStream(stdout.getBytes(StandardCharsets.UTF_8))
      override def getErrorStream: InputStream = new ByteArrayInputStream(stderr.getBytes(StandardCharsets.UTF_8))
      override def waitFor(): Int = exitcode
      override def exitValue(): Int = exitcode
      override def destroy(): Unit = ()
    }

  private def _failure[A](result: Consequence[A]): Conclusion =
    result match {
      case Consequence.Failure(conclusion) => conclusion
      case Consequence.Success(_) => fail("expected shell failure")
    }

  private def _success[A](result: Consequence[A]): A =
    result match {
      case Consequence.Success(value) => value
      case Consequence.Failure(conclusion) => fail(conclusion.toString)
    }

  private final class ObservedProcess(stdout: String, stderr: String) extends Process {
    private val _stdout_read = new AtomicBoolean(false)
    private val _stderr_read = new AtomicBoolean(false)
    var waitObservedDrains = false

    override def getOutputStream: OutputStream = new ByteArrayOutputStream()
    override def getInputStream: InputStream = _observed_stream(stdout, () => _stdout_read.set(true))
    override def getErrorStream: InputStream = _observed_stream(stderr, () => _stderr_read.set(true))
    override def waitFor(): Int = {
      val deadline = System.nanoTime() + 1000000000L
      while ((!_stdout_read.get || !_stderr_read.get) && System.nanoTime() < deadline)
        Thread.`yield`()
      waitObservedDrains = _stdout_read.get && _stderr_read.get
      if (!waitObservedDrains)
        throw new IllegalStateException("drains did not start before waitFor")
      0
    }
    override def exitValue(): Int = 0
    override def destroy(): Unit = ()
  }

  private final class InterruptingProcess extends Process {
    private val _stdout = new CloseTrackingInputStream
    private val _stderr = new CloseTrackingInputStream
    var destroyed = false
    var forceDestroyed = false

    def stdoutClosed: Boolean = _stdout.closed
    def stderrClosed: Boolean = _stderr.closed
    override def getOutputStream: OutputStream = new ByteArrayOutputStream()
    override def getInputStream: InputStream = _stdout
    override def getErrorStream: InputStream = _stderr
    override def waitFor(): Int = throw new InterruptedException("cancelled")
    override def exitValue(): Int = 0
    override def destroy(): Unit = destroyed = true
    override def destroyForcibly(): Process = {
      forceDestroyed = true
      this
    }
    override def isAlive: Boolean = true
  }

  private def _observed_stream(value: String, observed: () => Unit): InputStream =
    new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)) {
      override def read(): Int = {
        observed()
        super.read()
      }

      override def read(buffer: Array[Byte], offset: Int, length: Int): Int = {
        observed()
        super.read(buffer, offset, length)
      }
    }

  private final class CloseTrackingInputStream extends ByteArrayInputStream(Array.emptyByteArray) {
    var closed = false
    override def close(): Unit = closed = true
  }
}
