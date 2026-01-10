package org.goldenport

import cats.{Applicative, Functor, Monad}

/*
 * @since   Jan. 10, 2026
 * @version Jan. 10, 2026
 * @author  ASAMI, Tomoharu
 */
final case class ConsequenceT[F[_], A](
  value: F[Consequence[A]]
)

object ConsequenceT {
  def pure[F[_]: Applicative, A](a: A): ConsequenceT[F, A] =
    ConsequenceT(Applicative[F].pure(Consequence.Success(a)))

  def liftF[F[_]: Functor, A](fa: F[A]): ConsequenceT[F, A] =
    ConsequenceT(Functor[F].map(fa)(Consequence.Success(_)))

  def fromConsequence[F[_]: Applicative, A](
    c: Consequence[A]
  ): ConsequenceT[F, A] =
    ConsequenceT(Applicative[F].pure(c))

  given functor[F[_]](using Functor[F]): Functor[[A] =>> ConsequenceT[F, A]] with
    def map[A, B](fa: ConsequenceT[F, A])(f: A => B): ConsequenceT[F, B] =
      fa.map(f)

  given monad[F[_]](using Monad[F]): Monad[[A] =>> ConsequenceT[F, A]] with
    def pure[A](x: A): ConsequenceT[F, A] =
      ConsequenceT.pure(x)

    def flatMap[A, B](
      fa: ConsequenceT[F, A]
    )(f: A => ConsequenceT[F, B]): ConsequenceT[F, B] =
      fa.flatMap(f)

    def tailRecM[A, B](
      a: A
    )(f: A => ConsequenceT[F, Either[A, B]]): ConsequenceT[F, B] = {
      val resolved =
        Monad[F].tailRecM[A, Consequence[B]](a) { current =>
          Functor[F].map(f(current).value) {
            case Consequence.Success(Right(b)) =>
              Right(Consequence.Success(b))
            case Consequence.Success(Left(next)) =>
              Left(next)
            case e @ Consequence.Failure(_) =>
              Right(e.asInstanceOf[Consequence[B]])
          }
        }
      ConsequenceT(resolved)
    }
}

extension [F[_], A](self: ConsequenceT[F, A])
  def map[B](f: A => B)(using Functor[F]): ConsequenceT[F, B] =
    ConsequenceT(
      Functor[F].map(self.value) {
        case Consequence.Success(a) => Consequence.Success(f(a))
        case e @ Consequence.Failure(_) =>
          e.asInstanceOf[Consequence[B]]
      }
    )

  def flatMap[B](f: A => ConsequenceT[F, B])(using Monad[F]): ConsequenceT[F, B] =
    ConsequenceT(
      Monad[F].flatMap(self.value) {
        case Consequence.Success(a) =>
          f(a).value
        case e @ Consequence.Failure(_) =>
          Monad[F].pure(e.asInstanceOf[Consequence[B]])
      }
    )
