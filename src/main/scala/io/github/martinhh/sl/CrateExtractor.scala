package io.github.martinhh.sl

import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}


/**
  * Extracts the audio file paths from a Serato .crate file.
  */
object CrateExtractor {

  case class CrateExtractionError(msg: String) extends Throwable

  // The start of an audio file path is marked by these 4 bytes + 4 subsequent bytes
  // that contain the length of the audio file path.
  private val StartMarker: Array[Byte] = "ptrk".map(_.toByte).toArray
  private val PathLengthOffset = 4
  private val StartMarkerFullLength = StartMarker.length + PathLengthOffset

  /** Check that bytes contains subSet at idx. */
  private def hasEqualBytesAt(idx: Int, bytes: Array[Byte], subSet: Array[Byte]): Boolean = {
    idx < bytes.length - subSet.length && subSet.indices.forall(i => bytes(idx + i) == subSet(i))
  }

  def audioFilePathsFromCrateFile(dirPath: String, fileName: String): List[String] = {
    audioFilePathsFromCrateFile(Paths.get(dirPath, fileName))
  }

  def audioFilePathsFromCrateFile(pathToCrateFile: String): List[String] = {
    audioFilePathsFromCrateFile(Paths.get(pathToCrateFile))
  }

  def audioFilePathsFromCrateFile(pathToCrateFile: Path): List[String] = {
    val bytesOfFile = Files.readAllBytes(pathToCrateFile)
    val bytesLength = bytesOfFile.length

    var i = 0
    var results = List.empty[String]

    /** Create String from bytesOfFile starting at i. */
    def bytesToString(size: Int): String = {
      new java.lang.String(bytesOfFile, i, size, StandardCharsets.UTF_16)
    }

    while(i < bytesLength - StartMarkerFullLength) {

      // search for a startMarker:
      if(hasEqualBytesAt(i, bytesOfFile, StartMarker)) {
        // skip marker
        i += StartMarker.length

        // the next 4 bytes indicate the length of the audio file path
        val pathSize = ByteBuffer.wrap(bytesOfFile, i, 4).getInt

        i += PathLengthOffset

        if(pathSize > 10000 || pathSize <= 0)
          throw CrateExtractionError(s"Unexpected path size (pathSize=$pathSize)")

        if(i + pathSize > bytesLength)
          throw CrateExtractionError(s"Path size out of bounds (pathSize=$pathSize, bytesLength=$bytesLength, idx=$i)")

        // add audio file path to results:
        results ::= bytesToString(pathSize)

        i += pathSize
      }

      i += 1
    }

    results.reverse
  }

  val CrateSuffixRegex = """\.[cC][rR][aA][tT][eE]$"""
  val CrateFileRegex = s""".*$CrateSuffixRegex"""

  def getCrateFiles(parentDir: String): Array[String] = {
    val file = new File(parentDir)
    if(file.exists() && file.isDirectory) {
      val crateFiles = file.list().filter(_.matches(CrateFileRegex))
      crateFiles
    } else {
      Array.empty
    }
  }

  def getSimpleNameWithoutCrateSuffix(file: String): String = {
    new File(file).getName.replaceAll(CrateSuffixRegex, "")
  }
}
