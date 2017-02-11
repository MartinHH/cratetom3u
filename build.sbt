
name := "CrateToM3U"

organization := "io.github.martinhh"

version := "0.1.0"

scalaVersion := "2.12.1"

libraryDependencies += "org.rogach" %% "scallop" % "2.0.7"

enablePlugins(JavaAppPackaging)

lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "io.github.martinhh.sl"
  )