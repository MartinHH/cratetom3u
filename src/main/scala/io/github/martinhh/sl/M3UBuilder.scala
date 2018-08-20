package io.github.martinhh.sl

import java.io.File

import fs2.Pipe

import scala.language.higherKinds


object M3UBuilder {

  private val HeaderString = "#EXTM3U"

  def filePathsToM3U[F[_]](config: M3UConfig): Pipe[F, String, String] = in => {
    val converted = in.map { audioFilePath =>
      val removed = config.remove.fold(audioFilePath)(r => audioFilePath.replaceFirst(r, ""))
      val prepended = config.prepend.fold(removed)(_ + removed)
      if (config.backslash) prepended.replace('/', '\\') else prepended
    }
    fs2.Stream(HeaderString) ++ converted
  }

  /**
   * Conversion-parameters for [[filePathsToM3U()]]
   *
   * @param remove      An optional [[String]] whose first match is removed from each
   *                    audio file path.
   * @param prepend     An optional [[String]] that is prepended to each audio file
   *                    path.
   * @param charSetName Optional name of the charset to be used for writing the m3u file.
   * @param backslash   If true, all '/'s in audio file paths will be replaced b '\'s.
   */
  case class M3UConfig(remove: Option[String], prepend: Option[String], charSetName: Option[String], backslash: Boolean)

}
