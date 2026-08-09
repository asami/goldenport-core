package org.goldenport.protocol.handler.ingress

import java.net.{ConnectException, NoRouteToHostException}
import java.net.http.HttpTimeoutException
import java.net.URI

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalacheck.Gen

import org.goldenport.Consequence
import org.goldenport.observation.{Cause, Descriptor}
import org.goldenport.protocol.{Argument, Property, Switch}
import org.goldenport.protocol.spec.{OperationDefinition, OperationDefinitionGroup, ParameterDefinition, RequestDefinition, ResponseDefinition, ServiceDefinition, ServiceDefinitionGroup}
import org.goldenport.observation.Taxonomy
import org.goldenport.value.BaseContent
import cats.data.NonEmptyVector

/*
 * Executable Spec for ArgsIngress
 *
 * Purpose:
 * - Fix the canonical behavior of ArgsIngress
 * - args:Array[String] -> Request
 * - Service/operation identification is syntactic and uses ServiceDefinitionGroup
 */
/*
 * Canonical Parsing Contract (Normative)
 *
 * ArgsIngress performs syntactic canonicalization only.
 * Semantic resolution is explicitly out of scope.
 *
 * Supported invocation patterns:
 *
 * 1. operation arg1 arg2 ...
 *    - No explicit service name
 *    - operation is resolved by scanning ServiceDefinitionGroup
 *
 * 2. service operation arg1 arg2 ...
 *    - Explicit service name
 *    - operation is resolved within the specified service
 *
 * Service identification rules:
 * - If args(0) matches a service name in ServiceDefinitionGroup,
 *   it is treated as service and args(1) as operation.
 * - Otherwise, args(0) is treated as operation name and searched
 *   across all services.
 *
 * Argument classification rules:
 * - Switch / Property / Argument classification is performed here.
 * - OperationDefinition may be consulted ONLY to classify parameter kinds.
 * - Positional arguments are syntactically named as param1, param2, ...
 *
 * ArgsIngress MUST NOT:
 * - Perform semantic validation
 * - Interpret business meaning of arguments
 * - Resolve overloads beyond syntactic disambiguation
 */
/*
 * @since   Jan.  1, 2026
 *  version Jan.  2, 2026
 *  version Mar. 24, 2026
 *  version Apr. 14, 2026
 * @version Aug. 10, 2026
 * @author  ASAMI, Tomoharu
 */
