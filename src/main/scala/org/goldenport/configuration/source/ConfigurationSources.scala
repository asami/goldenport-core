package org.goldenport.configuration

import org.goldenport.configuration.source.ConfigurationSource
import org.goldenport.configuration.source.ProjectRootFinder

import java.nio.file.Path

/*
 * @since   Dec. 18, 2025
 *  version Jan. 16, 2026
 * @version Mar. 24, 2026
 * @author  ASAMI, Tomoharu
 */
final case class ConfigurationSources(
  sources: Seq[ConfigurationSource]
)

object ConfigurationSources {
  def standard(
    cwd: Path,
    applicationname: String = ConfigurationSource.DefaultApplicationName,
    args: Map[String, String] = Map.empty,
    env: Map[String, String]  = sys.env
  ): ConfigurationSources = {

    val home      = ConfigurationSource.home(applicationname)
    val project   =
      ProjectRootFinder
        .find(cwd, applicationname)
        .toSeq
        .flatMap(root => ConfigurationSource.project(root, applicationname))
    val current   = ConfigurationSource.cwd(cwd, applicationname)
    val envSrc    = ConfigurationSource.env(env, applicationname)
    val argSrc    = ConfigurationSource.args(args)

    ConfigurationSources(
      Seq(
        home,
        project,
        current,
        envSrc,
        argSrc
      ).flatten
    )
  }
}
