package org.goldenport.protocol

import org.goldenport.Consequence
import io.circe.Json
import org.goldenport.record.Record
// import org.goldenport.observation.Cause
import org.goldenport.observation.Taxonomy
import org.goldenport.protocol.Request
import org.goldenport.protocol.operation.OperationRequest
import org.goldenport.protocol.spec.{OperationDefinition, ParameterDefinition, RequestDefinition, ResponseDefinition}
import org.goldenport.protocol.spec.OperationDefinition.{ResolvedEmpty, ResolvedMultiple, ResolvedSingle, ResolvedValue}
import org.goldenport.schema.{Constraint, DataType, Multiplicity, ValueDomain, XJson, XSemiStructuredData, XNonNegativeInteger, XPositiveInteger, XString}
import org.goldenport.test.matchers.ConsequenceMatchers
import org.scalacheck.Gen
import org.scalacheck.Shrink
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
/*
 * @since   Dec. 29, 2025
 *  version Jan. 29, 2026
 *  version Apr. 11, 2026
 *  version Apr. 14, 2026
 *  version May. 11, 2026
 * @version Oct. 19, 2026
 * @author  ASAMI, Tomoharu
 */
class OperationDefinitionResolveParameterSpec
  extends AnyWordSpec
  with Matchers
  with GivenWhenThen
  with ScalaCheckDrivenPropertyChecks
  with ConsequenceMatchers {

  implicit val noShrink: Shrink[Long] = Shrink.shrinkAny

  private final case class IntRequest(request: Request, value: BigInt) extends OperationRequest
  private final case class StringRequest(request: Request, value: String) extends OperationRequest

  private def assert_taxonomy(result: Consequence[?], expected: Taxonomy): Unit =
    result match {
      case Consequence.Failure(conclusion) =>
        conclusion.observation.taxonomy.shouldBe(expected)
      case Consequence.Success(r) =>
        fail(s"expected Failure but Success: $r")
    }

  private def assert_datatype(result: Consequence[?]): Unit =
    result match {
      case Consequence.Failure(conclusion) =>
        conclusion.observation.taxonomy.shouldBe(Taxonomy.argumentInvalid)
        conclusion.observation.cause.getDataType.nonEmpty.shouldBe(true)
      case Consequence.Success(r) =>
        fail(s"expected Failure but Success: $r")
    }

  private def assert_constraint(result: Consequence[?]): Unit =
    result match {
      case Consequence.Failure(conclusion) =>
        conclusion.observation.taxonomy.shouldBe(Taxonomy.argumentInvalid)
        conclusion.observation.cause.getConstraints.nonEmpty.shouldBe(true)
      case Consequence.Success(r) =>
        fail(s"expected Failure but Success: $r")
    }

  private final class IntOperationDefinition(
    datatype: DataType,
    multiplicity: Multiplicity = Multiplicity.One,
    constraints: Vector[Constraint] = Vector.empty
  ) extends OperationDefinition {
    override val specification: OperationDefinition.Specification =
      OperationDefinition.Specification(
        name = "int-operation",
        request = RequestDefinition(
          parameters = List(
            ParameterDefinition(
              content = org.goldenport.value.BaseContent.simple("value"),
              kind = ParameterDefinition.Kind.Argument,
              domain = ValueDomain(
                datatype = datatype,
                multiplicity = multiplicity,
                constraints = constraints
              )
            )
          )
        ),
        response = ResponseDefinition.void
      )

    override def createOperationRequest(
      req: Request
    ): Consequence[OperationRequest] = {
      given Request = req
      val param = specification.request.parameters.head
      resolveParameter(param).flatMap {
        case ResolvedSingle(v: BigInt, _) =>
          Consequence.success(IntRequest(req, v))
        case ResolvedSingle(_, _) =>
          Consequence.argumentInvalid("unexpected parameter type")
        case ResolvedEmpty(_) =>
          Consequence.argumentMissing(param.name)
        case ResolvedMultiple(_, _) =>
          Consequence.argumentInvalid("multiple values not allowed")
      }
    }
  }

  private final class StringOperationDefinition(
    datatype: DataType = XString,
    multiplicity: Multiplicity = Multiplicity.One,
    constraints: Vector[Constraint] = Vector.empty
  ) extends OperationDefinition {
    override val specification: OperationDefinition.Specification =
      OperationDefinition.Specification(
        name = "string-operation",
        request = RequestDefinition(
          parameters = List(
            ParameterDefinition(
              content = org.goldenport.value.BaseContent.simple("value"),
              kind = ParameterDefinition.Kind.Argument,
              domain = ValueDomain(
                datatype = datatype,
                multiplicity = multiplicity,
                constraints = constraints
              )
            )
          )
        ),
        response = ResponseDefinition.void
      )

    override def createOperationRequest(
      req: Request
    ): Consequence[OperationRequest] = {
      given Request = req
      val param = specification.request.parameters.head
      resolveParameter(param).flatMap {
        case ResolvedSingle(v: String, _) =>
          Consequence.success(StringRequest(req, v))
        case ResolvedSingle(_, _) =>
          Consequence.argumentInvalid("unexpected parameter type")
        case ResolvedEmpty(_) =>
          Consequence.argumentMissing(param.name)
        case ResolvedMultiple(_, _) =>
          Consequence.argumentInvalid("multiple values not allowed")
      }
    }
  }

  private def requestWith(value: Any, extra: List[Any] = Nil): Request =
    Request(
      component = None,
      service = None,
      operation = "int-operation",
      arguments = Argument("value", value, None) :: extra.map(v => Argument("value", v, None)),
      switches = Nil,
      properties = Nil
    )

  private final class StructuredOperationDefinition(datatype: DataType, multiplicity: Multiplicity)
      extends OperationDefinition {
    override val specification: OperationDefinition.Specification =
      OperationDefinition.Specification(
        name = "structured-operation",
        request = RequestDefinition(parameters = List(ParameterDefinition(
          content = org.goldenport.value.BaseContent.simple("value"),
          kind = ParameterDefinition.Kind.Property,
          domain = ValueDomain(datatype = datatype, multiplicity = multiplicity)))),
        response = ResponseDefinition.void)

    def resolve(request: Request): Consequence[ResolvedValue] = {
      given Request = request
      resolveParameter(specification.request.parameters.head)
    }

    override def createOperationRequest(req: Request): Consequence[OperationRequest] =
      resolve(req).map(_ => OperationRequest(req))
  }

  private def _structured_request(values: List[Any]): Request =
    Request.of("fixture", "admission", "structured-operation").copy(
      properties = values.map(value => Property("value", value, None)))

  "Canonical structured property carriers" should {
    "interpret exact Vector carriers" which {
      "resolve omitted and empty zero-or-more Json carriers as absence" in {
        Given("a zero-or-more canonical Json property and omitted or exact empty Vector carriers")
        val op = new StructuredOperationDefinition(XJson, Multiplicity.ZeroMore)
        val domain = op.specification.request.parameters.head.domain
        val requests = List(_structured_request(Nil), _structured_request(List(Vector.empty[Json])))
        When("the existing resolver interprets both representations")
        val results = requests.map(op.resolve)
        Then("both resolve to the same typed empty value")
        results.map(_.toOption) shouldBe List.fill(2)(Some(ResolvedEmpty(domain)))
      }

      "reject missing and empty nonempty structured collections" in {
        Given("required nonempty Json and Record properties with omitted and empty Vector carriers")
        val operations = Vector(XJson, XSemiStructuredData).map(
          datatype => new StructuredOperationDefinition(datatype, Multiplicity.OneMore))
        val requests = List(_structured_request(Nil), _structured_request(List(Vector.empty[Any])))
        When("the resolver interprets the absent values")
        val results = operations.flatMap(op => requests.map(op.resolve))
        Then("the original required-parameter check rejects every absence")
        results.foreach(_.isSuccess shouldBe false)
      }

      "preserve one and ordered multiple Json values exactly like repeated properties" in {
        Given("Json vectors containing scalar documents, an array document and Json.Null")
        val op = new StructuredOperationDefinition(XJson, Multiplicity.ZeroMore)
        val domain = op.specification.request.parameters.head.domain
        val one = Vector(Json.Null)
        val many = Vector(Json.fromString("first"), Json.arr(Json.fromInt(2)), Json.Null)
        When("typed carriers and the existing repeated-property representation are resolved")
        val single = op.resolve(_structured_request(List(one)))
        val multiple = op.resolve(_structured_request(List(many)))
        val repeatedsingle = op.resolve(_structured_request(one.toList))
        val repeatedmultiple = op.resolve(_structured_request(many.toList))
        Then("one element remains ResolvedSingle and multiple elements retain their exact document order")
        single.toOption shouldBe Some(ResolvedSingle(Json.Null, domain))
        multiple.toOption shouldBe Some(ResolvedMultiple(many, domain))
        repeatedsingle.toOption shouldBe single.toOption
        repeatedmultiple.toOption shouldBe multiple.toOption
      }

      "retain a scalar Json array as one document and distinguish optional null from absence" in {
        Given("single and optional Json properties, one array document, Json.Null and a missing property")
        val single = new StructuredOperationDefinition(XJson, Multiplicity.One)
        val optional = new StructuredOperationDefinition(XJson, Multiplicity.ZeroOne)
        val array = Json.arr(Json.fromInt(1), Json.Null)
        val singledomain = single.specification.request.parameters.head.domain
        val optionaldomain = optional.specification.request.parameters.head.domain
        When("the resolver interprets the original scalar and optional paths")
        val arrayresult = single.resolve(_structured_request(List(array)))
        val nullresult = optional.resolve(_structured_request(List(Json.Null)))
        val missing = optional.resolve(_structured_request(Nil))
        Then("the array is not flattened and a present Json.Null is not missing")
        arrayresult.toOption shouldBe Some(ResolvedSingle(array, singledomain))
        nullresult.toOption shouldBe Some(ResolvedSingle(Json.Null, optionaldomain))
        missing.toOption shouldBe Some(ResolvedEmpty(optionaldomain))
      }

      "normalize Record and object Json carriers to exact nested Record documents" in {
        Given("one raw Record and an equivalent object Json with an omitted null member")
        val op = new StructuredOperationDefinition(XSemiStructuredData, Multiplicity.OneMore)
        val domain = op.specification.request.parameters.head.domain
        val raw = Record.createFull(Vector("nested" -> Record.createFull(Vector("count" -> 2))))
        val json = Json.obj("nested" -> Json.obj("count" -> Json.fromInt(2)), "omitted" -> Json.Null)
        val expected = Record.createFull(Vector("nested" -> Record.createFull(Vector("count" -> BigDecimal(2)))))
        When("one and mixed multiple carriers and repeated properties pass through canonical normalization")
        val single = op.resolve(_structured_request(List(Vector(raw))))
        val multiple = op.resolve(_structured_request(List(Vector(raw, json))))
        val repeatedsingle = op.resolve(_structured_request(List(raw)))
        val repeated = op.resolve(_structured_request(List(raw, json)))
        Then("only the normalized document contents occur in each ordered resolved element")
        single.toOption shouldBe Some(ResolvedSingle(expected, domain))
        repeatedsingle.toOption shouldBe single.toOption
        multiple.toOption shouldBe Some(ResolvedMultiple(Vector(expected, expected), domain))
        repeated.toOption shouldBe multiple.toOption
      }

      "resolve empty Record collections and optional absence while rejecting present optional Json.Null" in {
        Given("zero-or-more and optional canonical Record properties with missing empty and null inputs")
        val multiple = new StructuredOperationDefinition(XSemiStructuredData, Multiplicity.ZeroMore)
        val optional = new StructuredOperationDefinition(XSemiStructuredData, Multiplicity.ZeroOne)
        val multipledomain = multiple.specification.request.parameters.head.domain
        val optionaldomain = optional.specification.request.parameters.head.domain
        When("the resolver interprets the actual property carriers")
        val missing = multiple.resolve(_structured_request(Nil))
        val empty = multiple.resolve(_structured_request(List(Vector.empty[Record])))
        val optionalmissing = optional.resolve(_structured_request(Nil))
        val optionalnull = optional.resolve(_structured_request(List(Json.Null)))
        Then("missing or empty collections are absent while present Json.Null fails the Record normalizer")
        missing.toOption shouldBe Some(ResolvedEmpty(multipledomain))
        empty.toOption shouldBe Some(ResolvedEmpty(multipledomain))
        optionalmissing.toOption shouldBe Some(ResolvedEmpty(optionaldomain))
        optionalnull.isSuccess shouldBe false
      }

      "preserve arbitrary Json order without discarded samples" in {
        forAll(Gen.listOfN(4, Gen.choose(-10000, 10000))) { numbers =>
          Given("independently sampled Json scalar documents followed by Json.Null")
          val values = numbers.map(Json.fromInt).toVector :+ Json.Null
          val op = new StructuredOperationDefinition(XJson, Multiplicity.OneMore)
          val domain = op.specification.request.parameters.head.domain
          When("the actual property Vector is resolved")
          val result = op.resolve(_structured_request(List(values)))
          Then("every sampled element is retained in order without scalar promotion or null loss")
          result.toOption shouldBe Some(ResolvedMultiple(values, domain))
        }
      }
    }

    "retain strict normalization boundaries" which {
      "reject Vector carriers for scalar and optional structured properties" in {
        Given("single and optional canonical Json and Record parameters with Vector-valued properties")
        val operations = for {
          datatype <- Vector(XJson, XSemiStructuredData)
          multiplicity <- Vector(Multiplicity.One, Multiplicity.ZeroOne)
        } yield new StructuredOperationDefinition(datatype, multiplicity)
        val request = _structured_request(List(Vector(Json.obj())))
        When("the unchanged scalar path normalizes the whole Vector")
        val results = operations.map(_.resolve(request))
        Then("no single or optional parameter accepts a collection carrier")
        results.foreach(_.isSuccess shouldBe false)
      }

      "reject malformed and nested elements without flattening repeated property lists" in {
        Given("canonical repeated structured parameters with invalid elements or nested Vector carriers")
        val operations = Vector(XJson, XSemiStructuredData).map(
          datatype => new StructuredOperationDefinition(datatype, Multiplicity.ZeroMore))
        val requests = Vector(
          _structured_request(List(Vector[Any](Json.obj(), "{}"))),
          _structured_request(List(Vector[Any](Json.obj(), null))),
          _structured_request(List(Vector(Vector(Json.obj())))),
          _structured_request(List(Vector(Json.obj()), Json.obj())))
        When("each supplied element reaches its canonical scalar normalizer")
        val results = operations.flatMap(op => requests.map(op.resolve))
        Then("wrong runtime types, native null, nested carriers and mixed repeated carriers all fail")
        results.foreach(_.isSuccess shouldBe false)
      }

      "leave other carrier shapes and noncanonical datatype paths unchanged" in {
        Given("List, Array and NonEmptyVector carriers plus a nonstructured integer Vector")
        val jsonop = new StructuredOperationDefinition(XJson, Multiplicity.ZeroMore)
        val carriers = Vector[Any](List(Json.obj()), Array(Json.obj()), cats.data.NonEmptyVector.one(Json.obj()))
        val integerop = new StructuredOperationDefinition(XNonNegativeInteger, Multiplicity.ZeroMore)
        When("resolution uses only the admitted exact structured Vector adaptation")
        val results = carriers.map(value => jsonop.resolve(_structured_request(List(value)))) :+
          integerop.resolve(_structured_request(List(Vector(1, 2))))
        Then("unsupported carrier shapes and integer vectors remain rejected")
        results.foreach(_.isSuccess shouldBe false)
      }
    }
  }

  "OperationDefinition.resolveParameter" should {
    "produce OperationRequest when normalization and validation succeed" in {
      Given("an operation using XNonNegativeInteger")
      val op = new IntOperationDefinition(XNonNegativeInteger)
      When("resolving a valid integer value")
      val result = op.createOperationRequest(requestWith("0"))

      result match {
        case Consequence.Success(opreq: IntRequest) =>
          Then("the OperationRequest is generated with normalized value")
          opreq.value.shouldBe(BigInt(0))
        case _ =>
          fail("expected Success with IntRequest")
      }
    }

    "produce OperationRequest property-wise for valid integer strings" in {
      Given("operations using integer datatypes")
      val nonNegative = new IntOperationDefinition(XNonNegativeInteger)
      val positive = new IntOperationDefinition(XPositiveInteger)
      When("resolving arbitrary valid values")
      forAll(Gen.chooseNum(0L, Long.MaxValue)) { value =>
        nonNegative.createOperationRequest(requestWith(value.toString)) match {
          case Consequence.Success(opreq: IntRequest) =>
            opreq.value.shouldBe(BigInt(value))
          case _ =>
            fail(s"expected Success for value: ${value}")
        }
      }
      Then("valid values always generate OperationRequest")
      forAll(Gen.chooseNum(1L, Long.MaxValue)) { value =>
        positive.createOperationRequest(requestWith(value.toString)) match {
          case Consequence.Success(opreq: IntRequest) =>
            opreq.value.shouldBe(BigInt(value))
          case _ =>
            fail(s"expected Success for value: ${value}")
        }
      }
      Then("valid values always generate OperationRequest")
    }

    "fail with ValueDomainError for invalid integer domains" in {
      Given("operations using integer datatypes with domain predicates")
      val nonNegative = new IntOperationDefinition(XNonNegativeInteger)
      val positive = new IntOperationDefinition(XPositiveInteger)

      When("resolving domain-violating values")
      Then("each failure reports ValueDomainError")
      // assert_taxonomy(nonNegative.createOperationRequest(requestWith("-1")), Taxonomy.argumentInvalid)
      // assert_taxonomy(positive.createOperationRequest(requestWith("0")), Taxonomy.argumentInvalid)
      nonNegative.createOperationRequest(requestWith("-1")) should be_argument_invalid_failure
      positive.createOperationRequest(requestWith("0")) should be_argument_invalid_failure
    }

    "fail with ValueDomainError property-wise for negative values" in {
      Given("an operation using XNonNegativeInteger")
      val nonNegative = new IntOperationDefinition(XNonNegativeInteger)
      When("resolving negative values")
      forAll(Gen.chooseNum(Long.MinValue, -1L)) { value =>
        Then("each failure reports ValueDomainError")
        assert_datatype(
          nonNegative.createOperationRequest(requestWith(value.toString))
        )
      }
    }

    "fail with FormatError for invalid integer formats" in {
      Given("an operation using XPositiveInteger")
      val op = new IntOperationDefinition(XPositiveInteger)
      When("resolving a non-numeric string")
      Then("the failure reports FormatError")
      assert_taxonomy(
        op.createOperationRequest(requestWith("abc")),
        Taxonomy.argumentFormatError
      )
    }

    "fail with FormatError property-wise for alphabetic strings" in {
      Given("an operation using XPositiveInteger")
      val op = new IntOperationDefinition(XPositiveInteger)
      When("resolving alphabetic strings")
      forAll(Gen.alphaStr.suchThat(_.nonEmpty)) { value =>
        Then("each failure reports FormatError")
        assert_taxonomy(
          op.createOperationRequest(requestWith(value)),
          Taxonomy.argumentFormatError
        )
      }
    }

    "validate enum constraints during OperationRequest construction" in {
      Given("an operation with enum constraints on string parameters")
      val op = new StringOperationDefinition(
        constraints = Vector(Constraint.Enum(Set("a", "b")))
      )

      When("resolving an allowed value")
      op.createOperationRequest(requestWith("a")) match {
        case Consequence.Success(opreq: StringRequest) =>
          Then("the OperationRequest is generated")
          opreq.value.shouldBe("a")
        case _ =>
          fail("expected Success with StringRequest")
      }

      When("resolving a disallowed value")
      Then("the failure reports ValueDomainError")
      assert_constraint(
        op.createOperationRequest(requestWith("c"))
      )
    }

    "validate numeric range constraints during OperationRequest construction" in {
      Given("an operation with integer range constraints")
      val op = new IntOperationDefinition(
        datatype = XNonNegativeInteger,
        constraints = Vector(
          Constraint.MinInclusive(BigInt(0)),
          Constraint.MaxInclusive(BigInt(10))
        )
      )

      When("resolving a value within the range")
      op.createOperationRequest(requestWith("5")) match {
        case Consequence.Success(opreq: IntRequest) =>
          Then("the OperationRequest is generated")
          opreq.value.shouldBe(BigInt(5))
        case _ =>
          fail("expected Success with IntRequest")
      }

      When("resolving values outside the range")
      Then("each failure reports ValueDomainError")
      assert_datatype(
        op.createOperationRequest(requestWith("-1"))
      )
      assert_constraint(
        op.createOperationRequest(requestWith("11"))
      )
    }

    "validate range constraints property-wise" in {
      Given("an operation with integer range constraints")
      val op = new IntOperationDefinition(
        datatype = XNonNegativeInteger,
        constraints = Vector(
          Constraint.MinInclusive(BigInt(0)),
          Constraint.MaxInclusive(BigInt(10))
        )
      )

      When("resolving values within the range")
      forAll(Gen.chooseNum(0L, 10L)) { value =>
        op.createOperationRequest(requestWith(value.toString)) match {
          case Consequence.Success(opreq: IntRequest) =>
            opreq.value.shouldBe(BigInt(value))
          case _ =>
            fail(s"expected Success for value: ${value}")
        }
      }

      When("resolving values outside the range")
      forAll(Gen.oneOf(Gen.chooseNum(-10L, -1L), Gen.chooseNum(11L, 20L))) { value =>
        if (value < 0)
          assert_datatype(
            op.createOperationRequest(requestWith(value.toString))
          )
        else
          assert_constraint(
            op.createOperationRequest(requestWith(value.toString))
          )
      }
    }

    "enforce validation in default OperationDefinition.Instance" in {
      Given("a default OperationDefinition with a notEmpty constraint")
      val op = OperationDefinition(
        content = org.goldenport.value.BaseContent.simple("string-operation"),
        request = RequestDefinition(
          parameters = List(
            ParameterDefinition(
              content = org.goldenport.value.BaseContent.simple("value"),
              kind = ParameterDefinition.Kind.Argument,
              domain = ValueDomain(
                datatype = XString,
                multiplicity = Multiplicity.One,
                constraints = Vector(Constraint.NotEmpty)
              )
            )
          )
        ),
        response = ResponseDefinition.void
      )

      When("resolving a violating value through the default instance")
      val req = Request(
        component = None,
        service = None,
        operation = "string-operation",
        arguments = List(Argument("value", "", None)),
        switches = Nil,
        properties = Nil
      )
      val result = op.createOperationRequest(req)

      Then("the failure reports ValueDomainError")
      assert_constraint(result)
    }

    "keep Argument Missing and Redundant semantics" in {
      Given("an operation with a required integer argument")
      val op = new IntOperationDefinition(XNonNegativeInteger)
      val missing = Request(
        component = None,
        service = None,
        operation = "int-operation",
        arguments = Nil,
        switches = Nil,
        properties = Nil
      )

      When("resolving a request with a missing argument")
      Then("Missing is reported as Argument error")
      assert_taxonomy(op.createOperationRequest(missing), Taxonomy.argumentMissing)

      When("resolving a request with redundant arguments")
      val redundant = requestWith("1", extra = List("2"))
      Then("Redundant is reported as Argument error")
      assert_taxonomy(op.createOperationRequest(redundant), Taxonomy.argumentRedundant)
    }
  }
}
