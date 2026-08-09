package org.goldenport

import java.net.{ConnectException, NoRouteToHostException, SocketException, SocketTimeoutException}
import java.net.http.HttpTimeoutException
import java.util.concurrent.TimeoutException

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalacheck.Gen

import org.goldenport.conclusion.{Disposition, Interpretation}
import org.goldenport.error.DetailCode
import org.goldenport.observation.{Cause, Descriptor, Taxonomy}

/*
 * @since   Aug. 10, 2026
 * @version Aug. 10, 2026
 * @author  ASAMI, Tomoharu
 */
class AvailabilityFailureSpec
  extends AnyWordSpec
    with GivenWhenThen
    with Matchers
    with ScalaCheckDrivenPropertyChecks {

  "availability failures" should {
    "represent Docker and required-service not-running conditions structurally" in {
      Given("Docker and VOICEVOX daemon failures with concrete endpoints")
      val dockerexception = new IllegalStateException("Docker daemon is not running")
      val voicevoxexception = new ConnectException("Connection refused")

      When("the service-unavailable conclusions are materialized")
      val docker = _failure(
        Consequence.serviceUnavailable[Nothing](
          "Docker daemon is unavailable",
          Cause.Kind.NotRunning,
          Seq(
            Descriptor.Facet.Endpoint("docker://daemon"),
            Descriptor.Facet.Service("docker-daemon"),
            Descriptor.Facet.Component("docker"),
            Descriptor.Facet.Exception(dockerexception)
          )
        )
      )
      val voicevox = _failure(
        Consequence.serviceUnavailable[Nothing](
          "VOICEVOX service is unavailable",
          Cause.Kind.NotRunning,
          Seq(
            Descriptor.Facet.Endpoint("http://127.0.0.1:50021"),
            Descriptor.Facet.Service("voicevox"),
            Descriptor.Facet.Component("voicevox-client"),
            Descriptor.Facet.Exception(voicevoxexception)
          )
        )
      )
      val dockerfacets = docker.observation.cause.descriptor.facets
      val voicevoxfacets = voicevox.observation.cause.descriptor.facets

      Then("both preserve the service taxonomy and not-running mechanism")
      docker.observation.taxonomy shouldBe Taxonomy.serviceUnavailable
      docker.observation.cause.kind shouldBe Some(Cause.Kind.NotRunning)
      docker.interpretation shouldBe Interpretation.systemFailure
      docker.disposition shouldBe Disposition.serviceUnavailable
      docker.status.webCode shouldBe Conclusion.WebCode.ServiceUnavailable
      dockerfacets should contain(Descriptor.Facet.Endpoint("docker://daemon"))
      dockerfacets should contain(Descriptor.Facet.Service("docker-daemon"))
      dockerfacets should contain(Descriptor.Facet.Component("docker"))
      dockerfacets should contain(Descriptor.Facet.Exception(dockerexception))
      voicevox.observation.taxonomy shouldBe Taxonomy.serviceUnavailable
      voicevox.observation.cause.kind shouldBe Some(Cause.Kind.NotRunning)
      voicevoxfacets should contain(Descriptor.Facet.Endpoint("http://127.0.0.1:50021"))
      voicevoxfacets should contain(Descriptor.Facet.Service("voicevox"))
      voicevoxfacets should contain(Descriptor.Facet.Component("voicevox-client"))
      voicevoxfacets should contain(Descriptor.Facet.Exception(voicevoxexception))
    }

    "classify connection refusal as a network failure with deterministic status detail" in {
      Given("a connection-refused throwable and an HTTP endpoint")
      val endpoint = "http://127.0.0.1:50021"
      val exception = new ConnectException("Connection refused")

      When("the availability boundary creates a network failure")
      val conclusion = _failure(
        Consequence.networkUnavailable[Nothing](
          "VOICEVOX connection was refused",
          endpoint,
          exception,
          Seq(Descriptor.Facet.Component("external-ref-resolver"))
        )
      )
      val facets = conclusion.observation.cause.descriptor.facets

      Then("the conclusion uses network judgment, structured facets, and fixed dimensions")
      conclusion.observation.taxonomy shouldBe Taxonomy.networkUnavailable
      conclusion.observation.cause.kind shouldBe Some(Cause.Kind.ConnectionRefused)
      conclusion.interpretation shouldBe Interpretation.networkFailure
      conclusion.disposition shouldBe Disposition.serviceUnavailable
      conclusion.status.webCode shouldBe Conclusion.WebCode.ServiceUnavailable
      facets should contain(Descriptor.Facet.Endpoint(endpoint))
      facets should contain(Descriptor.Facet.Component("external-ref-resolver"))
      facets should contain(Descriptor.Facet.Exception(exception))
      DetailCode.dimensions(conclusion) shouldBe DetailCode.Dimensions(
        category = 18,
        symptom = 11,
        cause = 14,
        interpretation = 6,
        userAction = 4,
        responsibility = 3
      )
      conclusion.status.detailCode.map(_.code) shouldBe Some(181114060403L)
    }

    "classify unreachable and timeout phenomena without external network state" in {
      Given("deterministic unreachable and timeout throwables")
      val unreachable = new NoRouteToHostException("No route to host")
      val timeout = new HttpTimeoutException("request timed out")

      When("both are converted at an availability boundary")
      val unreachableconclusion = _failure(
        Consequence.networkUnavailable[Nothing](
          "Remote endpoint is unreachable",
          "https://unreachable.example.test/",
          unreachable,
          Seq(Descriptor.Facet.Component("external-ref-resolver"))
        )
      )
      val timeoutconclusion = _failure(
        Consequence.networkUnavailable[Nothing](
          "Remote endpoint timed out",
          "https://timeout.example.test/",
          timeout,
          Seq(Descriptor.Facet.Component("external-ref-resolver"))
        )
      )

      Then("their mechanisms remain distinguishable in the closed Cause vocabulary")
      unreachableconclusion.observation.cause.kind shouldBe Some(Cause.Kind.Unreachable)
      timeoutconclusion.observation.cause.kind shouldBe Some(Cause.Kind.Timeout)
      unreachableconclusion.observation.taxonomy shouldBe Taxonomy.networkUnavailable
      timeoutconclusion.observation.taxonomy shouldBe Taxonomy.networkUnavailable
    }

    "serialize endpoint facts and stable service/network detail codes" in {
      Given("representative structured Docker and network conclusions")
      val docker = _failure(
        Consequence.serviceUnavailable[Nothing](
          "Docker daemon is unavailable",
          Cause.Kind.NotRunning,
          Seq(Descriptor.Facet.Endpoint("docker://daemon"))
        )
      )
      val network = _failure(
        Consequence.networkUnavailable[Nothing](
          "Connection refused",
          Cause.Kind.ConnectionRefused,
          Seq(Descriptor.Facet.Endpoint("https://api.example.test/"))
        )
      )

      When("their record and JSON projections are requested")
      val dockercauserecord = docker.toRecord
        .getRecord("observation")
        .flatMap(_.getRecord("cause"))
        .getOrElse(fail("expected Docker cause record"))
      val endpointrecord = Descriptor.Facet.Endpoint("docker://daemon").toRecord

      Then("the endpoint key, JSON representation, and numeric dimensions are exact")
      dockercauserecord.getString("kind") shouldBe Some("not-running")
      dockercauserecord.getString("endpoint") shouldBe Some("docker://daemon")
      endpointrecord.toJsonString shouldBe "{\"endpoint\":\"docker://daemon\"}"
      docker.toJsonString should include("\"endpoint\":\"docker://daemon\"")
      docker.status.detailCode.map(_.code) shouldBe Some(111113050403L)
      DetailCode.dimensions(docker) shouldBe DetailCode.Dimensions(11, 11, 13, 5, 4, 3)
      network.status.detailCode.map(_.code) shouldBe Some(181114060403L)
      DetailCode.dimensions(network) shouldBe DetailCode.Dimensions(18, 11, 14, 6, 4, 3)
    }

    "centralize wrapped availability-Throwable classification and leave unrelated failures alone" in {
      Given("wrapped refusal, socket reachability, timeout, and unrelated throwables")
      val wrappedrefusal = new RuntimeException("wrapper", new ConnectException("Connection refused"))
      val socketunreachable = new SocketException("Network is unreachable")
      val sockettimeout = new SocketTimeoutException("socket timed out")
      val timeout = new TimeoutException("deadline elapsed")
      val unrelated = new IllegalArgumentException("invalid input")
      val interrupted = new InterruptedException("cancelled")

      When("the closed classifier traverses ordinary cause wrappers")
      val classifications = Vector(
        Cause.availabilityKind(wrappedrefusal),
        Cause.availabilityKind(socketunreachable),
        Cause.availabilityKind(sockettimeout),
        Cause.availabilityKind(timeout),
        Cause.availabilityKind(unrelated),
        Cause.availabilityKind(interrupted)
      )

      Then("it identifies availability mechanisms without relabeling interruption or unrelated errors")
      classifications shouldBe Vector(
        Some(Cause.Kind.ConnectionRefused),
        Some(Cause.Kind.Unreachable),
        Some(Cause.Kind.Timeout),
        Some(Cause.Kind.Timeout),
        None,
        None
      )
    }

    "give nested interruption priority over every availability mechanism" which {
      "the wrapper depth is generated" should {
        "reject availability classification and preserve the actual interruption" in {
          Given("a generated wrapper chain around a connection-refused throwable and interruption")
          val depthgen = Gen.choose(0, 12)

          When("the full cause chain is classified")
          forAll(depthgen) { depth =>
            val interrupted = new InterruptedException("cancelled")
            val availability = new ConnectException("Connection refused")
            availability.initCause(interrupted)
            val wrapped = (0 until depth).foldLeft(availability: Throwable) { (cause, index) =>
              new RuntimeException(s"wrapper-$index", cause)
            }

            Then("interruption outranks availability and returns the original instance")
            Cause.availabilityKind(wrapped) shouldBe None
            Cause.interruption(wrapped) shouldBe Some(interrupted)
          }
        }
      }

      "the throwable cause graph cycles" should {
        "terminate without inventing an availability kind" in {
          Given("a self-referential throwable")
          val cycle = new RuntimeException("cycle") {
            override def getCause: Throwable = this
          }

          When("the full-chain helpers traverse it")
          val kind = Cause.availabilityKind(cycle)
          val interruption = Cause.interruption(cycle)

          Then("both traversals remain cycle-safe")
          kind shouldBe None
          interruption shouldBe None
        }
      }
    }
  }

  private def _failure[A](result: Consequence[A]): Conclusion =
    result match {
      case Consequence.Failure(conclusion) => conclusion
      case Consequence.Success(_) => fail("expected availability failure")
    }
}
