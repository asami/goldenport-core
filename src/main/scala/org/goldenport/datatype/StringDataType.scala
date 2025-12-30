package org.goldenport.datatype

/*
 * @since   Jul. 20, 2025
 *  version Jul. 23, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
abstract class StringDataType() extends DataType() {
  import StringDataType._

  def value: String

  def length: Int = value.length
  def codePointCount: Int = value.codePointCount(0, value.length)
  def isMatch(p: String): Boolean = value == p

  protected def length_min: Int
  protected def length_max: Int
  protected def length_Min: Option[Int] = None
  protected def length_Max: Option[Int] = None
  protected def is_valid(p: String): Boolean
  protected def is_Valid(p: String): Option[Boolean] = None

  protected final def is_valid_min: Boolean =
    length >= length_min && length_Min.fold(true)(_ <= length)

  protected final def is_valid_max: Boolean =
    length <= length_max && length_Max.fold(true)(_ >= length)

  protected final def is_valid_string(p: String): Boolean =
    is_valid(p) && is_Valid(p).getOrElse(true)

  require (is_valid_min, s"Too short: ${value.length}")
  require (is_valid_max, s"Too large: ${value.length}")
  require (is_valid_string(value), s"Invalid value")

  protected final def validate_printable(s: String) =
    isValidPrintable(s)

  protected final def validate_token(s: String) =
    isValidToken(s)

  protected final def validate_identifier(s: String) =
    isValidIdentifier(s)
}

object StringDataType {
  def isValidPrintable(s: String): Boolean =
    !s.exists(ch => ch <= '\u001F' || ch == '\u007F')

  def isValidToken(s: String): Boolean = {
    // Must not contain invalid XML control characters
    val hasInvalidControlChars = s.exists { ch =>
      (ch <= '\u001F' && ch != ' ') || ch == '\u007F' || (ch >= '\u0080' && ch <= '\u009F')
    }

    // Must not contain \n, \r, or \t
    val hasIllegalWhitespace = s.exists(ch => ch == '\n' || ch == '\r' || ch == '\t')

    // Must not have leading/trailing whitespace
    val hasTrimmedWhitespace = s != s.trim

    // Must not contain multiple consecutive spaces
    val hasConsecutiveSpaces = s.contains("  ")

    !(hasInvalidControlChars || hasIllegalWhitespace || hasTrimmedWhitespace || hasConsecutiveSpaces)
  }

  def isValidIdentifier(s: String): Boolean = {
    val pattern = "^[A-Za-z][A-Za-z0-9_]*$".r
    pattern.matches(s)
  }
}
