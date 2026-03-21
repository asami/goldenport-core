package org.goldenport.configuration.source

import java.nio.file.{Files, Path}

import scala.annotation.tailrec

/** Canonical upward project-root detection for core configuration. */
/*
 * @since   May. 21, 2025
 * @version Mar. 21, 2026
 * @author  ASAMI, Tomoharu
 */
object ProjectRootFinder {
  def find(
    cwd: Path,
    applicationname: String = ConfigurationSource.DefaultApplicationName
  ): Option[Path] = {
    val targetdir = _config_dir_name(applicationname)

    @tailrec
    def _loop_(dir: Path): Option[Path] = {
      if (_exists_dir_(dir.resolve(targetdir)))
        Some(dir)
      else if (_exists_dir_(dir.resolve(".git")))
        Some(dir)
      else
        Option(dir.getParent) match {
          case Some(parent) => _loop_(parent)
          case None => None
        }
    }

    _loop_(cwd.toAbsolutePath.normalize())
  }

  private def _exists_dir_(path: Path): Boolean =
    Files.exists(path) && Files.isDirectory(path)

  private def _config_dir_name(applicationname: String): String = {
    val a = applicationname.trim
    val name = if (a.startsWith(".")) a.drop(1) else a
    if (name.isEmpty)
      s".${ConfigurationSource.DefaultApplicationName}"
    else
      s".$name"
  }
}
