package org.goldenport.datatype

/*
 * @since   Jul. 23, 2025
 *  version Jul. 23, 2025
 * @version Apr.  9, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class Text() extends StringDataType() {
  import Text._

  protected final def length_min: Int = LENGTH_MIN
  protected final def length_max: Int = LENGTH_MAX
  protected final def is_valid(s: String): Boolean = validate_printable(s)
}

object Text {
import org.goldenport.Consequence
import org.goldenport.convert.ValueReader

  val LENGTH_MIN = 0
  val LENGTH_MAX = 8192

  case class Instance(value: String) extends Text() {
  }

  given ValueReader[Text] with
    def readC(v: Any): Consequence[Text] = Option(v) match
      case None => Consequence.failure("Invalid Text value: null")
      case Some(value) => value match
        case t: Text => Consequence.success(t)
        case s: String => Consequence.success(Text(s))
        case other => Consequence.success(Text(other.toString))

  def apply(s: String): Text = Instance(s)

  def parse(s: String): Consequence[Text] = Consequence(apply(s))
}
