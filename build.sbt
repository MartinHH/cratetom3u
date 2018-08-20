
name := "CrateToM3U"

organization := "io.github.martinhh"

version := "0.2.1"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
   "org.rogach" %% "scallop" % "2.0.7",

   "org.scalatest" %% "scalatest" % "3.0.1" % "test",

   "org.typelevel" %% "cats-kernel" % "1.1.0",
   "org.typelevel" %% "cats-effect" % "0.10.1",

   "co.fs2" %% "fs2-core" % "0.10.4",
   "co.fs2" %% "fs2-io" % "0.10.4"
)

enablePlugins(JavaAppPackaging)

lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "io.github.martinhh.sl"
  )