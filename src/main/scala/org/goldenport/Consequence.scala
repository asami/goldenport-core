package org.goldenport

import cats._
import cats.data.NonEmptyVector
import scala.util.{Failure => TryFailure, Success => TrySuccess, Try}
import scala.util.control.NonFatal
import org.goldenport.consequence.Failures
import org.goldenport.consequence.SourcePositionMacro
import org.goldenport.text.Presentable
import org.goldenport.datatype.Identifier
import org.goldenport.record.Record
import org.goldenport.schema.DataType
import org.goldenport.schema.Constraint
import org.goldenport.convert.ValueReader
import org.goldenport.provisional.observation.Observation
import org.goldenport.provisional.observation.Taxonomy
import org.goldenport.provisional.observation.Cause
import org.goldenport.observation.Descriptor
import org.goldenport.observation.SourcePosition
import org.goldenport.http.HttpRequest
import org.goldenport.id.UniversalId

/*
 * @since   Feb. 21, 2021
 *  version May. 30, 2021
 *  version Jun. 20, 2021
 *  version Oct. 25, 2021
 *  version Nov. 30, 2021
 *  version Dec.  5, 2021
 *  version Jan. 30, 2022
 *  version Mar. 10, 2022
 *  version Apr.  3, 2022
 *  version May. 31, 2022
 *  version Jun. 14, 2022
 *  version Jul. 27, 2022
 *  version Aug.  3, 2022
 *  version Sep.  3, 2022
 *  version Oct. 31, 2022
 *  version Nov. 27, 2022
 *  version Dec. 31, 2022
 *  version Jan. 25, 2023
 *  version Jul. 23, 2023
 *  version Sep. 27, 2023
 *  version Mar.  9, 2025
 *  version Apr. 21, 2025
 *  version May. 16, 2025
 *  version Jun. 15, 2025
 *  version Jul. 27, 2025
 *  version Sep. 30, 2025
 *  version Nov. 11, 2025
 *  version Dec. 26, 2025
 *  version Jan.  3, 2026
 *  version Jan. 31, 2026
 *  version Feb. 28, 2026
 *  version Mar. 13, 2026
 * @version Apr. 25, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait Consequence[+T] extends Presentable {
  def isSuccess: Boolean = this match {
    case Consequence.Success(_) => true
    case Consequence.Failure(_) => false
  }

  def isFaillure: Boolean = !isSuccess

  def get: Option[T]

  def TAKE: T

  @deprecated("Use TAKE instead.")
  def take: T = TAKE

  def RAISE: Nothing = this match {
    case Consequence.Failure(conclusion) =>
      throw conclusion.getException.getOrElse(
        new ConsequenceException(this)
      )
    case Consequence.Success(_) =>
      throw new IllegalStateException("RAISE called on Success")
  }

  def RAISEC: Nothing =
    throw new ConsequenceException(this)

  def zip[U](that: Consequence[U]): Consequence[(T, U)] = {
    (this, that) match {
      case (Consequence.Success(a), Consequence.Success(b)) =>
        Consequence.Success((a, b))
      case (Consequence.Failure(c1), Consequence.Failure(c2)) =>
        Consequence.Failure(c1 ++ c2)
      case (Consequence.Failure(c), _) =>
        Consequence.Failure(c)
      case (_, Consequence.Failure(c)) =>
        Consequence.Failure(c)
    }
  }

  def zipWith[U, V](that: Consequence[U])(f: (T, U) => V): Consequence[V] =
    this.zip(that).map { case (a, b) => f(a, b) }

  def zip3With[U, V, W](
    that1: Consequence[U],
    that2: Consequence[V]
  )(f: (T, U, V) => W): Consequence[W] =
    this.zip(that1).zip(that2).map {
      case ((a, b), c) => f(a, b, c)
    }

  def map[U](f: T => U): Consequence[U] = {
    this match {
      case Consequence.Success(a) => Consequence.Success(f(a))
      case e @ Consequence.Failure(_) =>
        e.asInstanceOf[Consequence[U]]
    }
  }

  def flatMap[U](f: T => Consequence[U]): Consequence[U] =
    this match {
      case Consequence.Success(a) => f(a)
      case e @ Consequence.Failure(_) =>
        e.asInstanceOf[Consequence[U]]
    }

  def leftMap(f: Conclusion => Conclusion): Consequence[T]

  def transform[U](
    s: T => Consequence[U],
    c: Conclusion => Consequence[U]
  ): Consequence[U]

  def recoverWith[U >: T](f: Conclusion => Consequence[U]): Consequence[U]

  def recover[U >: T](f: Conclusion => U): Consequence[U]

  def recoverConclusion(f: Conclusion => Conclusion): Consequence[T]

  def fold[U >: T](c: Conclusion => U, s: T => U): U

  def foldIdntity[U >: T](c: Conclusion => U): U

  def getOrElse[U >: T](body: => U): U

  def orElse[U >: T](body: => Consequence[U]): Consequence[U]

  def toOption: Option[T]

  def collapseOption[U](using ev: T <:< Option[U]): Option[U] = toOption.flatten

  override def print: String = this match {
    case Consequence.Success(v) => Presentable.print(v)
    case Consequence.Failure(c) => c.print
  }

  override def display: String = this match {
    case Consequence.Success(_) => "success"
    case Consequence.Failure(c) => c.display
  }

  override def show: String = this match {
    case Consequence.Success(v) => s"Success(${Presentable.show(v)})"
    case Consequence.Failure(c) => s"Failure(${c.show})"
  }
}

object Consequence {
  val unit: Consequence[Unit] = Consequence(())
  def none[T]: Consequence[Option[T]] = Consequence(None)

  case class Success[+T](
    result: T
  ) extends Consequence[T] {
    def get: Option[T] = Some(result)

    def TAKE = result

    def transform[U](
      s: T => Consequence[U],
      c: Conclusion => Consequence[U]
    ): Consequence[U] = s(result)

    def leftMap(f: Conclusion => Conclusion): Consequence[T] = this

    def recoverWith[U >: T](f: Conclusion => Consequence[U]): Consequence[U] = this

    def recover[U >: T](f: Conclusion => U): Consequence[U] = this

    def recoverConclusion(f: Conclusion => Conclusion): Consequence[T] = this

    def fold[U >: T](c: Conclusion => U, s: T => U): U = s(result)

    def foldIdntity[U >: T](c: Conclusion => U): U = result

    def getOrElse[U >: T](body: => U): U = result

    def orElse[U >: T](body: => Consequence[U]): Consequence[U] = this

    def toOption = Some(result)
  }

  case class Failure[+T](conclusion: Conclusion) extends Consequence[T] {
    def get: Option[T] = None

    def TAKE = RAISEC

    def transform[U](
      s: T => Consequence[U],
      c: Conclusion => Consequence[U]
    ): Consequence[U] = Consequence.run(c(conclusion))

    def leftMap(f: Conclusion => Conclusion): Consequence[T] =
      Consequence.Failure(f(conclusion))

    def recoverWith[U >: T](f: Conclusion => Consequence[U]): Consequence[U] =
      Consequence.run(f(conclusion))

    def recover[U >: T](f: Conclusion => U): Consequence[U] =
      Consequence(f(conclusion))

    def recoverConclusion(f: Conclusion => Conclusion): Consequence[T] =
      Consequence.run(Consequence.Failure(f(conclusion)))

    def fold[U >: T](c: Conclusion => U, s: T => U): U = c(conclusion)

    def foldIdntity[U >: T](c: Conclusion => U): U = c(conclusion)

    def getOrElse[U >: T](body: => U): U = body

    def orElse[U >: T](body: => Consequence[U]): Consequence[U] = body

    def toOption = None
  }

  sealed trait ValidatedValue[+T] {
    def value: T
  }

  object ValidatedValue {
    final case class Success[+T](a: T) extends ValidatedValue[T] {
      def value: T = a
    }

    case object Failure extends ValidatedValue[Nothing] {
      def value: Nothing =
        throw new IllegalStateException(
          "Consequence.ValidatedValue.value is only valid inside a successful ValidatedBuilder.build body"
        )
    }
  }

  final class ValidatedBuilder private[Consequence] () {
    private val _failures = scala.collection.mutable.ArrayBuffer.empty[Conclusion]

    def read[T](p: => Consequence[T]): ValidatedValue[T] =
      Consequence.run(p) match {
        case Success(a) => ValidatedValue.Success(a)
        case Failure(c) =>
          _failures += c
          ValidatedValue.Failure
      }

    def build[T](body: => T): Consequence[T] =
      _conclusion match {
        case Some(c) => Failure(c)
        case None => Consequence(body)
      }

    private def _conclusion: Option[Conclusion] =
      if (_failures.isEmpty)
        None
      else
        Some(_failures.reduceLeft(_ ++ _))
  }

  object ValidatedBuilder {
    def run[T](body: ValidatedBuilder => Consequence[T]): Consequence[T] = {
      val builder = new ValidatedBuilder()
      Consequence.run(body(builder))
    }
  }

  implicit object ConsequenceMonad extends Monad[Consequence] {
    override def pure[A](x: A): Consequence[A] =
      Success(x)

    override def flatMap[A, B](fa: Consequence[A])(f: A => Consequence[B]): Consequence[B] =
      fa match {
        case Success(result) => f(result)
        case e @ Failure(_)       => e.asInstanceOf[Consequence[B]]
      }

    override def tailRecM[A, B](a: A)(f: A => Consequence[Either[A, B]]): Consequence[B] =
      f(a) match {
        case Success(Right(b)) => Success(b)
        case Success(Left(nextA)) => tailRecM(nextA)(f)
        case e @ Failure(_) => e.asInstanceOf[Consequence[B]]
      }
  }

  def apply[T](body: => T): Consequence[T] = try {
    success(body)
  } catch {
    case NonFatal(e) => Failure(Conclusion.from(e))
  }

  def run[T](body: => Consequence[T]): Consequence[T] = try {
    body
  } catch {
    case NonFatal(e) => Failure(Conclusion.from(e))
  }

  private def _using[R, T](
    acquire: Consequence[R],
    use: R => Consequence[T],
    release: R => Consequence[Unit]
  ): Consequence[T] = {
    acquire.flatMap { r =>
      val result: Consequence[T] =
        try {
          use(r)
        } catch {
          case NonFatal(e) =>
            Failure(Conclusion.from(e))
        }
      result.transform(
        s => {
          // success path: always release (best-effort)
          try {
            release(r)
          } catch {
            case NonFatal(_) => ()
          }
          Success(s)
        },
        c => {
          // failure path: always release (best-effort)
          try {
            release(r)
          } catch {
            case NonFatal(_) => ()
          }
          Failure(c)
        }
      )
    }
  }

  /**
    * NOTE:
    * `using` here is a Consequence-level resource lifecycle combinator,
    * unrelated to Scala 3 context-parameter `using` syntax.
    * If Scala language semantics change in the future,
    * this name can be revised without affecting the underlying model.
    */
  def using[R, T](
    acquire: Consequence[R]
  )(
    use: R => Consequence[T]
  )(
    release: R => Consequence[Unit]
  ): Consequence[T] = _using(acquire, use, release)

  def using[R <: AutoCloseable, T](
    resource: Consequence[R]
  )(
    use: R => Consequence[T]
  ): Consequence[T] =
    _using(resource, use, r =>
      Consequence {
        try {
          r.close()
        } catch {
          case NonFatal(_) => ()
        }
      }
    )

  /**
    * NOTE:
    * This overload assumes that resource acquisition has already succeeded.
    * Prefer the Consequence[R] variant when acquisition can fail
    * or needs to produce observations.
    */
  def using[R <: AutoCloseable, T](
    resource: R
  )(
    use: R => Consequence[T]
  ): Consequence[T] =
    using(Consequence.success(resource))(use)

  def success[T](p: T): Consequence[T] = Success(p)

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failure[T](message: String): Consequence[T] =
    Failure(Conclusion.simple(message))

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failure[T](e: Throwable): Consequence[T] =
    Failure(Conclusion.from(e))

  def fromOption[A](
    opt: Option[A],
    onNone: => String
  ): Consequence[A] =
    opt match {
      case Some(v) => Success(v)
      case None => Failure(Conclusion.simple(onNone))
    }

  def fromTry[T](p: Try[T]): Consequence[T] = p match {
    case TrySuccess(s) => success(s)
    case TryFailure(e) => Failure(Conclusion.from(e))
  }

  def toInt(p: String): Consequence[Int] = Consequence(p.toInt)

  def zip3[A, B, C](
    ca: Consequence[A],
    cb: Consequence[B],
    cc: Consequence[C]
  ): Consequence[(A, B, C)] = {
    ca.zip(cb).zip(cc).map {
      case ((a, b), c) => (a, b, c)
    }
  }

  def map2[A, B, C](
    ca: Consequence[A],
    cb: Consequence[B]
  )(f: (A, B) => C): Consequence[C] =
    ca.zipWith(cb)(f)

  def map3[A, B, C, D](
    ca: Consequence[A],
    cb: Consequence[B],
    cc: Consequence[C]
  )(f: (A, B, C) => D): Consequence[D] =
    ca.zip3With(cb, cc)(f)

  def map4[A, B, C, D, E](
    ca: Consequence[A],
    cb: Consequence[B],
    cc: Consequence[C],
    cd: Consequence[D]
  )(f: (A, B, C, D) => E): Consequence[E] =
    ca.zip(cb).zip(cc).zip(cd).map {
      case (((a, b), c), d) => f(a, b, c, d)
    }

  def map5[A, B, C, D, E, F](
    ca: Consequence[A],
    cb: Consequence[B],
    cc: Consequence[C],
    cd: Consequence[D],
    ce: Consequence[E]
  )(f: (A, B, C, D, E) => F): Consequence[F] =
    ca.zip(cb).zip(cc).zip(cd).zip(ce).map {
      case ((((a, b), c), d), e) => f(a, b, c, d, e)
    }

  def map6[A, B, C, D, E, F, G](
    ca: Consequence[A],
    cb: Consequence[B],
    cc: Consequence[C],
    cd: Consequence[D],
    ce: Consequence[E],
    cf: Consequence[F]
  )(f: (A, B, C, D, E, F) => G): Consequence[G] =
    ca.zip(cb).zip(cc).zip(cd).zip(ce).zip(cf).map {
      case (((((a, b), c), d), e), f0) => f(a, b, c, d, e, f0)
    }

  def map7[A, B, C, D, E, F, G, H](
    ca: Consequence[A],
    cb: Consequence[B],
    cc: Consequence[C],
    cd: Consequence[D],
    ce: Consequence[E],
    cf: Consequence[F],
    cg: Consequence[G]
  )(f: (A, B, C, D, E, F, G) => H): Consequence[H] =
    ca.zip(cb).zip(cc).zip(cd).zip(ce).zip(cf).zip(cg).map {
      case ((((((a, b), c), d), e), f0), g) => f(a, b, c, d, e, f0, g)
    }

  def map8[A, B, C, D, E, F, G, H, I](
    ca: Consequence[A],
    cb: Consequence[B],
    cc: Consequence[C],
    cd: Consequence[D],
    ce: Consequence[E],
    cf: Consequence[F],
    cg: Consequence[G],
    ch: Consequence[H]
  )(f: (A, B, C, D, E, F, G, H) => I): Consequence[I] =
    ca.zip(cb).zip(cc).zip(cd).zip(ce).zip(cf).zip(cg).zip(ch).map {
      case (((((((a, b), c), d), e), f0), g), h) => f(a, b, c, d, e, f0, g, h)
    }

  def map9[A, B, C, D, E, F, G, H, I, J](
    ca: Consequence[A],
    cb: Consequence[B],
    cc: Consequence[C],
    cd: Consequence[D],
    ce: Consequence[E],
    cf: Consequence[F],
    cg: Consequence[G],
    ch: Consequence[H],
    ci: Consequence[I]
  )(f: (A, B, C, D, E, F, G, H, I) => J): Consequence[J] =
    ca.zip(cb).zip(cc).zip(cd).zip(ce).zip(cf).zip(cg).zip(ch).zip(ci).map {
      case ((((((((a, b), c), d), e), f0), g), h), i) => f(a, b, c, d, e, f0, g, h, i)
    }

  def map10[A, B, C, D, E, F, G, H, I, J, K](
    ca: Consequence[A],
    cb: Consequence[B],
    cc: Consequence[C],
    cd: Consequence[D],
    ce: Consequence[E],
    cf: Consequence[F],
    cg: Consequence[G],
    ch: Consequence[H],
    ci: Consequence[I],
    cj: Consequence[J]
  )(f: (A, B, C, D, E, F, G, H, I, J) => K): Consequence[K] =
    ca.zip(cb).zip(cc).zip(cd).zip(ce).zip(cf).zip(cg).zip(ch).zip(ci).zip(cj).map {
      case (((((((((a, b), c), d), e), f0), g), h), i), j) =>
        f(a, b, c, d, e, f0, g, h, i, j)
    }

  def zipN[A](xs: Seq[Consequence[A]]): Consequence[Seq[A]] = {
    val results = scala.collection.mutable.ArrayBuffer[A]()
    var failure: Option[Conclusion] = None

    xs.foreach {
      case Success(a) =>
        results += a
      case Failure(c) =>
        failure = failure match {
          case Some(acc) => Some(acc ++ c)
          case None      => Some(c)
        }
    }

    failure match {
      case Some(c) => Failure(c)
      case None    => Success(results.toSeq)
    }
  }

  // FailureBuilder API for Observation/Descriptor-based failures
  // def failArgumentMissing: FailureBuilder =
  //   FailureBuilder(org.goldenport.observation.Cause.Argument(
  //     org.goldenport.observation.Cause.Reason.Missing
  //   ))

  // def failArgumentRedundant: FailureBuilder =
  //   FailureBuilder(org.goldenport.observation.Cause.Argument(
  //     org.goldenport.observation.Cause.Reason.Redundant
  //   ))

  // def failArgumentValidationError: FailureBuilder =
  //   FailureBuilder(org.goldenport.observation.Cause.Argument(
  //     org.goldenport.observation.Cause.Reason.ValidationError
  //   ))
