package org.goldenport.util

import cats.Eq
import org.goldenport.id.{Identifiable, Identified}
import org.goldenport.observation.{SpanId, TraceId}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scala.compiletime.testing.typeCheckErrors
import TypeSafeCollections.*

object TypeSafeCollectionsFixtures {
  final case class Entity(id: Option[TraceId], payload: String) derives CanEqual
  final case class RequiredEntity(id: TraceId) derives CanEqual
  val optional: Identifiable[Entity, TraceId] = new Identifiable[Entity, TraceId] {
    def id(entity: Entity): Option[TraceId] = entity.id
  }
  val required: Identified[RequiredEntity, TraceId] = new Identified[RequiredEntity, TraceId] {
    def id(entity: RequiredEntity): TraceId = entity.id
  }
}

final class TypeSafeCollectionsSpec extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckDrivenPropertyChecks {
  import TypeSafeCollectionsFixtures.*

  "Vector equality" should {
    "consume the supplied lawful equality rather than universal equality" in {
      Given("case-insensitive string equality")
      val equality: Eq[String] = Eq.instance((left, right) => left.equalsIgnoreCase(right))
      When("membership and the equality laws are checked")
      val values = Vector("Alpha").typeSafe
      Then("membership and reflexivity, symmetry and transitivity hold")
      val _ = values.containsEq("ALPHA")(using equality) shouldBe true
      forAll { (a: String, b: String, c: String) =>
        val _ = equality.eqv(a, a) shouldBe true
        val _ = equality.eqv(a, b) shouldBe equality.eqv(b, a)
        if (equality.eqv(a, b) && equality.eqv(b, c)) { val _ = equality.eqv(a, c) shouldBe true }
      }
      Vector.empty[String].typeSafe.containsEq("x")(using equality) shouldBe false
    }

    "use an explicitly structural equality for nested arrays" in {
      Given("a structural Eq for nested arrays")
      val equality: Eq[Array[Array[Int]]] = Eq.instance((left, right) =>
        left.length == right.length && left.indices.forall(i => left(i).sameElements(right(i))))
      When("distinct array objects have equal contents")
      val values = Vector(Array(Array(1, 2))).typeSafe
      Then("membership uses the supplied structure")
      val _ = values.containsEq(Array(Array(1, 2)))(using equality) shouldBe true
      values.containsEq(Array(Array(2, 1)))(using equality) shouldBe false
    }
  }

  "Vector identity" should {
    "keep the first present identity and every unidentified occurrence in order" in {
      Given("equivalent IDs, conflicting payloads and repeated unidentified references")
      val id = TraceId("major", "minor")
      val first = Entity(Some(id), "first")
      val conflict = Entity(Some(TraceId("major", "minor")), "conflict")
      val absent = Entity(None, "absent")
      val other = Entity(None, "other")
      val equality: Eq[TraceId] = Eq.instance((left, right) => left.major == right.major && left.minor == right.minor)
      val values = Vector(absent, first, conflict, absent, other).typeSafe.identifiableBy(optional)(using equality)
      When("looking up and removing duplicate identities")
      val distinct = values.distinctById
      Then("lookup returns the first representative and absent IDs never collapse")
      val _ = values.findById(TraceId("major", "minor")) shouldBe Some(first)
      val _ = values.containsId(id) shouldBe true
      val _ = distinct shouldBe Vector(absent, first, absent, other)
      val _ = Vector(absent).typeSafe.identifiableBy(optional)(using equality).findById(id) shouldBe None
      Vector.empty[Entity].typeSafe.identifiableBy(optional)(using equality).distinctById shouldBe Vector.empty[Entity]
    }

    "use the ID Eq for mandatory relations" in {
      Given("an Eq that regards all IDs of this type as equal")
      val equality: Eq[TraceId] = Eq.instance((_, _) => true)
      val first = RequiredEntity(TraceId("major", "minor"))
      When("the relation is selected explicitly")
      val values = Vector(first, RequiredEntity(TraceId("different", "minor"))).typeSafe.identifiedBy(required)(using equality)
      Then("lookup and deduplication use that Eq")
      val _ = values.containsId(TraceId("different", "minor")) shouldBe true
      val _ = values.findById(TraceId("different", "minor")) shouldBe Some(first)
      values.distinctById shouldBe Vector(first)
    }
  }

