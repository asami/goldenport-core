package org.goldenport.process

import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal
import scala.util.Using
import scala.io.Source
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import org.goldenport.Consequence

/*
 * @since   Feb.  5, 2026
 * @version Feb.  5, 2026
 * @author  ASAMI, Tomoharu
 */
trait ExternalCommandExecutor {
  def execute(command: ExternalCommand): Consequence[ExternalCommandResult]
}

final case class ExternalCommand(
  command: Vector[String],
  workDir: Option[Path] = None,
  env: Map[String, String] = Map.empty
)

final case class ExternalCommandResult(
  exitCode: Int,
  stdout: String,
  stderr: String
)

final class LocalExternalCommandExecutor extends ExternalCommandExecutor {
  override def execute(command: ExternalCommand): Consequence[ExternalCommandResult] = {
    try {
      val builder = new ProcessBuilder(command.command.asJava)
      command.workDir.foreach(path => builder.directory(path.toFile))
      builder.environment().putAll(command.env.asJava)

      val process = builder.start()
      val exitCode = process.waitFor()
      val stdout = _readStream(process.getInputStream)
      val stderr = _readStream(process.getErrorStream)
      Consequence.success(ExternalCommandResult(exitCode, stdout, stderr))
    } catch {
      case NonFatal(e) =>
        Consequence.failure(e)
    }
  }

  private def _readStream(stream: java.io.InputStream): String =
    Using.resource(Source.fromInputStream(stream, StandardCharsets.UTF_8.name()))(_.mkString)
}
