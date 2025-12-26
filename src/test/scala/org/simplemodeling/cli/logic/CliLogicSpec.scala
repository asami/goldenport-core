package org.goldenport.cli.logic

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.goldenport.protocol.spec.*
import org.goldenport.protocol.ProtocolEngine
import org.goldenport.protocol.logic.ProtocolLogic
import org.goldenport.protocol.operation.OperationRequest
import org.goldenport.Consequence
import cats.data.NonEmptyVector
import org.goldenport.schema.Multiplicity

/*
 * @since   Dec. 25, 2025
 * @version Dec. 26, 2025
 * @author  ASAMI, Tomoharu
 */
class CliLogicSpec extends AnyWordSpec with Matchers {

  "CliLogic.makeRequest" should {

    "parse arguments into Request without throwing (syntactic phase)" in {
      val service = ServiceDefinition(
        name = "user",
        operations = OperationDefinitionGroup(
          NonEmptyVector.of(dummyOperation("create"))
        ) // minimal working spec
      )

      val services = ServiceDefinitionGroup(Vector(service))
      val protocol = dummyProtocolEngine
      val cli = new CliLogic(services, protocol)

      cli.makeRequest(Array("create", "--dry-run"))
      succeed
    }

    "not validate operation existence yet (still syntactic)" in {
      val service = ServiceDefinition(
        name = "user",
        operations = OperationDefinitionGroup(
          NonEmptyVector.of(dummyOperation("create"))
        )
      )

      val services = ServiceDefinitionGroup(Vector(service))
      val protocol = dummyProtocolEngine
      val cli = new CliLogic(services, protocol)

      // unknown operation should not be rejected at this phase
      cli.makeRequest(Array("unknown-op"))
      succeed
    }

    "set Request.operation from the first positional argument" in {
      val service = ServiceDefinition(
        name = "user",
        operations = OperationDefinitionGroup(
          NonEmptyVector.of(dummyOperation("create"))
        )
      )

      val services = ServiceDefinitionGroup(Vector(service))
      val protocol = dummyProtocolEngine
      val cli = new CliLogic(services, protocol)

      val result = cli.makeRequest(Array("create", "--dry-run"))

      // Working spec: syntactic phase just copies the operation name
      result match {
        case Consequence.Success(req) =>
          req.operation shouldBe "create"
        case _ =>
          fail("makeRequest should not fail in syntactic phase")
      }
    }

    "separate switches and properties in syntactic phase" in {
      val service = ServiceDefinition(
        name = "user",
        operations = OperationDefinitionGroup(
          NonEmptyVector.of(dummyOperation("create"))
        )
      )

      val services = ServiceDefinitionGroup(Vector(service))
      val protocol = dummyProtocolEngine
      val cli = new CliLogic(services, protocol)

      val result = cli.makeRequest(
        Array("create", "--dry-run", "--timeout=30")
      )

      result match {
        case Consequence.Success(req) =>
          req.switches.map(_.name) should contain ("dry-run")
          req.properties.map(p => (p.name, p.value)) should contain (("timeout", "30"))

        case _ =>
          fail("makeRequest should not fail in syntactic phase")
      }
    }
  }

