
name := "CrateToM3U"

organization := "io.github.martinhh"

version := "0.2.3"

scalaVersion := "2.13.5"

libraryDependencies += "org.rogach" %% "scallop" % "4.0.2"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.7" % "test"
libraryDependencies += "org.scalatest" %% "scalatest-funsuite" % "3.2.7" % "test"

scalacOptions ++= Seq(
  "-deprecation",
  "-Xfatal-warnings",
  "-Wunused:imports"
)

enablePlugins(JavaAppPackaging)

lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "io.github.martinhh.sl"
  )