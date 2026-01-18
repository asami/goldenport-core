package org.goldenport.configuration.source

import java.net.URL
import java.nio.file.Path

import scala.jdk.CollectionConverters.*
import scala.util.Using

import org.goldenport.Conclusion
import org.goldenport.Consequence
import org.goldenport.configuration.Configuration
import org.goldenport.configuration.ConfigurationValue
import org.goldenport.configuration.ConfigurationOrigin
import org.goldenport.configuration.source.file.FileConfigLoader
import org.goldenport.configuration.source.file.SimpleFileConfigLoader

/**
 * ConfigSource represents one configuration input source.
 *
 * This is an internal framework abstraction.
 * Concrete sources are defined as case classes under the companion object
 * to keep the public surface small and explicit.
 */
/*
 * @since   Dec. 18, 2025
 * @version Jan. 16, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait ConfigurationSource {
  def origin: ConfigurationOrigin
  def rank: Int
  def location: Option[String]

  def load(): Consequence[Configuration]
}

object ConfigurationSource {

  def home(): Option[ConfigurationSource] = {
    val home = sys.props.get("user.home").map(p => Path.of(p))
    home.map { base =>
      val path = base.resolve(".sie").resolve("config.conf")
      File(
        origin = ConfigurationOrigin.Home,
        path   = path,
        rank   = Rank.Home,
        loader = new SimpleFileConfigLoader
      )
    }
  }

  def project(cwd: Path): Option[ConfigurationSource] = {
    val path = cwd.resolve(".sie").resolve("config.conf")
    Some(
      File(
        origin = ConfigurationOrigin.Project,
        path   = path,
        rank   = Rank.Project,
        loader = new SimpleFileConfigLoader
      )
    )
  }

  def cwd(cwd: Path): Option[ConfigurationSource] = {
    val path = cwd.resolve(".sie").resolve("config.conf")
    Some(
      File(
        origin = ConfigurationOrigin.Cwd,
        path   = path,
        rank   = Rank.Cwd,
        loader = new SimpleFileConfigLoader
      )
    )
  }

  def env(env: Map[String, String]): Option[ConfigurationSource] =
    Some(Env(env, Rank.Environment))

  def args(args: Map[String, String]): Option[ConfigurationSource] =
    Some(Args(args, Rank.Arguments))

  object Rank {
    val Resource: Int    = 5
    val Home: Int        = 10
    val Project: Int     = 20
    val Cwd: Int         = 30
    val Environment: Int = 40
    val Arguments: Int   = 50
  }

  final case class File(
    origin: ConfigurationOrigin,
    path: Path,
    rank: Int,
    loader: FileConfigLoader
  ) extends ConfigurationSource {

    override def location: Option[String] = Some(path.toString)

    override def load(): Consequence[Configuration] =
      loader.load(path)
  }

  final case class Env(
    env: Map[String, String],
    rank: Int = Rank.Environment
  ) extends ConfigurationSource {

    override val origin: ConfigurationOrigin = ConfigurationOrigin.Environment
    override val location: Option[String] = None

    override def load(): Consequence[Configuration] =
      Consequence.success(
        Configuration(
          env.map { case (k, v) => k -> ConfigurationValue.StringValue(v) }
        )
      )
  }

  final case class Args(
    args: Map[String, String],
    rank: Int = Rank.Arguments
  ) extends ConfigurationSource {

    override val origin: ConfigurationOrigin = ConfigurationOrigin.Arguments
    override val location: Option[String] = None

    override def load(): Consequence[Configuration] =
      Consequence.success(
        Configuration(
          args.map { case (k, v) => k -> ConfigurationValue.StringValue(v) }
        )
      )
  }
}

final case class ResourceConfigurationSource(
  resourceName: String,
  resourceUrl: URL,
  override val rank: Int = ConfigurationSource.Rank.Resource
) extends ConfigurationSource {

  override val origin: ConfigurationOrigin = ConfigurationOrigin.Resource

  override def location: Option[String] =
    Some(resourceUrl.toExternalForm)

  override def load(): Consequence[Configuration] = {
    Using(scala.io.Source.fromInputStream(resourceUrl.openStream(), "UTF-8")) { source =>
      val values =
        source
          .getLines()
          .map(_.trim)
          .filterNot(_.isEmpty)
          .filterNot(_.startsWith("#"))
          .flatMap { line =>
            line.split("=", 2) match {
              case Array(k, v) =>
                Some(k.trim -> ConfigurationValue.StringValue(v.trim))
              case _ =>
                None
            }
          }
          .toMap

      Configuration(values)
    } match {
      case scala.util.Success(cfg) => Consequence.Success(cfg)
      case scala.util.Failure(exception) =>
        Consequence.Failure(Conclusion.from(exception))
    }
  }
}

object ResourceConfigurationSource {
  private val DefaultResourceNames = Seq("configuration.conf", "application.conf")

  def fromClasspath(
    names: Seq[String] = DefaultResourceNames,
    loader: ClassLoader = Option(Thread.currentThread().getContextClassLoader)
      .orElse(Option(getClass.getClassLoader))
      .getOrElse(ClassLoader.getSystemClassLoader)
  ): Seq[ConfigurationSource] =
    names.flatMap { name =>
      val resources = loader.getResources(name).asScala
      resources.map(url => new ResourceConfigurationSource(name, url))
    }
}
