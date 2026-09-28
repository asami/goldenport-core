val scala3Version = "3.3.8"

lazy val root = project
  .in(file("."))
  .settings(
    organization := "org.goldenport",
    name := "goldenport-core",
    version := "0.4.3-SNAPSHOT",

    scalaVersion := scala3Version,
    Compile / scalacOptions ++= Seq("-release:17", "-deprecation"),
    Compile / javacOptions ++= Seq("--release", "17"),

    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test",
    libraryDependencies += "org.typelevel" %% "cats-core" % "2.7.0",
    libraryDependencies += "org.typelevel" %% "cats-kernel-laws" % "2.7.0",
    libraryDependencies += "org.typelevel" %% "cats-free" % "2.7.0",
    libraryDependencies += "org.typelevel" %% "cats-effect" % "3.3.0",
    libraryDependencies += "org.typelevel" %% "kittens" % "3.5.0",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.10",
    libraryDependencies += "org.typelevel" %% "cats-testkit" % "2.7.0" % "test",
    libraryDependencies += "org.typelevel" %% "discipline-core" % "1.3.0" % "test",
    libraryDependencies += "org.typelevel" %% "discipline-scalatest" % "2.1.5" % "test",
    libraryDependencies += "org.typelevel" %% "spire" % "0.18.0",
    libraryDependencies += "io.circe" %% "circe-core" % "0.14.3",
    libraryDependencies += "io.circe" %% "circe-generic" % "0.14.3",
    libraryDependencies += "io.circe" %% "circe-parser" % "0.14.3",
    libraryDependencies += "com.typesafe" % "config" % "1.4.3",
    libraryDependencies += "org.yaml" % "snakeyaml" % "2.1",
    libraryDependencies += "com.fasterxml.jackson.dataformat" % "jackson-dataformat-toml" % "2.15.2",
    libraryDependencies += "org.apache.poi" % "poi-ooxml" % "5.2.5",
    publishMavenStyle := true,
    Compile / packageDoc / publishArtifact := false,

    publishTo := {
      val repo = sys.env.get("SIMPLEMODELING_MAVEN_LOCAL")
        .map(file)
        .getOrElse(baseDirectory.value / "maven-local")

      Some(
        Resolver.file(
          "local-simplemodeling-maven",
          repo
        )
      )
    }
  )
