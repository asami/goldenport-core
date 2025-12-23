package org.simplemodeling

import cats._
import scala.util.control.NonFatal

/*
 * @since   Jul. 19, 2025
 *  version Jul. 20, 2025
 *  version Sep. 30, 2025
 *  version Nov. 11, 2025
 * @version Dec. 18, 2025
 * @author  ASAMI, Tomoharu
 */
sealed trait Consequence[+T] {
  def take: T
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

  def toInt(p: String): Consequence[Int] = Consequence(p.toInt)

  def takeOrMissingPropertyFault[T](key: String, v: Option[T]): Consequence[T] =
    ???
}