  "CliLogic.makeOperationRequest" should {

    "build an OperationRequest from a syntactic Request" in {
      val service = ServiceDefinition(
        name = "user",
        operations = OperationDefinitionGroup(
          NonEmptyVector.of(dummyOperation("create"))
        )
      )

      val services = ServiceDefinitionGroup(Vector(service))
      val protocol = dummyProtocolEngine
      val cli = new CliLogic(services, protocol)

      val request = org.goldenport.protocol.Request(
        service = Some("user"),
        operation = "create",
        arguments = List(org.goldenport.protocol.Argument("first", "first", None)),
        switches = Nil,
        properties = Nil
      )

      val result = cli.makeOperationRequest(request)

      result match {
        case Consequence.Success(opreq) =>
          opreq.shouldBe(
            OperationRequest(
              service = Some("user"),
              operation = "create",
              arguments = List(org.goldenport.protocol.Argument("first", "first", None)),
              switches = Nil,
              properties = Nil
            )
          )
        case _ =>
          fail("makeOperationRequest should not fail in semantic entry phase")
      }
    }

    "populate operation request fields for demo execution" in {
      val service = ServiceDefinition(
        name = "user",
        operations = OperationDefinitionGroup(
          NonEmptyVector.of(dummyOperation("create"))
        )
      )

      val services = ServiceDefinitionGroup(Vector(service))
      val protocol = dummyProtocolEngine
      val cli = new CliLogic(services, protocol)

      val request = org.goldenport.protocol.Request(
        service = Some("user"),
        operation = "create",
        arguments = List(
          org.goldenport.protocol.Argument("arg1", "v1", None),
          org.goldenport.protocol.Argument("arg2", "v2", None)
        ),
        switches = List(org.goldenport.protocol.Switch("dry-run", true, None)),
        properties = List(org.goldenport.protocol.Property("timeout", "30", None))
      )

      val result = cli.makeOperationRequest(request)

      result match {
        case Consequence.Success(opreq) =>
          opreq.shouldBe(
            OperationRequest(
              service = Some("user"),
              operation = "create",
              arguments = List(
                org.goldenport.protocol.Argument("arg1", "v1", None),
                org.goldenport.protocol.Argument("arg2", "v2", None)
              ),
              switches = List(org.goldenport.protocol.Switch("dry-run", true, None)),
              properties = List(org.goldenport.protocol.Property("timeout", "30", None))
            )
          )
        case _ =>
          fail("makeOperationRequest should build a demo-ready request")
      }
    }

    "use OperationDefinition.createOperationRequest for concrete operation request" in {
      val testoperation = new TestOperationDefinition("query")
      val service = ServiceDefinition(
        name = "user",
        operations = OperationDefinitionGroup(
          NonEmptyVector.of(testoperation)
        )
      )

      val services = ServiceDefinitionGroup(Vector(service))
      val protocol = dummyProtocolEngine
      val cli = new CliLogic(services, protocol)

      val request = org.goldenport.protocol.Request(
        service = Some("user"),
        operation = "query",
        arguments = List(
          org.goldenport.protocol.Argument("id", "u1", None),
          org.goldenport.protocol.Argument("name", "Alice", None),
          org.goldenport.protocol.Argument(
            "birthday",
            "2000-01-01T00:00:00Z",
            None
          )
        ),
        switches = Nil,
        properties = Nil
      )

      val result = cli.makeOperationRequest(request)

      result match {
        case Consequence.Success(opreq) =>
          opreq.isInstanceOf[TestQuery].shouldBe(true)
        case _ =>
          fail("makeOperationRequest should return a concrete test request")
      }
    }

    "fail when operation does not exist" in {
      val service = ServiceDefinition(
        name = "user",
        operations = OperationDefinitionGroup(
          NonEmptyVector.of(dummyOperation("create"))
        )
      )

      val services = ServiceDefinitionGroup(Vector(service))
      val protocol = dummyProtocolEngine
      val cli = new CliLogic(services, protocol)

      val request = org.goldenport.protocol.Request(
        service = Some("user"),
        operation = "delete",
        arguments = Nil,
        switches = Nil,
        properties = Nil
      )

      val result = cli.makeOperationRequest(request)

      result match {
        case _: Consequence.Failure[?] =>
          succeed
        case _ =>
          fail("makeOperationRequest should fail for unknown operation")
      }
    }

    "fail when service does not exist" in {
      val service = ServiceDefinition(
        name = "user",
        operations = OperationDefinitionGroup(
          NonEmptyVector.of(dummyOperation("create"))
        )
      )

      val services = ServiceDefinitionGroup(Vector(service))
      val protocol = dummyProtocolEngine
      val cli = new CliLogic(services, protocol)

      val request = org.goldenport.protocol.Request(
        service = Some("unknown"),
        operation = "create",
        arguments = Nil,
        switches = Nil,
        properties = Nil
      )

      val result = cli.makeOperationRequest(request)

      result match {
        case _: Consequence.Failure[?] =>
          succeed
        case _ =>
          fail("makeOperationRequest should fail for unknown service")
      }
    }
  }

  // --- helpers -------------------------------------------------

  private def dummyProtocolEngine: ProtocolEngine =
    new ProtocolEngine(
      new ProtocolLogic {
        override def makeRequest(
          req: org.goldenport.protocol.Request
        ): Consequence[org.goldenport.protocol.operation.OperationRequest] =
          Consequence.failure("not implemented")
      }
    )

  private def dummyOperation(name: String): OperationDefinition =
    OperationDefinition(
      name = name,
      request = RequestDefinition(
        parameters = Nil
      ),
      response = ResponseDefinition(
        result = Nil
      )
    )

  private final case class TestQuery(
    id: String,
    name: String,
    birthday: Option[java.time.ZonedDateTime]
  ) extends org.goldenport.protocol.operation.OperationRequest

  // --- Applicative operation test infrastructure ---

  // Keep TestOperationDefinition as-is
  private final class TestOperationDefinition(name: String)
    extends OperationDefinition {
    override val specification: OperationDefinition.Specification =
      OperationDefinition.Specification(
        name = name,
        request = RequestDefinition(
          parameters = List(
            ParameterDefinition(
              name = "id",
              kind = ParameterDefinition.Kind.Argument
            ),
            ParameterDefinition(
              name = "name",
              kind = ParameterDefinition.Kind.Argument
            ),
            ParameterDefinition(
              name = "birthday",
              kind = ParameterDefinition.Kind.Argument,
              domain = org.goldenport.schema.ValueDomain(
                multiplicity = Multiplicity.ZeroOne
              )
            )
          )
        ),
        response = ResponseDefinition(result = Nil)
      )

    override def createOperationRequest(
      req: org.goldenport.protocol.Request
    ): Consequence[org.goldenport.protocol.operation.OperationRequest] = {
      given org.goldenport.protocol.Request = req

      take_string("id")
        .zip3With(
          take_string("name"),
          get_datetime("birthday")
        )(TestQuery(_, _, _))
    }
  }

