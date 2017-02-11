package io.github.martinhh.sl


object M3UBuilder {

  private val HeaderString = "#EXTM3U"


  /**
    * Creates an m3u file.
    *
    * @param path           The path of the file that is to be created.
    * @param audioFilePaths List of audio file paths for the m3u content. Typically created
    *                       via [[CrateExtractor.audioFilePathsFromCrateFile(pathToCrateFile)]]
    * @param remove         An optional [[String]] whose first match is removed from each
    *                       audio file path.
    * @param prepend        An optional [[String]] that is prepended to each audio file
    *                       path.
    * @param charSetName    The name of the charset to be used for writing the m3u file.
    * @return               True if there was an error during file-writing. (This is the
    *                       error flag of the underlying [[java.io.PrintWriter]].)
    */
  def writeToFile(path: String, audioFilePaths: Traversable[String],
                  remove: Option[String], prepend: Option[String],
                  charSetName: String): Boolean = {

    import java.io._
    val pw = new PrintWriter(new File(path), charSetName)

    pw.println(HeaderString)

    audioFilePaths.foreach { audioFilePath =>
      val removed = remove.fold(audioFilePath)(r => audioFilePath.replaceFirst(r, ""))
      val prepended = prepend.fold(removed)(_ + removed)
      pw.println(prepended)
    }

    pw.close()
    pw.checkError()

  }
}
