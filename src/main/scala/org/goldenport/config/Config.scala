package org.goldenport.config

/*
 * @since   Dec. 30, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
object Config {
  enum Layer {
    case Base
    case User
    case Profile
    case Override
    case Runtime
    case Test
  }

  object Priority {
    val Base: Int = 10
    val User: Int = 15
    val Profile: Int = 30
    val Override: Int = 40
    val Runtime: Int = 50
    val Test: Int = 60
  }

  final case class Source(
    layer: Layer,
    name: String,
    origin: Option[String],
    priority: Int
  )

  final case class Value[A](
    value: A,
    source: Source
  )

  def resolve[A](values: Seq[Value[A]]): Value[A] =
    values.maxBy(_.source.priority)

  def explain(
    core: Core,
    resolved: Map[String, Seq[Value[String]]]
  ): String = {
    val keys = resolved.keys.toList.sorted
    val lines = keys.flatMap { key =>
      val effectivevalue = _core_value(core, key).getOrElse("")
      val candidates = resolved.getOrElse(key, Seq.empty).toList
      val winning = if (candidates.nonEmpty) Some(resolve(candidates)) else None
      val overridden = winning match {
        case Some(w) =>
          _sorted_candidates(candidates.filterNot(_eq_value(_, w)))
        case None =>
          _sorted_candidates(candidates)
      }

      val effective = winning.map(_render_source).getOrElse("unknown")
      val header = s"${key} = ${effectivevalue}"
      val effline = s"  effective: ${effective}"
      val overriddenlines =
        if (overridden.nonEmpty) {
          "  overridden:" :: overridden.map(c => s"    - ${_render_source(c)}")
        } else {
          Nil
        }
      header :: effline :: overriddenlines
    }
    lines.mkString("\n")
  }

  private def _eq_value[A](a: Value[A], b: Value[A]): Boolean =
    a.source == b.source && a.value == b.value

  private def _sorted_candidates(values: List[Value[String]]): List[Value[String]] =
    values.sortBy { v =>
      (
        -v.source.priority,
        v.source.layer.toString,
        v.source.name,
        v.source.origin.getOrElse("")
      )
    }

  private def _render_source(value: Value[String]): String =
    value.source.origin match {
      case Some(origin) => s"${value.source.layer} ${value.source.name} (${origin})"
      case None => s"${value.source.layer} ${value.source.name}"
    }

  private def _core_value(core: Core, key: String): Option[String] =
    key match {
      case "locale" => Some(core.locale)
      case "timezone" => Some(core.timezone)
      case "encoding" => Some(core.encoding)
      case "mathContext" => Some(core.mathContext)
      case "random" => Some(core.random)
      case "logging" => Some(core.logging)
      case _ => None
    }

  final case class Core(
    locale: String,
    timezone: String,
    encoding: String,
    mathContext: String,
    random: String,
    logging: String
  )

  trait Loader {
    def load(argOverrides: Map[String, String] = Map.empty): Core
  }

  object Defaults {
    def base: Core =
      Core(
        locale = "en",
        timezone = "UTC",
        encoding = "UTF-8",
        mathContext = "DECIMAL64",
        random = "deterministic",
        logging = "console"
      )
  }

  final class DefaultLoader(
    baseconfig: Core = Defaults.base,
    userconfig: Option[Core] = None,
    profileconfig: Option[Core] = None,
    overrideconfig: Option[Core] = None,
    runtimeconfig: Option[Core] = None,
    testconfig: Option[Core] = None
  ) extends Loader {
    def load(argOverrides: Map[String, String] = Map.empty): Core =
      _merge(
        _base_values(baseconfig) ++
          _file_values(Layer.User, Priority.User, _home_config_file) ++
          _file_values(Layer.Profile, Priority.Profile, _project_config_file) ++
          _file_values(Layer.Override, Priority.Override, _current_config_file) ++
          _core_values(Layer.Runtime, Priority.Runtime, Some("runtime"), runtimeconfig) ++
          _arg_values(argOverrides) ++
          _core_values(Layer.Test, Priority.Test, Some("test"), testconfig)
      )

    private case class KeyValue(
      key: String,
      value: Value[String]
    )

    private def _merge(values: List[KeyValue]): Core = {
      val locale = resolve(_filter(values, "locale")).value
      val timezone = resolve(_filter(values, "timezone")).value
      val encoding = resolve(_filter(values, "encoding")).value
      val mathcontext = resolve(_filter(values, "mathContext")).value
      val random = resolve(_filter(values, "random")).value
      val logging = resolve(_filter(values, "logging")).value

      Core(
        locale = locale,
        timezone = timezone,
        encoding = encoding,
        mathContext = mathcontext,
        random = random,
        logging = logging
      )
    }

    private def _filter(values: List[KeyValue], key: String): List[Value[String]] =
      values.collect { case KeyValue(`key`, v) => v }

    private def _base_values(core: Core): List[KeyValue] =
      _core_values(Layer.Base, Priority.Base, None, Some(core))

    private def _core_values(layer: Layer, priority: Int, origin: Option[String], core: Option[Core]): List[KeyValue] =
      core.map(c => _core_to_values(layer, priority, origin, c)).getOrElse(Nil)

    private def _core_to_values(layer: Layer, priority: Int, origin: Option[String], core: Core): List[KeyValue] =
      List(
        _key_value(layer, priority, "locale", core.locale, origin, origin.getOrElse("defaults")),
        _key_value(layer, priority, "timezone", core.timezone, origin, origin.getOrElse("defaults")),
        _key_value(layer, priority, "encoding", core.encoding, origin, origin.getOrElse("defaults")),
        _key_value(layer, priority, "mathContext", core.mathContext, origin, origin.getOrElse("defaults")),
        _key_value(layer, priority, "random", core.random, origin, origin.getOrElse("defaults")),
        _key_value(layer, priority, "logging", core.logging, origin, origin.getOrElse("defaults"))
      )

    private def _file_values(layer: Layer, priority: Int, file: Option[java.nio.file.Path]): List[KeyValue] =
      file.toList.flatMap(p => _read_properties(p, layer, priority))

    private def _arg_values(argOverrides: Map[String, String]): List[KeyValue] = {
      val priority = Priority.Runtime + 5
      argOverrides.toList.collect {
        case (key, value) if value != null && value.nonEmpty =>
          _key_value(Layer.Runtime, priority, key, value, Some(s"--${key}"), "args")
      }
    }

    private def _read_properties(path: java.nio.file.Path, layer: Layer, priority: Int): List[KeyValue] = {
      if (!java.nio.file.Files.isRegularFile(path)) {
        Nil
      } else {
        val props = new java.util.Properties()
        var ins: java.io.InputStream = null
        try {
          ins = java.nio.file.Files.newInputStream(path)
          props.load(ins)
          val origin = Some(path.toString)
          props.stringPropertyNames().toArray(new Array[String](0)).toList.map { key =>
            val value = props.getProperty(key)
            _key_value(layer, priority, key, value, origin, "goldenport.conf")
          }
        } catch {
          case _: Exception => Nil
        } finally {
          if (ins != null) {
            ins.close()
          }
        }
      }
    }

    private def _key_value(
      layer: Layer,
      priority: Int,
      key: String,
      value: String,
      origin: Option[String],
      sourceName: String
    ): KeyValue =
      KeyValue(
        key,
        Value(
          value,
          Source(
            layer = layer,
            name = sourceName,
            origin = origin,
            priority = priority
          )
        )
      )

    private def _home_config_file: Option[java.nio.file.Path] =
      _home_dir.map(_.resolve(".goldenport.conf"))

    private def _project_config_file: Option[java.nio.file.Path] =
      _project_root.map(_.resolve("goldenport.conf"))

    private def _current_config_file: Option[java.nio.file.Path] =
      Some(_current_dir.resolve("goldenport.conf"))

    private def _home_dir: Option[java.nio.file.Path] =
      Option(System.getProperty("user.home")).map(java.nio.file.Paths.get(_))

    private def _current_dir: java.nio.file.Path =
      java.nio.file.Paths.get(".").toAbsolutePath.normalize

    private def _project_root: Option[java.nio.file.Path] = {
      def _loop_(dir: java.nio.file.Path): Option[java.nio.file.Path] = {
        if (java.nio.file.Files.isDirectory(dir.resolve(".git"))) {
          Some(dir)
        } else {
          Option(dir.getParent) match {
            case Some(parent) => _loop_(parent)
            case None => None
          }
        }
      }

      _loop_(_current_dir)
    }
  }
}
