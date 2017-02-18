
name := "CrateToM3U"

organization := "io.github.martinhh"

version := "0.2.1"

scalaVersion := "2.11.8"

libraryDependencies += "org.rogach" %% "scallop" % "2.0.7"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

enablePlugins(JavaAppPackaging)

lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "io.github.martinhh.sl"
  )