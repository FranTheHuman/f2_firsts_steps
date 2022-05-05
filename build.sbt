ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.2"

lazy val root = (project in file("."))
  .settings(
    name := "fs2_firsts_steps"
  )

val Fs2Version = "3.2.7"
libraryDependencies += "co.fs2" %% "fs2-core" % Fs2Version