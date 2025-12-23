package org.simplemodeling.cli

/*
 * @since   Dec. 22, 2025
 * @version Dec. 22, 2025
 * @author  ASAMI, Tomoharu
 */
trait Renderer:
  def render(response: String): Unit

object TextRenderer extends Renderer:
  def render(response: String): Unit =
    println(response)

object JsonRenderer extends Renderer:
  def render(response: String): Unit =
    println(response)

object ArgParser:
  def parse(args: Array[String]): List[String] =
    args.toList
