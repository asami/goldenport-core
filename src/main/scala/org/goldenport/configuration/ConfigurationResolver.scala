package org.goldenport.configuration

import org.goldenport.Consequence
import org.goldenport.configuration.Configuration
import org.goldenport.configuration.ConfigurationSources
import org.goldenport.configuration.MergePolicy
import org.goldenport.configuration.ResolvedConfiguration
import org.goldenport.configuration.source.ConfigurationSource
import org.goldenport.configuration.ConfigurationTrace
import org.goldenport.configuration.ConfigurationOrigin
import scala.util.boundary
import scala.util.boundary.break

/**
 * ConfigurationResolver is the single public entry point for configuration resolution.
 *
 * Responsibilities:
 *   - evaluate given configuration sources in precedence order
 *   - merge configurations deterministically
 *   - produce both final configuration and full resolution trace
 *
 * Non-responsibilities:
 *   - configuration semantics
 *   - validation
 *   - defaults
 *   - logging
 *   - source discovery
 */
/*
 * @since   Dec. 18, 2025
 * @version Jan. 16, 2026
 * @author  ASAMI, Tomoharu
 */
trait ConfigurationResolver {

  def resolve(
    sources: Seq[ConfigurationSource]
  ): Consequence[ResolvedConfiguration]

  def resolve(
    sources: ConfigurationSources
  ): Consequence[ResolvedConfiguration] =
    resolve(sources.sources)
}

object ConfigurationResolver {

  def default: ConfigurationResolver =
    new DefaultConfigurationResolver
}

// Scaladoc to add comment? patch final class area
final class DefaultConfigurationResolver
  extends ConfigurationResolver {

  /**
   * Configuration resolution order is explicitly defined here:
   *   1. Resource
   *   2. Home
   *   3. Project
   *   4. Cwd
   *   5. Environment
   *   6. Arguments
   *
   * ConfigurationSource implementations must remain order-agnostic and simply expose data.
   * Configuration resolution is intentionally separate from semantic concerns.
   * This ordering is intended to stay stable during future core migration.
   */

  override def resolve(
    sources: Seq[ConfigurationSource]
  ): Consequence[ResolvedConfiguration] = boundary {

    val resources = sources.filter(_.origin == ConfigurationOrigin.Resource)
    val home = sources.filter(_.origin == ConfigurationOrigin.Home)
    val project = sources.filter(_.origin == ConfigurationOrigin.Project)
    val cwd = sources.filter(_.origin == ConfigurationOrigin.Cwd)
    val environment = sources.filter(_.origin == ConfigurationOrigin.Environment)
    val arguments = sources.filter(_.origin == ConfigurationOrigin.Arguments)
    val ordered = Seq(
      resources,
      home,
      project,
      cwd,
      environment,
      arguments
    ).flatten

    var currentConfiguration: Configuration = Configuration.empty
    var currentTrace: ConfigurationTrace = ConfigurationTrace.empty

    ordered.foreach { source =>
      source.load() match {
        case Consequence.Success(cfg) =>
          val (nextConfiguration, nextTrace) =
            MergePolicy.merge(
              currentConfiguration,
              currentTrace,
              source
            )

          currentConfiguration = nextConfiguration
          currentTrace  = nextTrace

        case Consequence.Failure(err) =>
          break(Consequence.Failure(err))
      }
    }

    Consequence.Success(
      ResolvedConfiguration(
        configuration = currentConfiguration,
        trace  = currentTrace
      )
    )
  }
}