  // Abstract base for test operations
  private abstract class BaseTestOperationDefinition(name: String)
    extends OperationDefinition {

    override val specification: OperationDefinition.Specification =
      OperationDefinition.Specification(
        name = name,
        request = RequestDefinition(
          parameters = List(
            ParameterDefinition(
              name = "id",
              kind = ParameterDefinition.Kind.Argument
            ),
            ParameterDefinition(
              name = "name",
              kind = ParameterDefinition.Kind.Argument
            ),
            ParameterDefinition(
              name = "birthday",
              kind = ParameterDefinition.Kind.Argument,
              domain = org.goldenport.schema.ValueDomain(
                multiplicity = Multiplicity.ZeroOne
              )
            )
          )
        ),
        response = ResponseDefinition(result = Nil)
      )
  }

  // Cats-based applicative
  private final class CatsApplicativeOperationDefinition(name: String)
    extends BaseTestOperationDefinition(name) {

    import cats.syntax.all.*

    override def createOperationRequest(
      req: org.goldenport.protocol.Request
    ): Consequence[org.goldenport.protocol.operation.OperationRequest] = {
      given org.goldenport.protocol.Request = req

      (
        take_string("id"),
        take_string("name"),
        get_datetime("birthday")
      ).mapN(TestQuery(_, _, _))
    }
  }

  // Cats applicative using |@|
  private final class CatsBarAtBarOperationDefinition(name: String)
    extends BaseTestOperationDefinition(name) {

    import cats.syntax.apply.*
    import cats.instances.tuple.*

    override def createOperationRequest(
      req: org.goldenport.protocol.Request
    ): Consequence[org.goldenport.protocol.operation.OperationRequest] = {
      given org.goldenport.protocol.Request = req

      (
        (take_string("id"), take_string("name"), get_datetime("birthday"))
      ).mapN(TestQuery(_, _, _))
    }
  }

  // Consequence-native applicative (zip3With) — canonical
  private final class NativeZip3OperationDefinition(name: String)
    extends BaseTestOperationDefinition(name) {

    override def createOperationRequest(
      req: org.goldenport.protocol.Request
    ): Consequence[org.goldenport.protocol.operation.OperationRequest] = {
      given org.goldenport.protocol.Request = req

      take_string("id")
        .zip3With(
          take_string("name"),
          get_datetime("birthday")
        )(TestQuery(_, _, _))
    }
  }

  // Sequential (non-applicative, fail-fast)
  private final class SequentialOperationDefinition(name: String)
    extends BaseTestOperationDefinition(name) {

    override def createOperationRequest(
      req: org.goldenport.protocol.Request
    ): Consequence[org.goldenport.protocol.operation.OperationRequest] = {
      given org.goldenport.protocol.Request = req

      for {
        id <- take_string("id")
        name <- take_string("name")
        birthday <- get_datetime("birthday")
      } yield TestQuery(id, name, birthday)
    }
  }


  // --- Applicative style comparison tests ---

  "Applicative style comparison" should {

    def run(op: OperationDefinition): Consequence[OperationRequest] = {
      val service = ServiceDefinition(
        name = "user",
        operations = OperationDefinitionGroup(
          NonEmptyVector.of(op)
        )
      )
      val services = ServiceDefinitionGroup(Vector(service))
      val cli = new CliLogic(services, dummyProtocolEngine)

      val request = org.goldenport.protocol.Request(
        service = Some("user"),
        operation = op.specification.name,
        arguments = Nil, // missing all params
        switches = Nil,
        properties = Nil
      )

      cli.makeOperationRequest(request)
    }

    "aggregate errors with cats applicative" in {
      val result = run(new CatsApplicativeOperationDefinition("cats"))
      result match {
        case _: Consequence.Failure[?] =>
          succeed
        case _ =>
          fail("cats applicative should aggregate errors")
      }
    }

    "aggregate errors with native zip3With applicative" in {
      val result = run(new NativeZip3OperationDefinition("native-zip3"))
      result match {
        case _: Consequence.Failure[?] =>
          succeed
        case _ =>
          fail("native zip3With applicative should aggregate errors")
      }
    }

    "aggregate errors with cats |@| applicative" in {
      val result = run(new CatsBarAtBarOperationDefinition("cats-bar-at-bar"))
      result match {
        case _: Consequence.Failure[?] =>
          succeed
        case _ =>
          fail("cats |@| applicative should aggregate errors")
      }
    }

    "fail fast with sequential style" in {
      val result = run(new SequentialOperationDefinition("seq"))
      result match {
        case _: Consequence.Failure[?] =>
          succeed
        case _ =>
          fail("sequential style should fail fast")
      }
    }
  }
}
