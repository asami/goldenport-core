package org.goldenport.id

/*
 * @since   Feb. 22, 2026
 * @version Feb. 22, 2026
 * @author  ASAMI, Tomoharu
 */
trait Identified[T, I <: UniversalId] {
  def id(entity: T): I
}

trait Identifiable[T, I <: UniversalId] {
  def id(entity: T): Option[I]
}
