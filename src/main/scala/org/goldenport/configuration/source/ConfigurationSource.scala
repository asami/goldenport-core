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
import org.goldenport.configuration.source.file.ConfigTextDecoder
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
 *  version Jan. 16, 2026
 * @version Mar. 24, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait ConfigurationSource {
  def origin: ConfigurationOrigin
  def rank: Int
  def location: Option[String]

  def load(): Consequence[Configuration]
}

object ConfigurationSource {
  val DefaultApplicationName = "simplemodeling"
  private val ConfigFileExtensions = Seq("conf", "props", "properties", "json", "yaml")

  def home(applicationname: String = DefaultApplicationName): Seq[ConfigurationSource] = {
    val home = sys.props.get("user.home").map(p => Path.of(p))
    home.map(base => _config_files(base.resolve(_config_dir_name(applicationname)), ConfigurationOrigin.Home, Rank.Home))
      .getOrElse(Nil)
  }

  def project(
    cwd: Path,
    applicationname: String = DefaultApplicationName
  ): Seq[ConfigurationSource] =
    _config_files(cwd.resolve(_config_dir_name(applicationname)), ConfigurationOrigin.Project, Rank.Project)

  def cwd(
    cwd: Path,
    applicationname: String = DefaultApplicationName
  ): Seq[ConfigurationSource] =
    _config_files(cwd.resolve(_config_dir_name(applicationname)), ConfigurationOrigin.Cwd, Rank.Cwd)

  def env(
    env: Map[String, String],
    applicationname: String = DefaultApplicationName
  ): Option[ConfigurationSource] =
    Some(Env(_normalize_env(env, applicationname), Rank.Environment))

  def args(args: Map[String, String]): Option[ConfigurationSource] =
    Some(Args(args, Rank.Arguments))

  private def _config_files(
    base: Path,
    origin: ConfigurationOrigin,
    rank: Int
  ): Seq[ConfigurationSource] =
    ConfigFileExtensions.map { ext =>
      File(
        origin = origin,
        path = base.resolve(s"config.$ext"),
        rank = rank,
        loader = new SimpleFileConfigLoader
      )
    }

  private def _config_dir_name(applicationname: String): String = {
    val a = applicationname.trim
    val name = if (a.startsWith(".")) a.drop(1) else a
    if (name.isEmpty) s".$DefaultApplicationName" else s".$name"
  }

  private def _normalize_env(
    env: Map[String, String],
    applicationname: String
  ): Map[String, String] = {
    val app = _application_name(applicationname)
    val prefix = s"${app.toUpperCase}_"
    env.collect {
      case (k, v) if k.startsWith(prefix) && k.length > prefix.length =>
        _to_canonical_key(k) -> v
    }
  }

  private def _application_name(applicationname: String): String = {
    val a = applicationname.trim
    val name = if (a.startsWith(".")) a.drop(1) else a
    if (name.isEmpty) DefaultApplicationName else name
  }

  private def _to_canonical_key(envkey: String): String =
    envkey.toLowerCase.replace('_', '.')

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
      val content = source.mkString
      ConfigTextDecoder.decode(resourceName, content) match {
        case Consequence.Success(cfg) => cfg
        case Consequence.Failure(err) => throw new IllegalStateException(err.print)
      }
    } match {
      case scala.util.Success(cfg) => Consequence.Success(cfg)
      case scala.util.Failure(exception) =>
        Consequence.Failure(Conclusion.from(exception))
    }
  }
}

object ResourceConfigurationSource {
  private val DefaultResourceNames = Seq(
    "configuration.conf",
    "configuration.props",
    "configuration.properties",
    "configuration.json",
    "configuration.yaml",
    "application.conf",
    "application.props",
    "application.properties",
    "application.json",
    "application.yaml"
  )

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
