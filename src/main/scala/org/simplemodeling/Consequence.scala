package org.simplemodeling

import cats._
import scala.util.control.NonFatal

/*
 * @since   Jul. 19, 2025
 *  version Jul. 20, 2025
 *  version Sep. 30, 2025
 *  version Nov. 11, 2025
 * @version Dec. 26, 2025
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

