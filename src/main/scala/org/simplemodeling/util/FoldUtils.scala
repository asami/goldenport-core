package org.goldenport.util

import cats._
import cats.implicits._

/*
 * @since   Oct. 17, 2025
 * @version Oct. 17, 2025
 * @author  ASAMI, Tomoharu
 */
object FoldUtils {
  def intercalateFoldLeft[B: Monoid](
    as: Seq[B],
    sep: B
  ): B = as match {
    case Nil => Monoid[B].empty
    case Seq(a) => a
    case a +: rest => rest.foldLeft(a)((acc, b) => acc |+| sep |+| b)
  }

  def intercalateFoldMap[A, B: Monoid](
    as: Seq[A],
    sep: B
  )(f: A => B): B = as match {
    case Nil => Monoid[B].empty
    case Seq(a) => f(a)
    case a +: rest => rest.foldLeft(f(a))((acc, x) => acc |+| sep |+| f(x))
  }

  def intercalateFoldMap_[A, B: Monoid](
    as: Seq[A],
    sep: B
  )(f: A => B): Unit = {
    val _ = intercalateFoldMap(as, sep)(f)
    ()
  }
} 
