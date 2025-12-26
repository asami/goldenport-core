package org.goldenport.cli.config

/*
 * @since   Dec. 22, 2025
 * @version Dec. 22, 2025
 * @author  ASAMI, Tomoharu
 */
case class CliConfig(endpoint: String = "")

case class CliConfigSchema(name: String = "cli")

object ConfigMerge:
  def merge(configs: Seq[CliConfig]): CliConfig =
    configs.headOption.getOrElse(CliConfig())
