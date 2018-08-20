package io.github.martinhh.sl

import java.io.File
import java.nio.file.Path

import cats.effect.Effect
import fs2.io

import scala.concurrent.ExecutionContext
import scala.language.higherKinds


/**
 * Extracts the audio file paths from a Serato .crate file.
 */
object CrateExtractor {

  def filePathsStream[F[_]](path: Path)(implicit F: Effect[F], ec: ExecutionContext): fs2.Stream[F, String] =
    io.file.readAllAsync(path, 4096).through(PathsFromBytes.pipe[F])

  val CrateSuffixRegex = """\.[cC][rR][aA][tT][eE]$"""
  val CrateFileRegex = s""".*$CrateSuffixRegex"""

  def getCrateFiles(parentDir: String, matchRegex: Option[String]): Array[String] = {
    def matches(string: String): Boolean =
      string.matches(CrateFileRegex) && matchRegex.fold(true)(string.matches)

    val file = new File(parentDir)
    if (file.exists() && file.isDirectory) {
      val crateFiles = file.listFiles.collect {
        case f if !f.isDirectory && matches(f.getName) => f.getName
      }
      crateFiles
    } else {
      Array.empty
    }
  }

  def getSimpleNameWithoutCrateSuffix(file: String): String = {
    new File(file).getName.replaceAll(CrateSuffixRegex, "")
  }
}
