package org.goldenport.process

import cats.*
import cats.syntax.all.*
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal
import java.util.concurrent.{Callable, ExecutionException, FutureTask}
import java.nio.file.Path
import org.goldenport.Consequence
import org.goldenport.bag.Bag
import org.goldenport.datatype.FileContent
import org.goldenport.observation.{Cause, Descriptor}
import org.goldenport.vfs.FileSystemView
import org.goldenport.vfs.DirectoryFileSystemView

/*
 * @since   Feb.  5, 2026
 *  version Feb.  6, 2026
 * @version Aug. 10, 2026
 * @author  ASAMI, Tomoharu
 */
trait ShellCommandExecutor {
  def execute(command: ShellCommand): Consequence[ShellCommandResult]
}

final case class ShellCommand(
  command: Vector[String],
  workDir: Option[Path] = None,
  env: Map[String, String] = Map.empty,
  directive: ShellCommand.Directive = Directive.empty
)
object ShellCommand {
  final case class Directive(
    files: Vector[Directive.Rule] = Vector.empty,
    directories: Vector[Directive.Rule] = Vector.empty
  )
  object Directive {
    val empty = Directive()

    case class Rule(name: String, path: Path)
  }
}

final case class ShellCommandResult(
  exitCode: Int,
  stdout: Bag,
  stderr: Bag,
  files: Map[String, FileContent],
  directories: Map[String, FileSystemView]
)

