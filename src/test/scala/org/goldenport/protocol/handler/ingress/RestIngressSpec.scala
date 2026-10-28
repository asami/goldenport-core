package org.goldenport.protocol.handler.ingress

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import io.circe.Json
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers

import org.goldenport.Consequence
import org.goldenport.bag.Bag
import org.goldenport.datatype.{ContentType, MimeBody}
import org.goldenport.protocol.{Argument, Property}
import org.goldenport.protocol.Request
import org.goldenport.protocol.spec.{OperationDefinition, ParameterDefinition, RequestDefinition, ResponseDefinition}
import org.goldenport.http.HttpRequest
import org.goldenport.record.Record
import org.goldenport.schema.{Multiplicity, ValueDomain, XJson, XString}
import org.goldenport.value.BaseContent

/*
 * @since   Apr. 23, 2026
 *  version Jul. 20, 2026
 * @version Oct. 28, 2026
 * @author  ASAMI, Tomoharu
 */
class RestIngressSpec
    extends AnyWordSpec
    with GivenWhenThen
    with Matchers
    with ScalaCheckDrivenPropertyChecks {

  "RestIngress" should {
    "encode GET with no query and no body as an empty Request" in {
      Given("an HttpRequest with GET method and no parameters")
      val http =
        HttpRequest.fromPath(
          method = HttpRequest.GET,
          path = "/any/service/op",
          query = Record.empty,
          header = Record.empty,
          body = None
        )

      val opdef =
        OperationDefinition(
          content = org.goldenport.value.BaseContent.simple("op"),
          request = RequestDefinition(parameters = Nil),
          response = ResponseDefinition.void
        )

      val ingress =
        IngressCollection(
          ingresses = Vector(new RestIngress {})
        ).ingress(http).TAKE

      When("encoding HttpRequest into Request")
      val result = ingress.encode(opdef, http)

      result match {
        case Consequence.Success(req) =>
          Then("it succeeds and produces empty parameters")
          req.operation shouldBe "op"
          req.arguments shouldBe Nil
          req.switches shouldBe Nil
          req.properties shouldBe Nil

        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "encode GET query parameters as named Arguments" in {
      Given("an HttpRequest with GET method and query parameters")
      val http =
        HttpRequest.fromPath(
          method = HttpRequest.GET,
          path = "/query",
          query = Record.data("query" -> "target"),
          header = Record.empty
        )

      val opdef =
        OperationDefinition(
          content = org.goldenport.value.BaseContent.simple("query"),
          request = RequestDefinition(parameters = Nil),
          response = ResponseDefinition.void
        )

      val ingress =
        IngressCollection(
          ingresses = Vector(new RestIngress {})
        ).ingress(http).TAKE

      When("encoding HttpRequest into Request")
      val result = ingress.encode(opdef, http)

      result match {
        case Consequence.Success(req) =>
          Then("it succeeds and produces a canonical Request")
          req.operation shouldBe "query"
          req.arguments shouldBe List(
            Argument("query", "target", None)
          )
          req.properties shouldBe Nil

        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "encode POST form parameters as named Arguments" in {
      Given("an HttpRequest with POST method and form parameters")
      val http =
        HttpRequest.fromPath(
          method = HttpRequest.POST,
          path = "/query",
          query = Record.empty,
          header = Record.empty,
          form = Record.data("name" -> "alice")
        )

      val opdef =
        OperationDefinition(
          content = org.goldenport.value.BaseContent.simple("query"),
          request = RequestDefinition(parameters = Nil),
          response = ResponseDefinition.void
        )

      val ingress =
        IngressCollection(
          ingresses = Vector(new RestIngress {})
        ).ingress(http).TAKE

      When("encoding HttpRequest into Request")
      val result = ingress.encode(opdef, http)

      result match {
        case Consequence.Success(req) =>
          Then("it succeeds and produces a canonical Request")
          req.operation shouldBe "query"
          req.arguments shouldBe List(
            Argument("name", "alice", None)
          )
          req.properties shouldBe Nil

        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "preserve explicitly supplied POST collection arguments" in {
      Given("an HttpRequest with repeated form parameters")
      val http =
        HttpRequest.fromPath(
          method = HttpRequest.POST,
          path = "/facility/update",
          query = Record.empty,
          header = Record.empty,
          form = Record.data(
            "id" -> "facility-1",
            "fetch_methods" -> Vector("official_driver", "museum_or_jp")
          )
        )

      val opdef =
        OperationDefinition(
          content = BaseContent.simple("updateFacilityRecord"),
          request = RequestDefinition(parameters = List(
            ParameterDefinition(
              BaseContent.simple("id"),
              kind = ParameterDefinition.Kind.Argument,
              domain = ValueDomain(datatype = XString, multiplicity = Multiplicity.One)
            ),
            ParameterDefinition(
              BaseContent.simple("fetch_methods"),
              kind = ParameterDefinition.Kind.Argument,
              domain = ValueDomain(datatype = XString, multiplicity = Multiplicity.ZeroMore)
            )
          )),
          response = ResponseDefinition.void
        )

      val ingress =
        IngressCollection(
          ingresses = Vector(new RestIngress {})
        ).ingress(http).TAKE

      When("encoding HttpRequest into Request")
      val result = ingress.encode(opdef, http)

      result match {
        case Consequence.Success(req) =>
          Then("it preserves the explicit collection for operation multiplicity handling")
          req.arguments shouldBe List(
            Argument("id", "facility-1", None),
            Argument("fetch_methods", Vector("official_driver", "museum_or_jp"), None)
          )
          req.properties shouldBe Nil

        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "preserve an explicitly empty repeated POST form property" in {
      Given("an HttpRequest whose repeated form control was explicitly cleared")
      val http =
        HttpRequest.fromPath(
          method = HttpRequest.POST,
          path = "/facility/update",
          query = Record.empty,
          header = Record.empty,
          form = Record.data("fetch_methods" -> Vector.empty[String])
        )
      val opdef =
        OperationDefinition(
          content = BaseContent.simple("updateFacility"),
          request = RequestDefinition(parameters = List(
            ParameterDefinition(
              BaseContent.simple("fetch_methods"),
              kind = ParameterDefinition.Kind.Property,
              domain = ValueDomain(datatype = XString, multiplicity = Multiplicity.ZeroMore)
            )
          )),
          response = ResponseDefinition.void
        )
      val ingress = IngressCollection(
        ingresses = Vector(new RestIngress {})
      ).ingress(http).TAKE

      When("encoding the form into a protocol Request")
      val result = ingress.encode(opdef, http)

      result match {
        case Consequence.Success(req) =>
          Then("the explicit empty collection remains distinguishable from an omitted field")
          req.arguments shouldBe Nil
          req.properties shouldBe List(Property("fetch_methods", Vector.empty, None))
        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "retain exact opaque collection strings for every cardinality and declared parameter kind" in {
      val opaque = Gen.listOf(Gen.oneOf("a", " ", "雪", "é", "e\u0301", "🧭", "/", "%", "\n"))
        .map(_.mkString)
      forAll(Gen.listOf(opaque)) { sample =>
        Given("declared collection Arguments and Properties with opaque ordered strings")
        val values = sample.toVector
        val opdef = OperationDefinition(
          content = BaseContent.simple("create"),
          request = RequestDefinition(parameters = List(
            ParameterDefinition(BaseContent.simple("resources"), ParameterDefinition.Kind.Property,
              ValueDomain(datatype = XString, multiplicity = Multiplicity.ZeroMore)),
            ParameterDefinition(BaseContent.simple("sources"), ParameterDefinition.Kind.Argument,
              ValueDomain(datatype = XString, multiplicity = Multiplicity.ZeroMore))
          )), response = ResponseDefinition.void)
        When("explicit empty singleton and sampled arrays pass through REST and Request Record conversion")
        val results = Vector(Vector.empty[String], Vector(" 雪/e\u0301/%🧭 "), values).map { entries =>
          val http = HttpRequest.fromPath(method = HttpRequest.POST, path = "/create",
            form = Record.data("resources" -> entries, "sources" -> entries))
          entries -> RestIngress().encode(opdef, http).TAKE
        }
        Then("the carrier cardinality order duplicates whitespace and Unicode remain exact")
        results.foreach { case (entries, req) =>
          req.properties shouldBe List(Property("resources", entries, None))
          req.arguments shouldBe List(Argument("sources", entries, None))
          req.toRecord.getAny("resources") shouldBe Some(entries)
          req.toRecord.getAny("sources") shouldBe Some(entries)
          opdef.createOperationRequest(req).isSuccess shouldBe true
        }
      }
    }

    "retain decoded JSON string arrays through native Request Record conversion" in {
      Given("JSON object input with explicitly empty singleton and multiple resource arrays")
      val opdef = OperationDefinition(content = BaseContent.simple("create"),
        request = RequestDefinition(parameters = List(ParameterDefinition(BaseContent.simple("resources"),
          ParameterDefinition.Kind.Property,
          ValueDomain(datatype = XString, multiplicity = Multiplicity.ZeroMore)))),
        response = ResponseDefinition.void)
      val cases = Vector(
        "{\"resources\":[]}" -> Vector.empty[String],
        """{"resources":[" 雪/%🧭 "]}""" -> Vector(" 雪/%🧭 "),
        "{\"resources\":[\"second\",\"first\",\"second\"]}" -> Vector("second", "first", "second"))
      When("decoded JSON Records pass through REST ingress")
      val results = cases.map { case (text, expected) =>
        val form = org.goldenport.record.io.RecordDecoder().json(text).TAKE
        val req = RestIngress().encode(opdef,
          HttpRequest.fromPath(method = HttpRequest.POST, path = "/create", form = form)).TAKE
        expected -> req
      }
      Then("explicit JSON array structure and exact ordered strings survive including zero and one entries")
      results.foreach { case (expected, req) =>
        req.properties shouldBe List(Property("resources", expected, None))
        req.toRecord.getAny("resources") shouldBe Some(expected)
      }
    }

    "retain legacy sequence expansion for undeclared and scalar parameter inputs" in {
      Given("a scalar string property and legacy List-valued inputs")
      val opdef = OperationDefinition(content = BaseContent.simple("update"),
        request = RequestDefinition(parameters = List(ParameterDefinition(BaseContent.simple("value"),
          ParameterDefinition.Kind.Property,
          ValueDomain(datatype = XString, multiplicity = Multiplicity.One)))),
        response = ResponseDefinition.void)
      When("REST encodes a scalar parameter Vector and an undeclared List")
      val req = RestIngress().encode(opdef, HttpRequest.fromPath(
        method = HttpRequest.POST, path = "/update",
        form = Record.data("value" -> Vector("a", "b"), "other" -> List("c", "d")))).TAKE
      Then("the existing repeated representation remains and scalar multiplicity rejects redundant values")
      req.properties shouldBe List(Property("value", "a", None), Property("value", "b", None))
      req.arguments shouldBe List(Argument("other", "c", None), Argument("other", "d", None))
      opdef.createOperationRequest(req).isSuccess shouldBe false
    }

    "canonicalize collection aliases without promoting scalar input or changing undeclared repeats" in {
      Given("a declared collection alias and a scalar supplied to the same definition")
      val content = BaseContent.simple("resources")
      val aliased = content.copy(nameAttributes = content.nameAttributes.copy(
        alias = Some(cats.data.NonEmptyVector.one(org.goldenport.datatype.I18nLabel("selected")))))
      val opdef = OperationDefinition(content = BaseContent.simple("create"),
        request = RequestDefinition(parameters = List(ParameterDefinition(aliased,
          ParameterDefinition.Kind.Property,
          ValueDomain(datatype = XString, multiplicity = Multiplicity.OneMore)))),
        response = ResponseDefinition.void)
      When("REST encodes an alias Vector scalar and undeclared repeated field")
      def _encode_(record: Record): Request = RestIngress().encode(opdef,
        HttpRequest.fromPath(method = HttpRequest.POST, path = "/create", form = record)).TAKE
      val collection = _encode_(Record.data("selected" -> Vector("雪")))
      val scalar = _encode_(Record.data("selected" -> "雪"))
      val repeated = _encode_(Record.data("other" -> Vector("first", "second")))
      Then("the declared name is canonical while scalar and undeclared behavior stay distinct")
      collection.properties shouldBe List(Property("resources", Vector("雪"), None))
      scalar.properties shouldBe List(Property("resources", "雪", None))
      scalar.toRecord.getAny("resources") shouldBe Some("雪")
      repeated.arguments shouldBe List(Argument("other", "first", None), Argument("other", "second", None))
    }

    "preserve raw Json documents and collection elements without transport stringification" in {
      Given("a Json collection containing primitives objects null and an array document")
      val values = Vector(Json.fromString("雪"), Json.fromInt(7), Json.fromBoolean(true),
        Json.obj("key" -> Json.fromString("value")), Json.Null, Json.arr(Json.fromInt(1)))
      val opdef = OperationDefinition(content = BaseContent.simple("documents"),
        request = RequestDefinition(parameters = List(ParameterDefinition(BaseContent.simple("value"),
          ParameterDefinition.Kind.Property,
          ValueDomain(datatype = XJson, multiplicity = Multiplicity.ZeroMore)))),
        response = ResponseDefinition.void)
      When("REST receives a Vector of Json elements and a scalar Json array document")
      val http = HttpRequest.fromPath(method = HttpRequest.POST, path = "/documents",
        form = Record.data("value" -> values))
      val req = RestIngress().encode(opdef, http).TAKE
      val array = Json.arr(Json.fromInt(1), Json.Null)
      val arrayreq = RestIngress().encode(opdef,
        HttpRequest.fromPath(method = HttpRequest.POST, path = "/documents",
          form = Record.data("value" -> array))).TAKE
      Then("all raw Json values retain their meaning and the scalar array remains one document")
      req.properties shouldBe List(Property("value", values, None))
      req.toRecord.getAny("value") shouldBe Some(values)
      opdef.createOperationRequest(req).isSuccess shouldBe true
      arrayreq.properties shouldBe List(Property("value", array, None))
      opdef.createOperationRequest(arrayreq).isSuccess shouldBe true
    }

    "preserve multipart MimeBody form values as named Arguments" in {
      Given("an HttpRequest with a binary multipart form value")
      val body = MimeBody(ContentType.IMAGE_PNG, Bag.binary(Array[Byte](1, 2, 3)))
      val http =
        HttpRequest.fromPath(
          method = HttpRequest.POST,
          path = "/upload",
          query = Record.empty,
          header = Record.empty,
          form = Record.data("blob.mainImage" -> body)
        )

      val opdef =
        OperationDefinition(
          content = org.goldenport.value.BaseContent.simple("upload"),
          request = RequestDefinition(parameters = Nil),
          response = ResponseDefinition.void
        )

      val ingress =
        IngressCollection(
          ingresses = Vector(new RestIngress {})
        ).ingress(http).TAKE

      When("encoding HttpRequest into Request")
      val result = ingress.encode(opdef, http)

      result match {
        case Consequence.Success(req) =>
          Then("the MimeBody remains available to operation code")
          req.arguments.map(_.name) shouldBe List("blob.mainImage")
          req.arguments.head.value shouldBe body

        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "encode POST with body but no parameters as an empty Request" in {
      Given("an HttpRequest with POST method and body only")
      val http =
        HttpRequest.fromPath(
          method = HttpRequest.POST,
          path = "/query",
          query = Record.empty,
          header = Record.empty,
          body = Some(Bag.text("payload")),
          form = Record.empty
        )

      val opdef =
        OperationDefinition(
          content = org.goldenport.value.BaseContent.simple("query"),
          request = RequestDefinition(parameters = Nil),
          response = ResponseDefinition.void
        )

      val ingress =
        IngressCollection(
          ingresses = Vector(new RestIngress {})
        ).ingress(http).TAKE

      When("encoding HttpRequest into Request")
      val result = ingress.encode(opdef, http)

      result match {
        case Consequence.Success(req) =>
          Then("it succeeds and produces empty parameters")
          req.operation shouldBe "query"
          req.arguments shouldBe Nil
          req.switches shouldBe Nil
          req.properties shouldBe Nil

        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }

    "carry HTTP headers into Request properties" in {
      Given("an HttpRequest with authentication and cookie headers")
      val http =
        HttpRequest.fromPath(
          method = HttpRequest.POST,
          path = "/query",
          query = Record.empty,
          header = Record.data(
            "x-textus-session" -> "sess-1",
            "Cookie" -> "textus-session-test=sess-1"
          ),
          form = Record.data("name" -> "alice")
        )

      val opdef =
        OperationDefinition(
          content = org.goldenport.value.BaseContent.simple("query"),
          request = RequestDefinition(parameters = Nil),
          response = ResponseDefinition.void
        )

      val ingress =
        IngressCollection(
          ingresses = Vector(new RestIngress {})
        ).ingress(http).TAKE

      When("encoding HttpRequest into Request")
      val result = ingress.encode(opdef, http)

      result match {
        case Consequence.Success(req) =>
          Then("it preserves headers as Request properties for downstream ingress resolution")
          req.arguments shouldBe List(
            Argument("name", "alice", None)
          )
          req.properties should contain (Property("x-textus-session", "sess-1", None))
          req.properties should contain (Property("Cookie", "textus-session-test=sess-1", None))

        case Consequence.Failure(err) =>
          fail(err.toString)
      }
    }
  }
}
