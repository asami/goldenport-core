package org.goldenport.schema

import java.io.File
import java.math.MathContext
import java.net.URL
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal
import com.typesafe.config.{Config, ConfigFactory, ConfigIncludeContext, ConfigIncluder,
  ConfigIncluderClasspath, ConfigIncluderFile, ConfigIncluderURL, ConfigList,
  ConfigObject, ConfigParseOptions, ConfigSyntax, ConfigValue, ConfigValueType}
import org.goldenport.Consequence
import org.goldenport.record.Record

/*
 * Literal HOCON conversion with exact numeric text and declared string meaning.
 *
 * @since   Oct.  5, 2026
 * @version Oct.  5, 2026
 * @author  ASAMI, Tomoharu
 */
private[schema] object StructuredDataHocon {
  def fromText(text: String): Consequence[Record] = {
    _parse_hocon(text).flatMap { original =>
      if (!original.isResolved) {
        _invalid("HOCON substitutions are unsupported")
      } else {
        _adapt_integer_values(text).flatMap { adapted =>
          if (adapted == text) {
            _hocon_object(original.root(), original.root())
          } else {
            _parse_hocon(adapted).flatMap(config => _hocon_object(original.root(), config.root()))
          }
        }
      }
    }
  }

  private def _parse_hocon(text: String): Consequence[Config] = {
    try {
      val options = ConfigParseOptions.defaults().setSyntax(ConfigSyntax.CONF)
        .setIncluder(RejectingIncluder)
      Consequence.success(ConfigFactory.parseString(text, options))
    } catch {
      case NonFatal(_) => Consequence.valueFormatError("Malformed or included HOCON input", XSemiStructuredData)
    }
  }

  private def _hocon_object(original: ConfigObject, adapted: ConfigObject): Consequence[Record] = {
    if (original.keySet() != adapted.keySet()) {
      _invalid("HOCON numeric adaptation changed object keys")
    } else {
      val fields = original.entrySet().asScala.toVector.map { member =>
        _decode_hocon(member.getValue, adapted.get(member.getKey)).map(member.getKey -> _)
      }
      Consequence.zipN(fields).map(Record.createFull)
    }
  }

  private def _hocon_list(original: ConfigList, adapted: ConfigList): Consequence[Vector[Any]] = {
    if (original.size() != adapted.size()) {
      _invalid("HOCON numeric adaptation changed list length")
    } else {
      Consequence.zipN(original.asScala.toVector.zip(adapted.asScala.toVector).map {
        case (before, after) => _decode_hocon(before, after)
      }).map(_.toVector)
    }
  }

  private def _decode_hocon(original: ConfigValue, adapted: ConfigValue): Consequence[Any] = {
    (original.valueType(), adapted.valueType()) match {
      case (ConfigValueType.OBJECT, ConfigValueType.OBJECT) =>
        _hocon_object(original.asInstanceOf[ConfigObject], adapted.asInstanceOf[ConfigObject])
      case (ConfigValueType.LIST, ConfigValueType.LIST) =>
        _hocon_list(original.asInstanceOf[ConfigList], adapted.asInstanceOf[ConfigList])
      case (ConfigValueType.STRING, ConfigValueType.NUMBER) => _hocon_number(adapted)
      case (ConfigValueType.STRING, ConfigValueType.STRING) =>
        Consequence.success(original.unwrapped().asInstanceOf[String])
      case (ConfigValueType.NUMBER, ConfigValueType.NUMBER) => _hocon_number(original)
      case (ConfigValueType.BOOLEAN, ConfigValueType.BOOLEAN) =>
        Consequence.success(original.unwrapped().asInstanceOf[Boolean])
      case (ConfigValueType.NULL, ConfigValueType.NULL) => Consequence.success(null)
      case _ => _invalid("HOCON numeric adaptation changed an unsupported value type")
    }
  }

  private def _hocon_number(value: ConfigValue): Consequence[BigDecimal] = {
    try {
      val text = value.atKey("canonicalValue").getString("canonicalValue")
      val decimal = new java.math.BigDecimal(text, MathContext.UNLIMITED)
      Consequence.success(new BigDecimal(decimal, MathContext.UNLIMITED))
    } catch {
      case NonFatal(_) => _invalid("HOCON number has no exact BigDecimal representation")
    }
  }

  private enum LexicalContext {
    case Key, AwaitValue, InValue, ArrayValue
  }

  // Config 1.4.3 treats integer lexemes outside Long as unquoted strings.
  // Adapt only value tokens after the original parser has admitted the syntax.
  private def _adapt_integer_values(text: String): Consequence[String] = {
    try {
      val result = new StringBuilder
      var frames = List(LexicalContext.Key)
      var index = 0
      while (index < text.length) {
        val char = text.charAt(index)
        if (_is_whitespace(char)) {
          result.append(char)
          if ((char == '\n' || char == '\r') && frames.head == LexicalContext.InValue) {
            frames = LexicalContext.Key :: frames.tail
          }
          index += 1
        } else if (char == '#' || _starts_comment(text, index)) {
          val start = index
          while (index < text.length && text.charAt(index) != '\n' && text.charAt(index) != '\r') {
            index += 1
          }
          result.append(text.substring(start, index))
        } else if (char == '"') {
          val end = _quoted_end(text, index)
          result.append(text.substring(index, end))
          if (frames.head == LexicalContext.AwaitValue) {
            frames = LexicalContext.InValue :: frames.tail
          }
          index = end
        } else if (char == '{' || char == '[') {
          if (frames.head != LexicalContext.ArrayValue) {
            frames = LexicalContext.InValue :: frames.tail
          }
          frames = (if (char == '{') LexicalContext.Key else LexicalContext.ArrayValue) :: frames
          result.append(char)
          index += 1
        } else if (char == '}' || char == ']') {
          frames = frames.tail
          result.append(char)
          index += 1
        } else if (char == ':' || char == '=') {
          if (frames.head != LexicalContext.ArrayValue) {
            frames = LexicalContext.AwaitValue :: frames.tail
          }
          result.append(char)
          index += 1
        } else if (char == ',') {
          if (frames.head != LexicalContext.ArrayValue) {
            frames = LexicalContext.Key :: frames.tail
          }
          result.append(char)
          index += 1
        } else if (_is_reserved(char)) {
          result.append(char)
          index += 1
        } else {
          val start = index
          while (index < text.length && !_is_whitespace(text.charAt(index)) &&
            !_is_reserved(text.charAt(index)) && !_starts_comment(text, index)) {
            index += 1
          }
          val token = text.substring(start, index)
          result.append(token)
          if (frames.head != LexicalContext.Key) {
            if (token.matches("-?[0-9]+") && !BigInt(token).isValidLong) {
              result.append(".0")
            }
            if (frames.head == LexicalContext.AwaitValue) {
              frames = LexicalContext.InValue :: frames.tail
            }
          }
        }
      }
      Consequence.success(result.toString)
    } catch {
      case NonFatal(_) => _invalid("HOCON integer value adaptation failed")
    }
  }

  private def _quoted_end(text: String, start: Int): Int = {
    if (text.startsWith("\"\"\"", start)) {
      val end = text.indexOf("\"\"\"", start + 3)
      if (end < 0) {
        throw new IllegalArgumentException("Unclosed HOCON triple-quoted string")
      }
      var index = end + 3
      while (index < text.length && text.charAt(index) == '"') {
        index += 1
      }
      index
    } else {
      var index = start + 1
      while (index < text.length && text.charAt(index) != '"') {
        index += (if (text.charAt(index) == '\\') 2 else 1)
      }
      if (index >= text.length) {
        throw new IllegalArgumentException("Unclosed HOCON quoted string")
      }
      index + 1
    }
  }

  private def _is_whitespace(char: Char): Boolean = {
    Character.isWhitespace(char) || char == '\u00a0' || char == '\u2007' ||
      char == '\u202f' || char == '\ufeff'
  }

  private def _starts_comment(text: String, index: Int): Boolean = {
    text.startsWith("//", index)
  }

  private def _is_reserved(char: Char): Boolean = {
    "$\"{}[]:=,+#`^?!@*&\\".indexOf(char) >= 0
  }

  private def _invalid[A](description: String): Consequence[A] = {
    Consequence.valueInvalid(description, XSemiStructuredData)
  }

  private object RejectingIncluder
    extends ConfigIncluder
    with ConfigIncluderFile
    with ConfigIncluderURL
    with ConfigIncluderClasspath {
    def withFallback(fallback: ConfigIncluder): ConfigIncluder = this

    def include(context: ConfigIncludeContext, what: String): ConfigObject = {
      throw new IllegalArgumentException("HOCON includes are unsupported")
    }

    def includeFile(context: ConfigIncludeContext, what: File): ConfigObject = {
      throw new IllegalArgumentException("HOCON file includes are unsupported")
    }

    def includeURL(context: ConfigIncludeContext, what: URL): ConfigObject = {
      throw new IllegalArgumentException("HOCON URL includes are unsupported")
    }

    def includeResources(context: ConfigIncludeContext, what: String): ConfigObject = {
      throw new IllegalArgumentException("HOCON classpath includes are unsupported")
    }
  }
}
