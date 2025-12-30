package org.goldenport.log

/*
 * @since   Dec. 30, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
trait Logger {
  def trace(message: => String): Unit
  def debug(message: => String): Unit
  def info(message: => String): Unit
  def warn(message: => String): Unit
  def error(message: => String): Unit
  def error(cause: Throwable, message: => String): Unit
  def fatal(message: => String): Unit
  def fatal(cause: Throwable, message: => String): Unit
}
