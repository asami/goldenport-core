package org.goldenport.convert

import java.math.MathContext
import java.nio.charset.StandardCharsets
import java.time.{Clock, Instant, ZoneOffset}
import java.util.Locale
import cats.data.NonEmptyVector
import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.context.*
import org.goldenport.log.Logger
import org.goldenport.record.Record
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Oct. 18, 2026
 * @version Oct. 18, 2026
 */
final class CollectionValueReaderSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import CollectionValueReaderSpec.*

  "Ordered collection readers" should {
    "distinguish missing fields, empty Vectors and invalid empty NonEmptyVectors" in {
      Given("a present empty collection and a separate missing field")
      val record = Record.data("items" -> Vector.empty[Int])
      given ExecutionContext = _context
      When("ordinary and contextual optional collection reads interpret both fields")
      val missing = Vector(record.getAsC[Vector[Int]]("missing"), record.getAsContextC[Vector[Int]]("missing"))
      val missingnev = Vector(record.getAsC[NonEmptyVector[Int]]("missing"), record.getAsContextC[NonEmptyVector[Int]]("missing"))
      val empty = Vector(record.getAsC[Vector[Int]]("items"), record.getAsContextC[Vector[Int]]("items"))
      val emptynev = Vector(record.getAsC[NonEmptyVector[Int]]("items"), record.getAsContextC[NonEmptyVector[Int]]("items"))
      Then("absence succeeds as None, emptiness succeeds only for Vector")
      missing.foreach(_.get shouldBe Some(None))
      missingnev.foreach(_.get shouldBe Some(None))
      empty.foreach(_.get shouldBe Some(Some(Vector.empty)))
      emptynev.foreach(_.isSuccess shouldBe false)
    }

    "admit Seq, primitive Array and actual NonEmptyVector in their original order" in {
      Given("three supported collection shapes with duplicates")
      val inputs = Vector[Any](List(3, 1, 3), Array(3, 1, 3), NonEmptyVector.of(3, 1, 3))
      given ExecutionContext = _context
      When("the public readers and real Record getters interpret each shape")
      val direct = inputs.map(summon[ValueReader[Vector[Int]]].readC)
      val records = inputs.map(value => Record.data("items" -> value))
      val vectors = records.flatMap(record => Vector(record.getAsC[Vector[Int]]("items"), record.getAsContextC[Vector[Int]]("items")))
      val nonempty = records.flatMap(record => Vector(record.getAsC[NonEmptyVector[Int]]("items"), record.getAsContextC[NonEmptyVector[Int]]("items")))
      Then("all positions and duplicates survive both reader families")
      direct.foreach(_.get shouldBe Some(Vector(3, 1, 3)))
      vectors.foreach(_.get shouldBe Some(Some(Vector(3, 1, 3))))
      nonempty.foreach(_.get shouldBe Some(Some(NonEmptyVector.of(3, 1, 3))))
    }

    "preserve ordered duplicate canonical Records and normalize nested object Json" in {
      Given("object Json containing omitted attributes, nested objects and exact numbers")
      val json = Json.obj("n" -> Json.fromInt(7), "absent" -> Json.Null,
        "child" -> Json.obj("items" -> Json.arr(Json.fromInt(2))))
      val expected = Record.data("n" -> BigDecimal(7), "child" -> Record.data("items" -> Vector(BigDecimal(2))))
      val record = Record.data("items" -> Vector(json, json))
      given ExecutionContext = _context
      When("ordinary and contextual getters request canonical Record elements")
      val vectors = Vector(record.getAsC[Vector[Record]]("items"), record.getAsContextC[Vector[Record]]("items"))
      val nonempty = Vector(record.getAsC[NonEmptyVector[Record]]("items"), record.getAsContextC[NonEmptyVector[Record]]("items"))
      Then("each object becomes the independent canonical expected Record without losing duplicates")
      vectors.foreach(_.get shouldBe Some(Some(Vector(expected, expected))))
      nonempty.foreach(_.get shouldBe Some(Some(NonEmptyVector.of(expected, expected))))
    }

    "retain raw Json null and embedded null shapes through Record getters" in {
      Given("typed Json values that remain distinct from canonical Record values")
      val json = Json.arr(Json.Null, Json.obj("missing" -> Json.Null))
      val record = Record.data("items" -> NonEmptyVector.of(Json.Null, json, Json.Null))
      given ExecutionContext = _context
      When("both optional read routes request Json collections")
      val vector = record.getAsC[Vector[Json]]("items")
      val nonempty = record.getAsContextC[NonEmptyVector[Json]]("items")
      Then("raw shapes, nulls and duplicate positions remain present")
      vector.get shouldBe Some(Some(Vector(Json.Null, json, Json.Null)))
      nonempty.get shouldBe Some(Some(NonEmptyVector.of(Json.Null, json, Json.Null)))
      vector.get.get.get(1) should be theSameInstanceAs json
    }

    "reject wrong roots and malformed elements without substituting empty values" in {
      Given("unsupported collection roots and invalid Int element text")
      val roots = Vector[Any](null, "1", 1, Json.arr(), Map("item" -> 1), Set(1))
      val record = Record.data("items" -> Vector("bad", "also-bad"))
      given ExecutionContext = _context
      When("strict readers interpret roots and ordinary and contextual getters decode malformed elements")
      val rootresults = roots.flatMap(value => Vector(summon[ValueReader[Vector[Int]]].readC(value), summon[ValueReader[NonEmptyVector[Int]]].readC(value)))
      val results = Vector(record.getAsC[Vector[Int]]("items"), record.getAsContextC[Vector[Int]]("items"),
        record.getAsC[NonEmptyVector[Int]]("items"), record.getAsContextC[NonEmptyVector[Int]]("items"))
      Then("every root fails and each element failure remains independently available")
      rootresults.foreach(_.isSuccess shouldBe false)
      results.foreach {
        case Consequence.Failure(conclusion) => conclusion.causes should have size 2
        case _ => fail("Malformed elements must remain aggregated failures")
      }
    }

    "reject native null positions before permissive element callbacks" in {
      Given("a permissive reader and supported roots containing a native null")
      var calls = 0
      val reader = new ValueReader[Any] {
        def readC(value: Any): Consequence[Any] = { calls += 1; Consequence.success(value) }
      }
      given ValueReader[Any] = reader
      val inputs = Vector[Any](Vector[Any](null), Array[Any](null), NonEmptyVector.of[Any](null))
      When("ordinary and contextual Vector and NonEmptyVector readers interpret those positions")
      given ExecutionContext = _context
      val results = inputs.flatMap(value => Vector(ValueReader.vector(reader).readC(value), ValueReader.vector(reader).readContextC(value),
        summon[ValueReader[NonEmptyVector[Any]]].readC(value), summon[ValueReader[NonEmptyVector[Any]]].readContextC(value)))
      Then("all reads fail before the callback can accept null")
      results.foreach(_.isSuccess shouldBe false)
      calls shouldBe 0
    }

    "permit scalar promotion only through the explicitly selected factory policy" in {
      Given("an Int reader and strict or explicitly scalar-promoting collection policies")
      val strict = ValueReader.vector(summon[ValueReader[Int]])
      val promoted = ValueReader.vector(summon[ValueReader[Int]], allowSingle = true)
      given ExecutionContext = _context
      When("each policy reads scalar text, invalid scalar text and native null")
      val strictresult = strict.readC("9")
      val promotedresults = Vector(promoted.readC("9"), promoted.readContextC("9"))
      val invalid = Vector(promoted.readC("bad"), promoted.readC(null), promoted.readContextC(null))
      Then("only the explicit policy promotes a successfully decoded non-null scalar")
      strictresult.isSuccess shouldBe false
      promotedresults.foreach(_.get shouldBe Some(Vector(9)))
      invalid.foreach(_.isSuccess shouldBe false)
    }

    "forward the identical read context to every Vector and NonEmptyVector element" in {
      Given("an element reader whose ordinary interpretation fails and contextual interpretation retains identity")
      val context = _context
      given ValueReader[ContextValue] with {
        def readC(value: Any): Consequence[ContextValue] = Consequence.valueInvalid("Context required")
        override def readContextC(value: Any)(using ctx: ExecutionContext): Consequence[ContextValue] =
          Consequence.success(ContextValue(ctx))
      }
      val record = Record.data("items" -> Vector("first", "second", "first"))
      When("real Record getters use the supplied context")
      val vector = record.getAsContextC[Vector[ContextValue]]("items")(using summon[ValueReader[Vector[ContextValue]]], context)
      val nonempty = record.getAsContextC[NonEmptyVector[ContextValue]]("items")(using summon[ValueReader[NonEmptyVector[ContextValue]]], context)
      val plain = record.getAsC[Vector[ContextValue]]("items")
      Then("all contextual callbacks see that exact context and the ordinary route retains its failure")
      vector.get.get.get should have size 3
      vector.get.get.get.foreach(_.context should be theSameInstanceAs context)
      nonempty.get.get.get.toVector.foreach(_.context should be theSameInstanceAs context)
      plain.isSuccess shouldBe false
    }

    "delegate application Record admission while retaining typed values and rejecting other roots" in {
      Given("an explicit application decoder and a Record containing an application-owned opaque value")
      val opaque = new Object()
      val record = Record.data("payload" -> opaque)
      val typed = CustomValue(opaque)
      var calls = 0
      val reader = ValueReader.recordReader[CustomValue] { value =>
        calls += 1
        value.getAny("payload").map(payload => Consequence.success(CustomValue(payload)))
          .getOrElse(Consequence.valueInvalid("Payload required"))
      }
      When("the adapter reads a Record, an already typed value, and foreign shapes")
      val decoded = reader.readC(record)
      val retained = reader.readC(typed)
      val invalid = Vector[Any](Json.obj(), "payload", null, 1).map(reader.readC)
      Then("Record decoding runs once without canonical normalization and typed identity remains intact")
      decoded.get shouldBe Some(typed)
      retained.get.get should be theSameInstanceAs typed
      calls shouldBe 1
      invalid.foreach(_.isSuccess shouldBe false)
    }

    "compose explicit application readers with real collection getters" in {
      Given("a custom record adapter and duplicate Records in a field")
      given ValueReader[CustomValue] = ValueReader.recordReader[CustomValue](record =>
        record.getAsC[String]("payload").flatMap {
          case Some(value) => Consequence.success(CustomValue(value))
          case None => Consequence.valueInvalid("Payload required")
        })
      val record = Record.data("items" -> Vector(Record.data("payload" -> "a"), Record.data("payload" -> "a")))
      given ExecutionContext = _context
      When("ordinary and contextual optional getters request custom values")
      val vector = record.getAsC[Vector[CustomValue]]("items")
      val nonempty = record.getAsContextC[NonEmptyVector[CustomValue]]("items")
      Then("the Core traversal preserves custom decoder ownership and duplicate order")
      vector.get shouldBe Some(Some(Vector(CustomValue("a"), CustomValue("a"))))
      nonempty.get shouldBe Some(Some(NonEmptyVector.of(CustomValue("a"), CustomValue("a"))))
    }

    "preserve generated order, duplicate positions and values across both collection families" in {
      Given("a generator with two independently varying values and a guaranteed duplicate position")
      val generator = for { first <- Gen.choose(-100000, 100000); second <- Gen.choose(-100000, 100000) } yield (first, second)
      val law = Prop.forAll(generator) { case (first, second) =>
        val values = Vector(first, second, first)
        val records = values.map(value => Record.data("value" -> BigDecimal(value)))
        val jsons = values.map(Json.fromInt)
        val record = Record.data("records" -> records, "jsons" -> jsons)
        record.getAsC[Vector[Record]]("records").get.contains(Some(records)) &&
          record.getAsC[NonEmptyVector[Record]]("records").get.contains(Some(NonEmptyVector(records.head, records.tail))) &&
          record.getAsC[Vector[Json]]("jsons").get.contains(Some(jsons)) &&
          record.getAsC[NonEmptyVector[Json]]("jsons").get.contains(Some(NonEmptyVector(jsons.head, jsons.tail)))
      }
      When("ScalaCheck evaluates at least one hundred successful order and value samples")
      val result = Test.check(Test.Parameters.default.withMinSuccessfulTests(100), law)
      Then("the law passes without discarded inputs")
      result.passed shouldBe true
      result.succeeded should be >= 100
      result.discarded shouldBe 0
    }
  }

  private def _context: ExecutionContext = {
    val clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)
    val logger = new Logger {
      def trace(message: => String): Unit = ()
      def debug(message: => String): Unit = ()
      def info(message: => String): Unit = ()
      def warn(message: => String): Unit = ()
      def error(message: => String): Unit = ()
      def error(cause: Throwable, message: => String): Unit = ()
      def fatal(message: => String): Unit = ()
      def fatal(cause: Throwable, message: => String): Unit = ()
    }
    val environment = EnvironmentContext.Instant(EnvironmentContext.Core("collection-specification"))
    val vm = VirtualMachineContext.Instant(VirtualMachineContext.Core(
      clock = clock, timezone = ZoneOffset.UTC, encoding = StandardCharsets.UTF_8,
      lineSeparator = "\n", mathContext = MathContext.UNLIMITED, environmentVariables = Map.empty,
      resourceBundleBaseNames = Nil, resourceBundleLocales = Nil, resourceBundleResolutionOrder = Nil))
    val i18n = I18nContext.Instant(I18nContext.Core(textNormalizationPolicy = "JapaneseCanonical",
      textComparisonPolicy = "JapaneseCollation", dateTimeFormatPolicy = "Iso8601", locale = None))
    ExecutionContext.Instant(ExecutionContext.Core(environment = environment, vm = vm, i18n = i18n,
      locale = Locale.JAPAN, timezone = ZoneOffset.UTC, encoding = StandardCharsets.UTF_8, clock = clock,
      math = MathContext.UNLIMITED, random = RandomContext.seeded("collection-specification"),
      entropy = EntropyContext.deterministic("collection-specification"), logger = logger))
  }
}

object CollectionValueReaderSpec {
  final case class ContextValue(context: ExecutionContext)
  final case class CustomValue(value: Any)
}
