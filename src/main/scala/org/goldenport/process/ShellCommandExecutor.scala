package org.goldenport.process

import cats.*
import cats.syntax.all.*
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal
import scala.util.Using
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.file.Path
import org.goldenport.Consequence
import org.goldenport.bag.Bag
import org.goldenport.datatype.FileContent
import org.goldenport.vfs.FileSystemView
import org.goldenport.vfs.DirectoryFileSystemView

/*
 * @since   Feb.  5, 2026
 * @version Feb.  6, 2026
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

final class LocalShellCommandExecutor extends ShellCommandExecutor {
  override def execute(command: ShellCommand): Consequence[ShellCommandResult] = {
    Consequence run {
      val builder = new ProcessBuilder(command.command.asJava)
      command.workDir.foreach(path => builder.directory(path.toFile))
      builder.environment().putAll(command.env.asJava)

      val process = builder.start()
      val exitCode = process.waitFor()
      for {
        stdout <- Bag.create(process.getInputStream)
        stderr <- Bag.create(process.getErrorStream)
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
      } yield ShellCommandResult(exitCode, stdout, stderr, files, dirs)
    }
  }
}