  "Captured types" should {
    "accept matching direct and captured calls with the same common-parent Eq instances" in {
      val errors = typeCheckErrors("""
        import org.goldenport.util.TypeSafeCollections.*
        import org.goldenport.util.TypeSafeCollectionsFixtures.*
        import org.goldenport.observation.*
        import org.goldenport.datatype.StringDataType.given
        import org.goldenport.datatype.IntDataType.given
        import org.goldenport.datatype.Identifier.given
        given cats.Eq[Any] = cats.Eq.instance[Any]((_, _) => true)
        given cats.Eq[Entity | String] = cats.Eq.instance[Entity | String]((_, _) => true)
        given cats.Eq[org.goldenport.id.UniversalId] = cats.Eq.by(_.value)
        given cats.Eq[Entity] = cats.Eq.instance((left, right) => left.payload == right.payload)
        given cats.Eq[TraceId] = cats.Eq.by(_.value)
        val values = Vector(Entity(None, "x")).typeSafe
        values.containsEq(Entity(None, "x"))
        Vector(Entity(None, "x")).typeSafe.containsEq(Entity(None, "x"))
        val widened: Vector[Any] = Vector(Entity(None, "x"))
        widened.typeSafe.containsEq[Any]("a valid Any")
        val ids = Vector(Entity(Some(TraceId("major", "minor")), "x")).typeSafe.identifiableBy(optional)
        ids.containsId(TraceId("major", "minor"))
        Vector(Entity(Some(TraceId("major", "minor")), "x")).typeSafe.identifiableBy(optional).containsId(TraceId("major", "minor"))
      """)
      errors shouldBe empty
    }

    "attribute mismatched element diagnostics to captured and inferred calls" in {
      val captured = typeCheckErrors("""
        import org.goldenport.util.TypeSafeCollections.*
        import org.goldenport.util.TypeSafeCollectionsFixtures.*
        import org.goldenport.observation.*
        import org.goldenport.datatype.StringDataType.given
        import org.goldenport.datatype.IntDataType.given
        import org.goldenport.datatype.Identifier.given
        given cats.Eq[Any] = cats.Eq.instance[Any]((_, _) => true)
        given cats.Eq[Entity | String] = cats.Eq.instance[Entity | String]((_, _) => true)
        given cats.Eq[org.goldenport.id.UniversalId] = cats.Eq.by(_.value)
        given cats.Eq[Entity] = cats.Eq.instance((left, right) => left.payload == right.payload)
        given cats.Eq[TraceId] = cats.Eq.by(_.value)
        val values = Vector(Entity(None, "x")).typeSafe
        values.containsEq("not an Entity")
      """)
      val inferred = typeCheckErrors("""
        import org.goldenport.util.TypeSafeCollections.*
        import org.goldenport.util.TypeSafeCollectionsFixtures.*
        import org.goldenport.observation.*
        import org.goldenport.datatype.StringDataType.given
        import org.goldenport.datatype.IntDataType.given
        import org.goldenport.datatype.Identifier.given
        given cats.Eq[Any] = cats.Eq.instance[Any]((_, _) => true)
        given cats.Eq[Entity | String] = cats.Eq.instance[Entity | String]((_, _) => true)
        given cats.Eq[org.goldenport.id.UniversalId] = cats.Eq.by(_.value)
        given cats.Eq[Entity] = cats.Eq.instance((left, right) => left.payload == right.payload)
        given cats.Eq[TraceId] = cats.Eq.by(_.value)
        Vector(Entity(None, "x")).typeSafe.containsEq("not an Entity")
      """)
      for ((label, errors) <- List("captured" -> captured, "inferred" -> inferred)) {
        info(s"element $label compiler diagnostics: $errors")
        val _ = errors should have size 1
        val error = errors.head
        val _ = error.lineContent should include ("containsEq(\"not an Entity\")")
        val _ = error.column should be >= error.lineContent.indexOf("containsEq")
        val _ = error.column should be <= error.lineContent.length
        val _ = error.message should include ("=:=" )
        val _ = error.message should include ("String")
        error.message should include ("Entity")
      }
    }

    "attribute sibling ID diagnostics to captured and inferred calls" in {
      val captured = typeCheckErrors("""
        import org.goldenport.util.TypeSafeCollections.*
        import org.goldenport.util.TypeSafeCollectionsFixtures.*
        import org.goldenport.observation.*
        import org.goldenport.datatype.StringDataType.given
        import org.goldenport.datatype.IntDataType.given
        import org.goldenport.datatype.Identifier.given
        given cats.Eq[Any] = cats.Eq.instance[Any]((_, _) => true)
        given cats.Eq[org.goldenport.id.UniversalId] = cats.Eq.by(_.value)
        given cats.Eq[Entity] = cats.Eq.instance((left, right) => left.payload == right.payload)
        given cats.Eq[TraceId] = cats.Eq.by(_.value)
        val ids = Vector(Entity(Some(TraceId("major", "minor")), "x")).typeSafe.identifiableBy(optional)
        ids.containsId(SpanId("major", "minor"))
      """)
      val inferred = typeCheckErrors("""
        import org.goldenport.util.TypeSafeCollections.*
        import org.goldenport.util.TypeSafeCollectionsFixtures.*
        import org.goldenport.observation.*
        import org.goldenport.datatype.StringDataType.given
        import org.goldenport.datatype.IntDataType.given
        import org.goldenport.datatype.Identifier.given
        given cats.Eq[Any] = cats.Eq.instance[Any]((_, _) => true)
        given cats.Eq[org.goldenport.id.UniversalId] = cats.Eq.by(_.value)
        given cats.Eq[Entity] = cats.Eq.instance((left, right) => left.payload == right.payload)
        given cats.Eq[TraceId] = cats.Eq.by(_.value)
        Vector(Entity(Some(TraceId("major", "minor")), "x")).typeSafe.identifiableBy(optional).containsId(SpanId("major", "minor"))
      """)
      for ((label, errors) <- List("captured" -> captured, "inferred" -> inferred)) {
        info(s"identity $label compiler diagnostics: $errors")
        val _ = errors should have size 1
        val error = errors.head
        val _ = error.lineContent should include ("containsId(SpanId(\"major\", \"minor\"))")
        val _ = error.column should be >= error.lineContent.indexOf("containsId")
        val _ = error.column should be < error.lineContent.length
        val _ = error.message should include ("Found:")
        val _ = error.message should include ("SpanId")
        val _ = error.message should include ("Required:")
        error.message should include ("TraceId")
      }
    }
  }
}
