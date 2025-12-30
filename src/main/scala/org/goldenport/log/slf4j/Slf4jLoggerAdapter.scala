package org.goldenport.log.slf4j

import org.goldenport.log.Logger

/*
 * @since   Dec. 30, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
class Slf4jLoggerAdapter(
  underlying: AnyRef
) extends Logger {
  def trace(message: => String): Unit = _invoke_1("trace", message)
  def debug(message: => String): Unit = _invoke_1("debug", message)
  def info(message: => String): Unit = _invoke_1("info", message)
  def warn(message: => String): Unit = _invoke_1("warn", message)
  def error(message: => String): Unit = _invoke_1("error", message)
  def error(cause: Throwable, message: => String): Unit = _invoke_2("error", message, cause)
  def fatal(message: => String): Unit = _invoke_1("error", s"[FATAL] ${message}")
  def fatal(cause: Throwable, message: => String): Unit = _invoke_2("error", s"[FATAL] ${message}", cause)

  private def _invoke_1(methodname: String, message: String): Unit = {
    val m = underlying.getClass.getMethod(methodname, classOf[String])
    m.invoke(underlying, message)
    ()
  }

  private def _invoke_2(methodname: String, message: String, cause: Throwable): Unit = {
    val m = underlying.getClass.getMethod(methodname, classOf[String], classOf[Throwable])
    m.invoke(underlying, message, cause)
    ()
  }
}

object Slf4jLoggerAdapter {
  def apply(name: String): Slf4jLoggerAdapter =
    new Slf4jLoggerAdapter(_logger_by_name(name))

  def apply(clazz: Class[?]): Slf4jLoggerAdapter =
    new Slf4jLoggerAdapter(_logger_by_name(clazz.getName))

  private def _logger_by_name(name: String): AnyRef = {
    val factoryclass = Class.forName("org.slf4j.LoggerFactory")
    val method = factoryclass.getMethod("getLogger", classOf[String])
    method.invoke(null, name).asInstanceOf[AnyRef]
  }
}
