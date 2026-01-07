package org.goldenport

import cats._
import scala.util.control.NonFatal

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
 * @version Jan.  7, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait Consequence[+T] {
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
}

object Consequence {
  case class Success[+T](
    result: T
  ) extends Consequence[T] {
    def take = result
  }

  case class Failure[+T](conclusion: Conclusion) extends Consequence[T] {
    def take = ???
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
  def failArgumentMissing: FailureBuilder =
    FailureBuilder(org.goldenport.observation.Cause.Argument(
      org.goldenport.observation.Cause.Reason.Missing
    ))

  def failArgumentRedundant: FailureBuilder =
    FailureBuilder(org.goldenport.observation.Cause.Argument(
      org.goldenport.observation.Cause.Reason.Redundant
    ))

  def failArgumentValidationError: FailureBuilder =
    FailureBuilder(org.goldenport.observation.Cause.Argument(
      org.goldenport.observation.Cause.Reason.ValidationError
    ))

  final case class FailureBuilder(
    cause: org.goldenport.observation.Cause,
    descriptor: org.goldenport.observation.Descriptor =
      org.goldenport.observation.Descriptor()
  ) {
    def withOperation(name: String): FailureBuilder =
      copy(descriptor = _add_aspect(org.goldenport.observation.Descriptor.Aspect.Operation(name)))

    def withInput(name: String, value: Option[String] = None): FailureBuilder =
      copy(descriptor = _add_aspect(
        org.goldenport.observation.Descriptor.Aspect.Input(
          name = Some(name),
          value = value
        )
      ))

    private def _add_aspect(
      aspect: org.goldenport.observation.Descriptor.Aspect
    ): org.goldenport.observation.Descriptor =
      descriptor.copy(aspects = descriptor.aspects :+ aspect)

    def build[A]: Consequence[A] = {
      val base = Conclusion.simple("validation error")
      val observation = base.observation.copy(
        cause = Some(cause),
        descriptor = descriptor
      )
      Failure(base.copy(observation = observation))
    }
  }
}

class ConsequenceException(
  val consequence: Consequence[?]
) extends RuntimeException(consequence match {
  case Consequence.Failure(c) => c.message
  case _ => "ConsequenceException"
}) {
  override def getMessage: String =
    consequence match {
      case Consequence.Failure(c) => c.message
      case _ => super.getMessage
    }
}
