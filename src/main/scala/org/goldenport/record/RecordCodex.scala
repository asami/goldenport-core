package org.goldenport.record

import org.goldenport.Consequence

/*
 * @since   Feb. 22, 2026
 * @version Mar. 31, 2026
 * @author  ASAMI, Tomoharu
 */
trait RecordEncoder[E]:
  def toRecord(e: E): Record

trait RecordDecoder[E]:
  def fromRecord(r: Record): Consequence[E]

trait RecordPresentable:
  def toRecord(): Record

trait RecordCodex[E]
  extends RecordEncoder[E]
     with RecordDecoder[E]
