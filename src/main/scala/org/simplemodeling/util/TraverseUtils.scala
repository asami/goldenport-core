package org.goldenport.util

import cats._
import cats.implicits._

/*
 * @since   Oct. 17, 2025
 * @version Oct. 17, 2025
 * @author  ASAMI, Tomoharu
 */
object TraverseUtils {
  def intercalateTraverse[F[_]: Applicative, A, B](
    as: Seq[A],
    sep: F[B]
  )(f: A => F[B]): F[Vector[B]] =
    as match {
      case Nil => Applicative[F].pure(Vector.empty[B])
      case Seq(a) => f(a).map(Vector(_))
      case _ =>
        def loop(xs: Seq[A]): F[Vector[B]] = xs match {
          case Nil => Applicative[F].pure(Vector.empty)
          case Seq(x) => f(x).map(Vector(_))
          case x +: rest =>
            (f(x), sep, loop(rest)).mapN { (h, s, t) =>
              h +: s +: t
            }
        }
        loop(as)
    }

  def intercalateTraverse_[F[_]: Applicative, A](
    as: Seq[A],
    sep: F[Unit]
  )(f: A => F[Unit]): F[Unit] =
    intercalateTraverse[F, A, Unit](as, sep)(f).void
} 