final class LocalShellCommandExecutor private[process] (
  private val _process_starter: ShellCommand => Process
) extends ShellCommandExecutor {
  def this() = this(ShellCommandExecutor._start)

  override def execute(command: ShellCommand): Consequence[ShellCommandResult] = {
    var process: Process = null
    var stdouttask: FutureTask[Consequence[Bag]] = null
    var stderrtask: FutureTask[Consequence[Bag]] = null
    try {
      process = _process_starter(command)
      stdouttask = _drain_task(process.getInputStream)
      stderrtask = _drain_task(process.getErrorStream)
      _start_drain("shell-command-stdout-drain", stdouttask)
      _start_drain("shell-command-stderr-drain", stderrtask)
      val exitcode = process.waitFor()
      val result = for {
        stdout <- _await_drain(stdouttask)
        stderr <- _await_drain(stderrtask)
        files <- Consequence {
          val base = command.workDir
          command.directive.files.map { rule =>
            val resolved = base.map(_.resolve(rule.path)).getOrElse(rule.path)
            rule.name -> FileContent.create(resolved)
          }.toMap
        }
        dirs <- Consequence {
          val base = command.workDir
          command.directive.directories.map { rule =>
            val resolved = base.map(_.resolve(rule.path)).getOrElse(rule.path)
            rule.name -> DirectoryFileSystemView(resolved)
          }.toMap
        }
      } yield ShellCommandResult(exitcode, stdout, stderr, files, dirs)
      result.flatMap(_classify_docker_daemon_unavailable(command, _))
    } catch {
      case e: InterruptedException =>
        _interrupt_cleanup(process, stdouttask, stderrtask)
        Thread.currentThread().interrupt()
        throw e
      case NonFatal(e) =>
        Cause.interruption(e) match {
          case Some(interrupted) =>
            _interrupt_cleanup(process, stdouttask, stderrtask)
            Thread.currentThread().interrupt()
            throw interrupted
          case None =>
            Cause.availabilityKind(e) match {
              case Some(_) =>
                Consequence.networkUnavailable(
                  _availability_message(e, command),
                  _command_endpoint(command),
                  e,
                  Seq(Descriptor.Facet.Component("shell-command-executor"))
                )
              case None =>
                Consequence {
                  throw e
                }
            }
        }
    }
  }

  private def _drain_task(stream: java.io.InputStream): FutureTask[Consequence[Bag]] =
    new FutureTask(new Callable[Consequence[Bag]] {
      override def call(): Consequence[Bag] = Bag.create(stream)
    })

  private def _start_drain(name: String, task: FutureTask[Consequence[Bag]]): Unit = {
    val thread = new Thread(task, name)
    thread.setDaemon(true)
    thread.start()
  }

  private def _await_drain(task: FutureTask[Consequence[Bag]]): Consequence[Bag] =
    try task.get()
    catch {
      case e: ExecutionException => throw e.getCause
    }

  private def _interrupt_cleanup(
    process: Process,
    stdouttask: FutureTask[Consequence[Bag]],
    stderrtask: FutureTask[Consequence[Bag]]
  ): Unit = {
    Option(process).foreach { p =>
      _ignore_nonfatal(p.destroy())
      if (p.isAlive)
        _ignore_nonfatal(p.destroyForcibly())
      _ignore_nonfatal(p.getInputStream.close())
      _ignore_nonfatal(p.getErrorStream.close())
      _ignore_nonfatal(p.getOutputStream.close())
    }
    Option(stdouttask).foreach(_.cancel(true))
    Option(stderrtask).foreach(_.cancel(true))
  }

  private def _ignore_nonfatal(body: => Unit): Unit =
    try body
    catch { case NonFatal(_) => () }

  private def _classify_docker_daemon_unavailable(
    command: ShellCommand,
    result: ShellCommandResult
  ): Consequence[ShellCommandResult] =
    if (!_is_docker_command(command) || result.exitCode == 0)
      Consequence.success(result)
    else
      result.stdout.asString().zip(result.stderr.asString()).flatMap {
        case (stdout, stderr) =>
          _docker_daemon_diagnostic(stdout, stderr) match {
            case Some(diagnostic) =>
              Consequence.serviceUnavailable(
                s"Docker daemon is unavailable: ${diagnostic}",
                Cause.Kind.NotRunning,
                Seq(
                  Descriptor.Facet.Endpoint(_docker_endpoint(command)),
                  Descriptor.Facet.Service("docker-daemon"),
                  Descriptor.Facet.Component("docker")
                )
              )
            case None =>
              Consequence.success(result)
          }
      }

  private def _is_docker_command(command: ShellCommand): Boolean =
    _command_basename(command).contains("docker")

  private def _command_basename(command: ShellCommand): Option[String] =
    command.command.headOption.map(_.replace('\\', '/').split('/').lastOption.getOrElse(""))

  private def _command_endpoint(command: ShellCommand): String =
    _command_basename(command).filter(_.nonEmpty).fold("command://unknown")(x => s"command://${x}")

  private def _docker_endpoint(command: ShellCommand): String =
    command.env.get("DOCKER_HOST").map(_.trim).filter(_.nonEmpty).getOrElse("docker://daemon")

  private def _docker_daemon_diagnostic(stdout: String, stderr: String): Option[String] = {
    val diagnostic = Vector(stdout, stderr).filter(_.nonEmpty).mkString("\n")
    val normalized = diagnostic.toLowerCase(java.util.Locale.ROOT)
    val unavailable = Vector(
      "cannot connect to the docker daemon",
      "is the docker daemon running",
      "docker daemon is not running"
    ).exists(normalized.contains)
    Option.when(unavailable)(diagnostic.trim.take(512))
  }

  private def _availability_message(e: Throwable, command: ShellCommand): String =
    Option(e.getMessage).filter(_.nonEmpty).getOrElse(
      s"Unable to start ${_command_basename(command).getOrElse("command")}"
    )
}

object ShellCommandExecutor {
  private[process] def forProcessStarter(
    processstarter: ShellCommand => Process
  ): LocalShellCommandExecutor =
    new LocalShellCommandExecutor(processstarter)

  private[process] def _start(command: ShellCommand): Process = {
    val builder = new ProcessBuilder(command.command.asJava)
    command.workDir.foreach(path => builder.directory(path.toFile))
    builder.environment().putAll(command.env.asJava)
    builder.start()
  }
}