class ArgsIngressSpec
  extends AnyWordSpec
    with GivenWhenThen
    with Matchers
    with ScalaCheckDrivenPropertyChecks {

  private val _ingress = new DefaultArgsIngress()
  private val _services = {
    val operation =
      OperationDefinition(
        content = org.goldenport.value.BaseContent.simple("query"),
        request = RequestDefinition(parameters = Nil),
        response = ResponseDefinition.void
      )
    val service =
      ServiceDefinition(
        name = "test",
        operations = OperationDefinitionGroup(
          NonEmptyVector.of(operation)
        )
      )
    ServiceDefinitionGroup(Vector(service))
  }

  "ArgsIngress" should {

    "extract operation name from args" in {
      Given("CLI-like arguments with only an operation name")
      val args = Array("query")

      When("encoding args into Request")
      val result = _ingress.encode(_services, args)

      result match {
        case org.goldenport.Consequence.Success(req) =>
          Then("it succeeds and sets operation")
          req.operation shouldBe "query"
          req.arguments shouldBe Nil
          req.properties shouldBe Nil
          req.switches shouldBe Nil
        case org.goldenport.Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "extract service name and operation name from args when service is explicit" in {
      Given("CLI-like arguments with explicit service and operation")
      val args = Array("test", "query", "hello")

      When("encoding args into Request")
      val result = _ingress.encode(_services, args)

      result match {
        case org.goldenport.Consequence.Success(req) =>
          Then("it succeeds and sets service and operation")
          req.service shouldBe Some("test")
          req.operation shouldBe "query"
          req.arguments shouldBe List(
            Argument("param1", "hello", None)
          )
          req.switches shouldBe Nil
          req.properties shouldBe Nil
        case org.goldenport.Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "treat first arg as operation when it does not match a service" in {
      Given("CLI-like arguments without explicit service")
      val args = Array("query", "hello")

      When("encoding args into Request")
      val result = _ingress.encode(_services, args)

      result match {
        case org.goldenport.Consequence.Success(req) =>
          Then("it succeeds and treats first arg as operation")
          req.service shouldBe None
          req.operation shouldBe "query"
          req.arguments shouldBe List(
            Argument("param1", "hello", None)
          )
          req.switches shouldBe Nil
          req.properties shouldBe Nil
        case org.goldenport.Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "extract properties from --key value form" in {
      Given("CLI-like arguments with a property")
      val args = Array("query", "--text", "hello")

      When("encoding args into Request")
      val result = _ingress.encode(_services, args)

      result match {
        case org.goldenport.Consequence.Success(req) =>
          Then("it succeeds and produces a Property")
          req.operation shouldBe "query"
          req.arguments shouldBe Nil
          req.switches shouldBe Nil
          req.properties shouldBe List(
            Property("text", "hello", None)
          )
        case org.goldenport.Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "extract positional arguments as Argument values" in {
      Given("CLI-like arguments with positional parameters")
      val args = Array("query", "hello", "world")

      When("encoding args into Request")
      val result = _ingress.encode(_services, args)

      result match {
        case org.goldenport.Consequence.Success(req) =>
          Then("it succeeds and produces Argument values")
          req.operation shouldBe "query"
          req.arguments shouldBe List(
            Argument("param1", "hello", None),
            Argument("param2", "world", None)
          )
          req.switches shouldBe Nil
          req.properties shouldBe Nil
        case org.goldenport.Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "classify switches and properties using an OperationDefinition" in {
      Given("CLI-like arguments and an OperationDefinition with switch/property names")
      val args = Array("query", "--mode", "--verbose", "hello")
      val opdef =
        OperationDefinition(
          content = org.goldenport.value.BaseContent.simple("query"),
          request =
            RequestDefinition(
              parameters = List(
                ParameterDefinition(
                  content = BaseContent.simple("mode"),
                  kind = ParameterDefinition.Kind.Property
                ),
                ParameterDefinition(
                  content = BaseContent.simple("verbose"),
                  kind = ParameterDefinition.Kind.Switch
                )
              )
            ),
          response = ResponseDefinition.void
        )

      When("encoding args into Request with OperationDefinition")
      val result = _ingress.encode(opdef, args)

      result match {
        case org.goldenport.Consequence.Success(req) =>
          Then("it succeeds and uses definition names for switch/property classification")
          req.operation shouldBe "query"
          req.arguments shouldBe List(
            Argument("param1", "hello", None)
          )
          req.switches shouldBe List(
            Switch("verbose", true, None)
          )
          req.properties shouldBe List(
            Property("mode", "", None)
          )
        case org.goldenport.Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "treat --name value as Argument when ParameterDefinition kind is Argument" in {
      Given("CLI-like arguments with a named argument using --name value form")
      val args = Array("query", "--limit", "10", "hello")
      import org.goldenport.protocol.spec.{RequestDefinition, ResponseDefinition}

      val opdef =
        OperationDefinition(
          content = org.goldenport.value.BaseContent.simple("query"),
          request =
            RequestDefinition(
              parameters = List(
                ParameterDefinition(
                  content = BaseContent.simple("query"),
                  kind = ParameterDefinition.Kind.Argument
                ),
                ParameterDefinition(
                  content = BaseContent.simple("limit"),
                  kind = ParameterDefinition.Kind.Argument
                )
              )
            ),
          response = ResponseDefinition.void
        )

      When("encoding args into Request with OperationDefinition")
      val result = _ingress.encode(opdef, args)

      result match {
        case org.goldenport.Consequence.Success(req) =>
          Then("it succeeds and treats --limit as an Argument, not a Property")
          req.operation shouldBe "query"
          req.arguments shouldBe List(
            Argument("limit", "10", None),
            Argument("query", "hello", None)
          )
          req.switches shouldBe Nil
          req.properties shouldBe Nil
        case org.goldenport.Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "report a missing ArgsIngress as a structured missing input failure" in {
      Given("an empty ingress collection and CLI-like arguments")
      val args = Array("query", "--limit", "10")

      When("resolving an ArgsIngress")
      val result = IngressCollection.empty.ingress(args)

      Then("it fails as an argument-missing result with the original args")
      val conclusion = result match {
        case Consequence.Failure(conclusion) => conclusion
        case Consequence.Success(_) => fail("expected failure")
      }
      conclusion.observation.taxonomy shouldBe Taxonomy.argumentMissing
      conclusion.observation.cause.descriptor.facets should contain(
        Descriptor.Facet.Args(args.toIndexedSeq)
      )
    }

    "classify deterministic HTTP connection refusal before generic Throwable conversion" in {
      Given("an HTTP resolver action that throws connection refusal")
      val uri = URI.create("https://voicevox.example.test/audio")
      val exception = new ConnectException("Connection refused")
      val resolver = ArgsIngress.externalRefResolver(_ => throw exception)

      When("the external reference is resolved")
      val conclusion = _failure(resolver.resolve(uri))
      val facets = conclusion.observation.cause.descriptor.facets

      Then("the boundary produces a typed network-unavailable conclusion")
      conclusion.observation.taxonomy shouldBe Taxonomy.networkUnavailable
      conclusion.observation.cause.kind shouldBe Some(Cause.Kind.ConnectionRefused)
      facets should contain(Descriptor.Facet.Endpoint(uri.toString))
      facets should contain(Descriptor.Facet.Component("external-ref-resolver"))
      facets should contain(Descriptor.Facet.Exception(exception))
    }

    "classify deterministic HTTP unreachability and timeout before generic Throwable conversion" in {
      Given("independent HTTP resolver actions that report routing and timeout failures")
      val unreachableuri = URI.create("https://unreachable.example.test/")
      val timeouturi = URI.create("https://timeout.example.test/")
      val unreachable = new NoRouteToHostException("No route to host")
      val timeout = new HttpTimeoutException("request timed out")
      val unreachableresolver = ArgsIngress.externalRefResolver(_ => throw unreachable)
      val timeoutresolver = ArgsIngress.externalRefResolver(_ => throw timeout)

      When("both external references are resolved")
      val unreachableconclusion = _failure(unreachableresolver.resolve(unreachableuri))
      val timeoutconclusion = _failure(timeoutresolver.resolve(timeouturi))

      Then("the two availability mechanisms remain distinct")
      unreachableconclusion.observation.taxonomy shouldBe Taxonomy.networkUnavailable
      unreachableconclusion.observation.cause.kind shouldBe Some(Cause.Kind.Unreachable)
      timeoutconclusion.observation.taxonomy shouldBe Taxonomy.networkUnavailable
      timeoutconclusion.observation.cause.kind shouldBe Some(Cause.Kind.Timeout)
    }

    "retain the existing generic Throwable fallback for unrelated HTTP failures" in {
      Given("an HTTP resolver action that throws an unrelated exception")
      val uri = URI.create("https://unexpected.example.test/")
      val exception = new IllegalStateException("unexpected parser state")
      val resolver = ArgsIngress.externalRefResolver(_ => throw exception)

      When("the external reference is resolved")
      val conclusion = _failure(resolver.resolve(uri))
      val facets = conclusion.observation.cause.descriptor.facets

      Then("the exception follows the generic system fallback without availability-only facets")
      conclusion.observation.taxonomy shouldBe Taxonomy(
        Taxonomy.Category.System,
        Taxonomy.Symptom.Corrupted
      )
      conclusion.observation.cause.kind shouldBe None
      facets should contain(Descriptor.Facet.Exception(exception))
      facets should not contain (Descriptor.Facet.Endpoint(uri.toString))
      facets should not contain (Descriptor.Facet.Component("external-ref-resolver"))
    }

    "preserve HTTP availability semantics" which {
      "availability wrappers are generated" should {
        "map each supported mechanism before the generic Throwable boundary" in {
          Given("generated wrapper depths and deterministic availability throwables")
          val depthgen = Gen.choose(0, 8)
          val kindgen: Gen[(Cause.Kind, Throwable)] = Gen.oneOf[(Cause.Kind, Throwable)](
            Cause.Kind.ConnectionRefused -> new ConnectException("Connection refused"),
            Cause.Kind.Unreachable -> new NoRouteToHostException("No route to host"),
            Cause.Kind.Timeout -> new HttpTimeoutException("request timed out")
          )

          When("a generated wrapped throwable is raised by the HTTP resolver")
          forAll(depthgen, kindgen) { (depth, expected) =>
            val wrapped = (0 until depth).foldLeft(expected._2: Throwable) { (cause, index) =>
              new RuntimeException(s"wrapper-$index", cause)
            }
            val resolver = ArgsIngress.externalRefResolver(_ => throw wrapped)
            val conclusion = _failure(resolver.resolve(URI.create("https://availability.example.test/")))

            Then("the boundary retains the availability kind and transport facets")
            conclusion.observation.cause.kind shouldBe Some(expected._1)
            conclusion.observation.taxonomy shouldBe Taxonomy.networkUnavailable
          }
        }
      }

      "an interruption is nested in an availability wrapper" should {
        "restore and propagate the actual interruption" in {
          Given("a resolver action with a wrapped interruption")
          val interrupted = new InterruptedException("cancelled")
          val availability = new ConnectException("Connection refused")
          availability.initCause(interrupted)
          val wrapped = new RuntimeException("wrapper", availability)
          val resolver = ArgsIngress.externalRefResolver(_ => throw wrapped)

          When("the HTTP boundary resolves the reference")
          try {
            val thrown = intercept[InterruptedException] {
              resolver.resolve(URI.create("https://interrupt.example.test/"))
            }

            Then("the original interruption is propagated and the flag is restored")
            thrown shouldBe interrupted
            Thread.currentThread().isInterrupted shouldBe true
          } finally {
            Thread.interrupted()
          }
        }
      }
    }
  }

  private def _failure[A](result: Consequence[A]): org.goldenport.Conclusion =
    result match {
      case Consequence.Failure(conclusion) => conclusion
      case Consequence.Success(_) => fail("expected external reference failure")
    }
}
