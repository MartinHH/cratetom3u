
name := "CrateToM3U"

organization := "io.github.martinhh"

version := "0.2.3"

scalaVersion := "3.1.0"

libraryDependencies += "org.rogach" %% "scallop" % "4.1.0"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.9" % "test"
libraryDependencies += "org.scalatest" %% "scalatest-funsuite" % "3.2.9" % "test"

scalacOptions ++= Seq(
  "-deprecation",
  // TODO: reactivate if this ever gets supported by scala 3
  // "-Wunused:imports",
  "-Xfatal-warnings"
)

enablePlugins(JavaAppPackaging)

lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "io.github.martinhh.sl"
  )