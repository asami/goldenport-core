package org.goldenport.record

import org.goldenport.Consequence

/*
 * @since   Feb. 22, 2026
 * @version Feb. 22, 2026
 * @author  ASAMI, Tomoharu
 */
trait RecordEncoder[E]:
  def toRecord(e: E): Record

trait RecordDecoder[E]:
  def fromRecord(r: Record): Consequence[E]

trait RecordCodex[E]
  extends RecordEncoder[E]
     with RecordDecoder[E]
