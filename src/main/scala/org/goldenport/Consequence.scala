package org.goldenport

import cats._
import cats.data.NonEmptyVector
import scala.util.control.NonFatal
import org.goldenport.text.Presentable
import org.goldenport.schema.DataType
import org.goldenport.schema.Constraint
import org.goldenport.provisional.observation.Observation
import org.goldenport.provisional.observation.Taxonomy
import org.goldenport.provisional.observation.Cause
import org.goldenport.observation.Descriptor
import org.goldenport.http.HttpRequest

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
 * @version Jan. 29, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait Consequence[+T] extends Presentable {
  def get: Option[T]

  def take: T

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

  override def print: String = this match {
    case Consequence.Success(v) => Presentable.print(v)
    case Consequence.Failure(c) => c.display
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
  case class Success[+T](
    result: T
  ) extends Consequence[T] {
    def get: Option[T] = Some(result)

    def take = result

    def transform[U](
      s: T => Consequence[U],
      c: Conclusion => Consequence[U]
    ): Consequence[U] = s(result)

    def recoverWith[U >: T](f: Conclusion => Consequence[U]): Consequence[U] = this

    def recover[U >: T](f: Conclusion => U): Consequence[U] = this

    def recoverConclusion(f: Conclusion => Conclusion): Consequence[T] = this

    def fold[U >: T](c: Conclusion => U, s: T => U): U = s(result)

    def foldIdntity[U >: T](c: Conclusion => U): U = result

    def getOrElse[U >: T](body: => U): U = result
  }

  case class Failure[+T](conclusion: Conclusion) extends Consequence[T] {
    def get: Option[T] = None

    def take = RAISEC

    def transform[U](
      s: T => Consequence[U],
      c: Conclusion => Consequence[U]
    ): Consequence[U] = Consequence.run(c(conclusion))

    def recoverWith[U >: T](f: Conclusion => Consequence[U]): Consequence[U] =
      Consequence.run(f(conclusion))

    def recover[U >: T](f: Conclusion => U): Consequence[U] =
      Consequence(f(conclusion))

    def recoverConclusion(f: Conclusion => Conclusion): Consequence[T] =
      Consequence.run(Consequence.Failure(f(conclusion)))

    def fold[U >: T](c: Conclusion => U, s: T => U): U = c(conclusion)

    def foldIdntity[U >: T](c: Conclusion => U): U = c(conclusion)

    def getOrElse[U >: T](body: => U): U = body
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

  def success[T](p: T): Consequence[T] = Success(p)

  def failure[T](message: String): Consequence[T] =
    Failure(Conclusion.simple(message))

  def failure[T](e: Throwable): Consequence[T] =
    Failure(Conclusion.from(e))

  def fromOption[A](
    opt: Option[A],
    onNone: => String
  ): Consequence[A] =
    opt match {
      case Some(v) => Success(v)
      case None => failure(onNone)
    }

  def toInt(p: String): Consequence[Int] = Consequence(p.toInt)

  def takeOrMissingPropertyFault[T](key: String, v: Option[T]): Consequence[T] =
    ???

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
  def failArgumentValidationError: FailureBuilder = ???

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

  // Fail
  def fail[A](o: Observation): Consequence.Failure[A] = ???

  def fail[A](taxonomy: Taxonomy, message: String, facets: Seq[Descriptor.Facet]):Consequence[A] = {
    ???
  }

  // def failArgumentEmpty[A]: Consequence.Failure[A] =
  //   Consequence.Failure(Conclusion.failArgumentEmpty)

  def failArgumentFormatError[A](name: String, value: Any, dt: DataType): Consequence.Failure[A] = ???

  def failArgumentFormatError[A](name: String, value: Any, msg: String): Consequence.Failure[A] = ???

  def failArgumentFormatError[A](name: String, value: Option[Any], msg: Option[String]): Consequence.Failure[A] = ???

  def failArgumentMissing[A]: Consequence.Failure[A] =
    Consequence.Failure(Conclusion.failArgumentMissing)

  def failArgumentMissing[A](name: String): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.failArgumentMissing(name))

  def failArgumentMissingInput[A](name: String): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.failArgumentMissingInput(name))

  def failArgumentMissingInput[A](args: Seq[String]): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.failArgumentMissingInput(args))

  def failArgumentMissingInput[A](req: HttpRequest): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.failArgumentMissingInput(req))

  def failArgumentMissingOperation[A](name: String, operation: String): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.failArgumentMissingOperation(name, operation))

  def failArgumentRedundantOperation[A](name: String, operation: String): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.failArgumentRedundantOperation(name, operation))

  def failArgumentRedundantOperationInput[A](operation: String, args: Seq[String]): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.failArgumentRedundantOperationInput(operation, args))

  def failArgumentDataType[A](name: String, value: Any, dt: DataType): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.failArgumentDataType(name, value, dt))

  def failArgumentConstraint[A](name: String, value: Any, constraints: NonEmptyVector[Constraint]): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.failArgumentConstraint(name, value, constraints))

  def failArgumentMultipleValues[A](name: String): Consequence.Failure[A] = ???

  def failOperationInvalid[A](name: String): Consequence[A] =
    Consequence.Failure(Conclusion.failOperationInvalid(name))

  def failValueInvalid[A](value: Any, dt: DataType): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.failValueInvalid(value, dt))

  def failValueFormatError[A](value: Any, dt: DataType): Consequence.Failure[A] =
    Consequence.Failure(Conclusion.failValueFormatError(value, dt))
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
