package org.goldenport.datatype

import org.goldenport.bag.Bag

/*
 * @since   Jan. 20, 2026
 * @version Jan. 20, 2026
 * @author  ASAMI, Tomoharu
 */
final case class MimeBody(
  contentType: ContentType,
  value: Bag
) extends DataType() {
  override def display: String =
    s"MimeBody(${contentType.display})"

  override def show: String =
    s"MimeBody(contentType=${contentType.show}, metadata=${value.metadata})"

  override final def print: String = display
}
