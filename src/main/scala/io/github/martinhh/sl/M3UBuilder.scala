package io.github.martinhh.sl

import java.io.File

/**
  * Writes `.m3u`-files (based on the raw audio file paths extracted by `CrateExtractor`).
  */
object M3UBuilder:

  private val HeaderString = "#EXTM3U"

  /**
    * Non-filepath-parameters for `writeToFile()`.
    *
    * @param remove         An optional `String` whose first match is removed from each
    *                       audio file path.
    * @param prepend        An optional `String` that is prepended to each audio file
    *                       path.
    * @param charSetName    Optional name of the charset to be used for writing the m3u file.
    * @param backslash      If true, all '/'s in audio file paths will be replaced b '\'s.
    */
  case class M3UConfig(
    remove: Option[String],
    prepend: Option[String],
    charSetName: Option[String],
    backslash: Boolean
  )

  def writeToFile(
    path: String,
    audioFilePaths: Iterable[String],
    config: M3UConfig
  ): Boolean =
    writeToFile(new File(path), audioFilePaths, config)

  def writeToFile(
    dir: String,
    name: String,
    audioFilePaths: Iterable[String],
    config: M3UConfig
  ): Boolean =
    val dirFile = new File(dir)
    if (!dirFile.exists())
      dirFile.mkdir()
    writeToFile(new File(dir, name), audioFilePaths, config)

  /**
    * Creates an m3u file.
    *
    * @param file           The file that is to be created.
    * @param audioFilePaths List of audio file paths for the m3u content. Typically created
    *                       via `CrateExtractor.audioFilePathsFromCrateFile(pathToCrateFile)`.
    * @param config         Additional parameters.
    * @return               True if there was an error during file-writing. (This is the
    *                       error flag of the underlying [[java.io.PrintWriter]].)
    */
  def writeToFile(
    file: File,
    audioFilePaths: Iterable[String],
    config: M3UConfig
  ): Boolean =

    import java.io.*
    import config.*
    val pw = charSetName.fold(new PrintWriter(file))(new PrintWriter(file, _))

    pw.println(HeaderString)

    audioFilePaths.foreach { audioFilePath =>
      val removed = remove.fold(audioFilePath)(r => audioFilePath.replaceFirst(r, ""))
      val prepended = prepend.fold(removed)(_ + removed)
      val backslashed = if (config.backslash) prepended.replace('/', '\\') else prepended
      pw.println(backslashed)
    }

    pw.close()
    pw.checkError()
  end writeToFile
