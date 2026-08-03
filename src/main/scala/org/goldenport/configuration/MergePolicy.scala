package org.goldenport.configuration

import org.goldenport.configuration.Configuration
import org.goldenport.configuration.ConfigurationValue
import org.goldenport.configuration.ConfigurationTrace
import org.goldenport.configuration.ConfigurationResolution
import org.goldenport.configuration.source.ConfigurationSource
import org.goldenport.configuration.source.ResourceConfigurationSource

/**
 * MergePolicy defines how configuration values are merged.
 *
 * This object is intentionally boring and explicit.
 * It performs:
 *   - key-based overwrite
 *   - deterministic resolution order
 *   - trace generation
 *
 * It does NOT:
 *   - validate values
 *   - interpret semantics
 *   - apply defaults
 *   - perform IO
 */
/*
 * @since   Dec. 18, 2025
 * @version Jul. 31, 2026
 * @author  ASAMI, Tomoharu
 */
object MergePolicy {

  /**
   * Merge a new ConfigurationSource into the current Configuration and ConfigurationTrace.
   */
  def merge(
    current: Configuration,
    trace: ConfigurationTrace,
    source: ConfigurationSource
  ): (Configuration, ConfigurationTrace) = {
    source.load() match {
      case org.goldenport.Consequence.Success(cfg) =>
        merge(current, trace, source, cfg)

      case org.goldenport.Consequence.Failure(_) =>
        (current, trace)
    }
  }

  def merge(
    current: Configuration,
    trace: ConfigurationTrace,
    source: ConfigurationSource,
    value: Configuration
  ): (Configuration, ConfigurationTrace) =
    value.values.foldLeft((current, trace)) {
      case ((cfg, tr), (key, entry)) =>
        val newconfiguration = Configuration(cfg.values.updated(key, entry))
        val newtrace  = _update_trace(tr, key, entry, source)
        (newconfiguration, newtrace)
    }

  private def _update_trace(
    trace: ConfigurationTrace,
    key: String,
    value: ConfigurationValue,
    source: ConfigurationSource
  ): ConfigurationTrace = {
    val prev = trace.entries.get(key)

    val history = prev match {
      case Some(res) =>
        res.history :+ res.copy()
      case None =>
        Nil
    }

    val resolution = ConfigurationResolution(
      key        = key,
      finalValue = value,
      origin     = source.origin,
      history    = history,
      sourceType = _source_metadata(source)._1,
      sourceId   = _source_metadata(source)._2
    )

    trace.copy(entries = trace.entries.updated(key, resolution))
  }

  private def _source_metadata(
    source: ConfigurationSource
  ): (Option[String], Option[String]) =
    source match {
      case resource: ResourceConfigurationSource =>
        (Some("resource"), Some(resource.resourceName))
      case file: ConfigurationSource.File =>
        (Some("file"), Some(file.path.toString))
      case _ =>
        (None, None)
    }
}
