package org.goldenport.configuration

import org.goldenport.configuration.source.ConfigurationSource
import org.goldenport.configuration.source.ResourceConfigurationSource

import java.nio.file.Path

/*
 * @since   Dec. 18, 2025
 * @version Jan. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final case class ConfigurationSources(
  sources: Seq[ConfigurationSource]
)

object ConfigurationSources {
  def standard(
    cwd: Path,
    args: Map[String, String] = Map.empty,
    env: Map[String, String]  = sys.env
  ): ConfigurationSources = {

    val resources = ResourceConfigurationSource.fromClasspath()
    val home      = ConfigurationSource.home()
    val project   = ConfigurationSource.project(cwd)
    val current   = ConfigurationSource.cwd(cwd)
    val envSrc    = ConfigurationSource.env(env)
    val argSrc    = ConfigurationSource.args(args)

    ConfigurationSources(
      Seq(
        resources,
        home,
        project,
        current,
        envSrc,
        argSrc
      ).flatten
    )
  }
}