//  def failArgumentMissing: FailureBuilder = ???
//  def failArgumentRedundant: FailureBuilder = ???
//  def failArgumentValidationError: FailureBuilder = ???

  final case class FailureBuilder(
    cause: Cause,
    descriptor: org.goldenport.observation.Descriptor =
      org.goldenport.observation.Descriptor()
  ) {
    def withOperation(name: String): FailureBuilder =
      copy(descriptor = _add_facet(org.goldenport.observation.Descriptor.Facet.Operation(name)))

    def withInput(name: String, value: Option[String] = None): FailureBuilder =
      copy(descriptor = _add_facet(
        org.goldenport.observation.Descriptor.Facet.Input(
          name = Some(name),
          value = value
        )
      ))

    private def _add_facet(
      facet: org.goldenport.observation.Descriptor.Facet
    ): org.goldenport.observation.Descriptor =
      descriptor.copy(facets = descriptor.facets :+ facet)

    // def build[A]: Consequence[A] = {
    //   val base = Conclusion.simple("validation error")
    //   val observation = base.observation.copy(
    //     cause = Some(cause),
    //     descriptor = descriptor
    //   )
    //   Failure(base.copy(observation = observation))
    // }
    def build[A]: Consequence[A] = create("validation error", cause, descriptor)
  }

  // Test migration
  def create[A](message: String, cause: Cause): Consequence[A] = ???
  def create[A](message: String, cause: Cause, descriptor: org.goldenport.observation.Descriptor): Consequence[A] = ???

  // def createFormatError[A](message: String): Consequence[A] = {
  //   ???
  // }

  // Success or Fail

  def successOrFail[T](p: Option[T])(observation: Observation): Consequence[T] =
    p match {
      case Some(s) => Consequence.success(s)
      case None => Failures.fail(observation)
    }

  def successOrFail[T](p: Option[T])(taxonomy: Taxonomy): Consequence[T] =
    p match {
      case Some(s) => Consequence.success(s)
      case None => Failures.fail(taxonomy)
    }

  inline def successOrPropertyNotFound[T](key: String, v: Option[T]): Consequence[T] =
    successOrPropertyNotFound(key, v, SourcePositionMacro.position())

  def successOrPropertyNotFound[T](key: String, v: Option[T], pos: SourcePosition): Consequence[T] =
    v match {
      case Some(s) => Consequence.success(s)
      case None => Failures.fail(Observation.propertyNotFound(pos, key), pos)
    }

  inline def successOrPropertyNotFound[T](
    key: String,
    v: Option[T],
    fallback: Option[T]
  ): Consequence[T] =
    successOrPropertyNotFound(key, v, fallback, SourcePositionMacro.position())

  def successOrPropertyNotFound[T](
    key: String,
    v: Option[T],
    fallback: Option[T],
    pos: SourcePosition
  ): Consequence[T] =
    v match {
      case Some(s) => Consequence.success(s)
      case None => fallback match {
        case Some(ss) => Consequence.success(ss)
        case None => Failures.fail(Observation.propertyNotFound(pos, key), pos)
      }
    }

  inline def successOrRecordNotFound[T](key: String, rec: Record)(using reader: ValueReader[T]): Consequence[T] =
    successOrRecordNotFound(key, rec, SourcePositionMacro.position())

  def successOrRecordNotFound[T](key: String, rec: Record, pos: SourcePosition)(using reader: ValueReader[T]): Consequence[T] =
    rec.getAsC[T](key) match {
      case Consequence.Success(s) => s match {
        case Some(ss) => Consequence.success(ss)
        case None => Failures.fail(Observation.recordNotFound(pos, key, rec), pos)
      }
      case Consequence.Failure(c) => Failures.fail(c, pos)
    }

  inline def successOrRecordNotFound[T](key: String, rec: Record, fallback: Option[T])(using reader: ValueReader[T]): Consequence[T] =
    successOrRecordNotFound(key, rec, fallback, SourcePositionMacro.position())

  def successOrRecordNotFound[T](
    key: String,
    rec: Record,
    fallback: Option[T],
    pos: SourcePosition
  )(using reader: ValueReader[T]): Consequence[T] =
    rec.getAsC[T](key) match {
      case Consequence.Success(s) => s match {
        case Some(ss) => Consequence.success(ss)
        case None => fallback match {
          case Some(sss) => Consequence.success(sss)
          case None => Failures.fail(Observation.recordNotFound(pos, key, rec), pos)
        }
      }
      case Consequence.Failure(c) => Failures.fail(c, pos)
    }

  inline def successOrResourceNotFound[T](
    rsc: Option[T]
  )(resource: Descriptor.Facet.Resource, facets: Descriptor.Facet*): Consequence[T] =
    rsc match {
      case Some(s) => Consequence.success(s)
      case None => Failures.fail(Conclusion.resourceNotFound(resource, facets))
    }

  inline def successOrServiceProviderNotFound[T](
    serviceprovider: Option[T]
  )(name: String, facets: Descriptor.Facet*): Consequence[T] =
    serviceprovider match {
      case Some(s) => Consequence.success(s)
      case None => Failures.fail(Conclusion.serviceProviderNotFound(name, facets))
    }

  inline def successOrServiceProviderByKeyNotFound[T](
    serviceprovider: Option[T]
  )(name: String, key: String): Consequence[T] =
    serviceprovider match {
      case Some(s) => Consequence.success(s)
      case None => Failures.fail(Conclusion.serviceProviderNotFound(name, Vector(Descriptor.Facet.Key(key))))
    }

  inline def successOrEntityNotFound[T](
    entity: Option[T]
  )(id: Identifier): Consequence[T] =
    entity match {
      case Some(s) => Consequence.success(s)
      case None => Failures.fail(Conclusion.entityNotFound(id))
    }

  // Low-level structured failure builder.
  // Use this when a semantic utility does not exist yet and the caller needs to
  // assemble a structured taxonomy/cause/facet failure explicitly. Add a
  // semantic utility later when the same failure shape becomes common.
  inline def fail(o: Observation): Consequence.Failure[Nothing] =
    Failures.fail(o)

  def fail(o: Observation, pos: SourcePosition): Consequence.Failure[Nothing] =
    Failures.fail(o, pos)

  inline def fail(taxonomy: Taxonomy): Consequence.Failure[Nothing] =
    Failures.fail(taxonomy)

  def fail(taxonomy: Taxonomy, message: String): Consequence.Failure[Nothing] =
    Failures.fail(taxonomy, Cause.message(message))

  def fail(taxonomy: Taxonomy, message: String, facets: Seq[Descriptor.Facet]): Consequence.Failure[Nothing] =
    Failures.fail(taxonomy, Descriptor.Facet.Message(message) +: facets)

  def fail(taxonomy: Taxonomy, facet: Descriptor.Facet, facets: Descriptor.Facet*): Consequence.Failure[Nothing] =
    Failures.fail(taxonomy, facet +: facets)

  def fail(taxonomy: Taxonomy, e: Throwable, facets: Seq[Descriptor.Facet] = Nil): Consequence.Failure[Nothing] =
    Failures.fail(taxonomy, Descriptor.Facet.Exception(e) +: facets)

  def fail(taxonomy: Taxonomy, cause: Cause): Consequence.Failure[Nothing] =
    Failures.fail(taxonomy, cause)

  inline def fail(c: Conclusion): Consequence.Failure[Nothing] =
    Failures.fail(c)

  def fail(c: Conclusion, pos: SourcePosition): Consequence.Failure[Nothing] =
    Failures.fail(c, pos)

  // Utilities
  def notImplemented: Consequence.Failure[Nothing] =
    Failures.notImplemented

  def notImplemented(msg: String): Consequence.Failure[Nothing] =
    Consequence.Failure(Conclusion.notImplemented(msg))

  def notImplemented(pos: SourcePosition): Consequence.Failure[Nothing] =
    Consequence.Failure(Conclusion.notImplemented(pos))

  def notImplemented(pos: SourcePosition, msg: String): Consequence.Failure[Nothing] =
    Consequence.Failure(Conclusion.notImplemented(pos, msg))

  inline def argumentMissing[A](name: String): Consequence.Failure[A] =
    argumentMissing(name, SourcePositionMacro.position())

  def argumentMissing[A](name: String, pos: SourcePosition): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.argumentMissing(name).withSourcePosition(pos))

  def argumentMissing[A]: Consequence.Failure[A] =
    Consequence.Failure(Conclusion.argumentMissing)

  def argumentMissingInput[A](name: String): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.argumentMissingInput(name))

  def argumentMissingInput[A](args: Seq[String]): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.argumentMissingInput(args))

  def argumentMissingInput[A](req: HttpRequest): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.argumentMissingInput(req))

  def argumentMissingOperation[A](name: String, operation: String): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.argumentMissingOperation(name, operation))

  def argumentRedundantOperation[A](name: String, operation: String): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.argumentRedundantOperation(name, operation))

  def argumentRedundantOperationInput[A](operation: String, args: Seq[String]): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.argumentRedundantOperationInput(operation, args))

  def argumentDataType[A](name: String, value: Any, dt: DataType): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.argumentDataType(name, value, dt))

  def argumentConstraint[A](name: String, value: Any, constraints: NonEmptyVector[Constraint]): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.argumentConstraint(name, value, constraints))

  inline def argumentInvalid[A](message: String): Consequence.Failure[A] =
    argumentInvalid(message, SourcePositionMacro.position())

  def argumentInvalid[A](message: String, pos: SourcePosition): Consequence.Failure[A] =
    Failures.fail(Taxonomy.argumentInvalid, Cause.message(message), pos)

  inline def operationNotFound[A](name: String): Consequence.Failure[A] =
    operationNotFound(name, SourcePositionMacro.position())

  def operationNotFound[A](name: String, pos: SourcePosition): Consequence.Failure[A] =
    Failures.operationNotFound(name, pos)

  inline def operationNotFound[A](
    name: String,
    facets: Seq[Descriptor.Facet]
  ): Consequence.Failure[A] =
    operationNotFound(name, facets, SourcePositionMacro.position())

  def operationNotFound[A](
    name: String,
    facets: Seq[Descriptor.Facet],
    pos: SourcePosition
  ): Consequence.Failure[A] =
    Failures.operationNotFound(name, facets, pos)

  inline def operationConflict[A](
    name: String,
    facets: Seq[Descriptor.Facet]
  ): Consequence.Failure[A] =
    operationConflict(name, facets, SourcePositionMacro.position())

  def operationConflict[A](
    name: String,
    facets: Seq[Descriptor.Facet],
    pos: SourcePosition
  ): Consequence.Failure[A] =
    Failures.operationConflict(name, facets, pos)

  inline def operationIllegal[A](
    name: String,
    facets: Seq[Descriptor.Facet]
  ): Consequence.Failure[A] =
    operationIllegal(name, facets, SourcePositionMacro.position())

  inline def operationIllegal[A](
    name: String,
    message: String
  ): Consequence.Failure[A] =
    operationIllegal(name, Seq(Descriptor.Facet.Message(message)), SourcePositionMacro.position())

  def operationIllegal[A](
    name: String,
    facets: Seq[Descriptor.Facet],
    pos: SourcePosition
  ): Consequence.Failure[A] =
    Failures.operationIllegal(name, facets, pos)

  inline def operationInvalid[A](name: String): Consequence.Failure[A] =
    operationInvalid(name, SourcePositionMacro.position())

  def operationInvalid[A](name: String, pos: SourcePosition): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.operationInvalid(name).withSourcePosition(pos))

  inline def operationInvalid[A](
    name: String,
    facets: Seq[Descriptor.Facet]
  ): Consequence.Failure[A] =
    operationInvalid(name, facets, SourcePositionMacro.position())

  inline def operationInvalid[A](
    name: String,
    message: String
  ): Consequence.Failure[A] =
    operationInvalid(name, Seq(Descriptor.Facet.Message(message)), SourcePositionMacro.position())

  def operationInvalid[A](
    name: String,
    facets: Seq[Descriptor.Facet],
    pos: SourcePosition
  ): Consequence.Failure[A] =
    Failures.operationInvalid(name, facets, pos)

  inline def valueInvalid[A](message: String): Consequence.Failure[A] =
    valueInvalid(message, SourcePositionMacro.position())

  def valueInvalid[A](message: String, pos: SourcePosition): Consequence.Failure[A] =
    Failures.valueInvalid(message, pos)

  def valueInvalid[A](value: Any, dt: DataType): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.valueInvalid(value, dt))

  inline def valueFormatError[A](message: String): Consequence.Failure[A] =
    valueFormatError(message, SourcePositionMacro.position())

  def valueFormatError[A](message: String, pos: SourcePosition): Consequence.Failure[A] =
    Failures.valueFormatError(message, pos)

  def valueFormatError[A](value: Any, dt: DataType): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.valueFormatError(value, dt))

  inline def configurationInvalid[A](message: String): Consequence.Failure[A] =
    configurationInvalid(message, SourcePositionMacro.position())

  def configurationInvalid[A](message: String, pos: SourcePosition): Consequence.Failure[A] =
    Failures.configurationInvalid(message, pos)

  inline def resourceUnsupported[A](message: String): Consequence.Failure[A] =
    resourceUnsupported(message, SourcePositionMacro.position())

  def resourceUnsupported[A](message: String, pos: SourcePosition): Consequence.Failure[A] =
    Failures.resourceUnsupported(message, pos)

  inline def protocolInvalid[A](message: String): Consequence.Failure[A] =
    protocolInvalid(message, SourcePositionMacro.position())

  def protocolInvalid[A](message: String, pos: SourcePosition): Consequence.Failure[A] =
    Failures.protocolInvalid(message, pos)

  inline def componentInvalid[A](
    facets: Seq[Descriptor.Facet]
  ): Consequence.Failure[A] =
    componentInvalid(facets, SourcePositionMacro.position())

  inline def componentInvalid[A](
    message: String
  ): Consequence.Failure[A] =
    componentInvalid(Seq(Descriptor.Facet.Message(message)), SourcePositionMacro.position())

  inline def componentInvalid[A](
    e: Throwable
  ): Consequence.Failure[A] =
    componentInvalid(Seq(Descriptor.Facet.Exception(e)), SourcePositionMacro.position())

  def componentInvalid[A](
    facets: Seq[Descriptor.Facet],
    pos: SourcePosition
  ): Consequence.Failure[A] =
    Failures.componentInvalid(facets, pos)

  inline def componentNotFound[A](name: String): Consequence.Failure[A] =
    componentNotFound(name, SourcePositionMacro.position())

  def componentNotFound[A](
    name: String,
    pos: SourcePosition
  ): Consequence.Failure[A] =
    Failures.componentNotFound(name, pos)

  inline def serviceNotFound[A](name: String): Consequence.Failure[A] =
    serviceNotFound(name, SourcePositionMacro.position())

  def serviceNotFound[A](
    name: String,
    pos: SourcePosition
  ): Consequence.Failure[A] =
    Failures.serviceNotFound(name, pos)

  inline def stateConflict[A](message: String): Consequence.Failure[A] =
    stateConflict(message, SourcePositionMacro.position())

  def stateConflict[A](message: String, pos: SourcePosition): Consequence.Failure[A] =
    Failures.fail(Taxonomy.stateConflict, Cause.message(message), pos)

  inline def serviceUnavailable[A](message: String): Consequence.Failure[A] =
    serviceUnavailable(message, SourcePositionMacro.position())

  def serviceUnavailable[A](message: String, pos: SourcePosition): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.serviceUnavailable(pos, Taxonomy.serviceUnavailable, Cause.message(message)))

  inline def resourceNotFound[A](message: String): Consequence.Failure[A] =
    resourceNotFound(message, SourcePositionMacro.position())

  def resourceNotFound[A](message: String, pos: SourcePosition): Consequence.Failure[A] =
    Failures.fail(Taxonomy.resourceNotFound, Cause.message(message), pos)

  inline def resourceInvalid[A](message: String): Consequence.Failure[A] =
    resourceInvalid(message, SourcePositionMacro.position())

  def resourceInvalid[A](message: String, pos: SourcePosition): Consequence.Failure[A] =
    Failures.fail(Taxonomy.resourceInvalid, Cause.message(message), pos)

  inline def dataStoreUnavailable[A](message: String): Consequence.Failure[A] =
    dataStoreUnavailable(message, SourcePositionMacro.position())

  def dataStoreUnavailable[A](message: String, pos: SourcePosition): Consequence.Failure[A] =
    Failures.fail(Taxonomy.dataStoreUnavailable, Cause.message(message), pos)

  inline def entityNotFound[A](message: String): Consequence.Failure[A] =
    entityNotFound(message, SourcePositionMacro.position())

  def entityNotFound[A](message: String, pos: SourcePosition): Consequence.Failure[A] =
    Failures.fail(Taxonomy.entityNotFound, Cause.message(message), pos)

  inline def recordNotFound(key: String, rec: Record): Consequence.Failure[Nothing] =
    Failures.recordNotFound(key, rec)

  def unreachableReached: Consequence.Failure[Nothing] =
    Failures.unreachableReached

  def unreachableReached(msg: String): Consequence.Failure[Nothing] =
    Failures.fail(Conclusion.unreachableReached(msg))

  def uninitializedState[A]: Consequence.Failure[A] =
    Failures.uninitializedState

  def uninitializedState[A](conclusion: Conclusion): Consequence.Failure[A] =
    Failures.uninitializedState(conclusion)

  // Deprecated compatibility aliases for the old fail* naming style.
  // New code should call semantic utilities such as argumentMissing,
  // operationInvalid, valueInvalid, recordNotFound, or notImplemented. Keep
  // these aliases only for source compatibility during migration.

  // def failArgumentEmpty[A]: Consequence.Failure[A] =
  //   Consequence.Failure(Conclusion.failArgumentEmpty)

  // obsolated. use argumentFormatError
  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failArgumentFormatError[A](name: String, value: Any, dt: DataType): Consequence.Failure[A] = ???

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failArgumentFormatError[A](name: String, value: Any, msg: String): Consequence.Failure[A] = ???

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failArgumentFormatError[A](name: String, value: Option[Any], msg: Option[String]): Consequence.Failure[A] = ???

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failArgumentMissing[A]: Consequence.Failure[A] =
    Consequence.Failure(Conclusion.argumentMissing)

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failArgumentMissing[A](name: String): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.argumentMissing(name))

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failArgumentMissingInput[A](name: String): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.argumentMissingInput(name))

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failArgumentMissingInput[A](args: Seq[String]): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.argumentMissingInput(args))

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failArgumentMissingInput[A](req: HttpRequest): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.argumentMissingInput(req))

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failArgumentMissingOperation[A](name: String, operation: String): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.argumentMissingOperation(name, operation))

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failArgumentRedundantOperation[A](name: String, operation: String): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.argumentRedundantOperation(name, operation))

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failArgumentRedundantOperationInput[A](operation: String, args: Seq[String]): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.argumentRedundantOperationInput(operation, args))

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failArgumentDataType[A](name: String, value: Any, dt: DataType): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.argumentDataType(name, value, dt))

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failArgumentConstraint[A](name: String, value: Any, constraints: NonEmptyVector[Constraint]): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.argumentConstraint(name, value, constraints))

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failArgumentMultipleValues[A](name: String): Consequence.Failure[A] = ???

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  inline def failOperationNotFound[A](name: String): Consequence[A] =
    Failures.operationNotFound(name)

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failOperationInvalid[A](name: String): Consequence[A] =
    Consequence.Failure(Conclusion.operationInvalid(name))

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failResourceInconsistency: Consequence.Failure[Nothing] =
    Failures.resourceInconsistency

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  inline def failRecordNotFound(key: String, rec: Record): Consequence.Failure[Nothing] =
    Failures.recordNotFound(key, rec)

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failValueInvalid[A](value: Any, dt: DataType): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.valueInvalid(value, dt))

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failValueFormatError[A](value: Any, dt: DataType): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.valueFormatError(value, dt))

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failNetworkUnavailable(e: Throwable, facet: Descriptor.Facet, facets: Descriptor.Facet*): Consequence.Failure[Nothing] =
    Failures.fail(Taxonomy.networkUnavailable, Descriptor.Facet.Exception(e) +: (facet +: facets))

  inline def securityAuthenticationRequired[A](message: String): Consequence.Failure[A] =
    securityAuthenticationRequired(message, SourcePositionMacro.position())

  def securityAuthenticationRequired[A](message: String, pos: SourcePosition): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.securityAuthenticationRequired(message).withSourcePosition(pos))

  inline def securityPermissionDenied[A](message: String): Consequence.Failure[A] =
    securityPermissionDenied(message, SourcePositionMacro.position())

  def securityPermissionDenied[A](message: String, pos: SourcePosition): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.securityPermissionDenied(message).withSourcePosition(pos))

  inline def securityPermissionDenied[A](
    message: String,
    facets: Seq[Descriptor.Facet]
  ): Consequence.Failure[A] =
    securityPermissionDenied(message, facets, SourcePositionMacro.position())

  def securityPermissionDenied[A](
    message: String,
    facets: Seq[Descriptor.Facet],
    pos: SourcePosition
  ): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.securityPermissionDenied(message, facets).withSourcePosition(pos))

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failUnreachableReached: Consequence.Failure[Nothing] =
    Failures.unreachableReached

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failUnreachableReached[A](msg: String): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.unreachableReached(msg))

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failUninitializedState[A]: Consequence.Failure[A] =
    Failures.uninitializedState

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failUninitializedState[A](conclusion: Conclusion): Consequence.Failure[A] =
    Failures.uninitializedState(conclusion)

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failImpossibleState[A](msg: String): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.impossibleState(msg))

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failUnsupported[A](msg: String): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.unsupported(msg))

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failNotImplemented: Consequence.Failure[Nothing] =
    Failures.notImplemented

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failNotImplemented[A](msg: String): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.notImplemented(msg))

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failInvariantViolation[A](msg: String): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.invariantViolation(msg))

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failPreconditionViolation[A](msg: String): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.preconditionViolation(msg))

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def failPostconditionViolation[A](msg: String): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.postconditionViolation(msg))

  // obsolated. Use notImplemented
  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def NotImplemented: Consequence.Failure[Nothing] =
    Failures.fail(Taxonomy.notImplemented)

  @deprecated("Use semantic Consequence utilities instead.", "Apr. 14, 2026")
  def NotImplemented(msg: String): Consequence.Failure[Nothing] =
    Failures.fail(Taxonomy.notImplemented, Cause.message(msg))

  def DataStoreNotFound(id: String): Consequence.Failure[Nothing] =
    Failures.fail(
      Taxonomy.dataStoreNotFound,
      Seq(Descriptor.Facet.Id(id))
    )

  def DataStoreDuplicate(id: String): Consequence.Failure[Nothing] =
    Failures.fail(
      Taxonomy.dataStoreDuplicate,
      Seq(Descriptor.Facet.Id(id))
    )

  // RAISE
  object RAISE {
    inline def UnreachableReached: Nothing = unreachableReached.RAISE
    inline def UnreachableReached(msg: String): Nothing = unreachableReached(msg).RAISE
    inline def UninitializedState: Nothing = uninitializedState.RAISE
    inline def UninitializedState(c: Conclusion): Nothing = uninitializedState(c).RAISE
    inline def ImpossibleState(msg: String): Nothing = Failure(Conclusion.impossibleState(msg)).RAISE
    inline def Unsupported(msg: String): Nothing = Failure(Conclusion.unsupported(msg)).RAISE
    inline def NotImplemented: Nothing = notImplemented.RAISE
    inline def NotImplemented(msg: String): Nothing = notImplemented(msg).RAISE
    inline def InvariantViolation(msg: String): Nothing = Failure(Conclusion.invariantViolation(msg)).RAISE
    inline def PreconditionViolation(msg: String): Nothing = Failure(Conclusion.preconditionViolation(msg)).RAISE
    inline def PostconditionViolation(msg: String): Nothing = Failure(Conclusion.postconditionViolation(msg)).RAISE
  }
}

class ConsequenceException(
  val consequence: Consequence[?]
) extends RuntimeException(consequence match {
  case Consequence.Failure(c) => c.displayMessage
  case _ => "ConsequenceException"
}) {
  override def getMessage: String =
    consequence match {
      case Consequence.Failure(c) => c.displayMessage
      case _ => super.getMessage
    }
}
