package org.goldenport.configuration.source.file

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

import org.goldenport.Consequence
import org.goldenport.configuration.Configuration
import org.goldenport.configuration.ConfigurationValue

trait FileConfigLoader {
  def load(
    path: Path
  ): Consequence[Configuration]
}

final class SimpleFileConfigLoader
  extends FileConfigLoader {

  override def load(
    path: Path
  ): Consequence[Configuration] = {

    if (!Files.exists(path))
      Consequence.Success(Configuration.empty)
    else {
      val lines = Files.readAllLines(path).asScala

      val values =
        lines
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

      Consequence.Success(
        Configuration(values)
      )
    }
  }
}
