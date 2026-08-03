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

  def resolveSnapshot(
    sources: Seq[ConfigurationSource]
  ): Consequence[ConfigurationResolutionSnapshot] =
    resolve(sources).map { resolved =>
      new ConfigurationResolutionSnapshot(resolved, Vector.empty)
    }

  def resolve(
    sources: ConfigurationSources
  ): Consequence[ResolvedConfiguration] =
    resolve(sources.sources)
}

object ConfigurationResolver {

  def default: ConfigurationResolver =
    new DefaultConfigurationResolver
}

/** The result of loading one physical configuration source.
 *
 *  `value` remains the compatibility map representation. `rawDocument`, when
 *  available, retains a file format's physical member order and multiplicity
 *  for consumers that need to perform their own semantic admission.
 */
final class ConfigurationSourceLoad(
  val value: Configuration,
  val rawDocument: Option[ConfigurationDocument.Object]
)

object ConfigurationSourceLoad {
  def apply(value: Configuration): ConfigurationSourceLoad =
    new ConfigurationSourceLoad(value, None)

  def apply(
    value: Configuration,
    rawDocument: Option[ConfigurationDocument.Object]
  ): ConfigurationSourceLoad =
    new ConfigurationSourceLoad(value, rawDocument)
}

final class ConfigurationRuntimeSourceSnapshot private[configuration] (
  val source: ConfigurationSource,
  val value: Configuration,
  val sourceOrdinal: Int,
  val rawDocument: Option[ConfigurationDocument.Object]
) {
  def origin: ConfigurationOrigin = source.origin
  def sourceRank: Int = source.rank
  def sourceIdentity: String =
    source.location.getOrElse(s"${origin.toString.toLowerCase}-$sourceOrdinal")
}

final class ConfigurationResolutionSnapshot private[configuration] (
  val resolved: ResolvedConfiguration,
  val sources: Vector[ConfigurationRuntimeSourceSnapshot]
)

/** Resolves ordered physical sources once and retains their immutable values
 *  alongside the compatibility resolved configuration and trace.
 */
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
  ): Consequence[ResolvedConfiguration] =
    resolveSnapshot(sources).map(_.resolved)

  override def resolveSnapshot(
    sources: Seq[ConfigurationSource]
  ): Consequence[ConfigurationResolutionSnapshot] = boundary {

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

    var currentconfiguration: Configuration = Configuration.empty
    var currenttrace: ConfigurationTrace = ConfigurationTrace.empty

    var snapshots = Vector.empty[ConfigurationRuntimeSourceSnapshot]

    ordered.zipWithIndex.foreach { case (source, ordinal) =>
      source.loadSnapshot() match {
        case Consequence.Success(loaded) =>
          val snapshot = new ConfigurationRuntimeSourceSnapshot(
            source,
            loaded.value,
            ordinal,
            loaded.rawDocument
          )
          val (nextconfiguration, nexttrace) =
            MergePolicy.merge(
              currentconfiguration,
              currenttrace,
              source,
              loaded.value
            )

          currentconfiguration = nextconfiguration
          currenttrace  = nexttrace
          snapshots = snapshots :+ snapshot

        case Consequence.Failure(err) =>
          break(Consequence.Failure(err))
      }
    }

    Consequence.Success(new ConfigurationResolutionSnapshot(
      ResolvedConfiguration(
        configuration = currentconfiguration,
        trace  = currenttrace
      ),
      snapshots
    ))
  }
}
