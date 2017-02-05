package io.github.martinhh.sl

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}


/**
  * Extracts the audio file paths from a Serato .crate file.
  */
object CrateExtractor {

  // start of a audio file path is marked by these 4 bytes + 4 subsequent bytes:
  private val StartMarker: Array[Byte] = "ptrk".map(_.toByte).toArray
  private val StartMarkerAdditionalOffset = 4
  private val StartMarkerFullLength = StartMarker.length + StartMarkerAdditionalOffset

  // end of a audio file path is marked by these 4 bytes:
  private val EndMarker: Array[Byte] = "otrk".map(_.toByte).toArray

  private def hasEqualBytesAt(idx: Int, bytes: Array[Byte], subSet: Array[Byte]): Boolean = {
    idx < bytes.length - subSet.length && subSet.indices.forall(i => bytes(idx + i) == subSet(i))
  }

  private def bytesToString(stringBytes: Array[Byte]): String = {
    new java.lang.String(stringBytes, 0, stringBytes.length, StandardCharsets.UTF_16)
  }

  def audioFilePathsFromCrateFile(pathToCrateFile: String): List[String] = {
    val bytesOfFile = Files.readAllBytes(Paths.get(pathToCrateFile))

    var i = 0
    var results = List.empty[String]

    while(i < bytesOfFile.size - StartMarkerFullLength) {

      // search for a startMarker:
      if(hasEqualBytesAt(i, bytesOfFile, StartMarker)) {
        // skip marker_
        i += StartMarkerFullLength

        // copy subsequent bytes until end of file or until endMarker:
        var pathBytes = List.empty[Byte]
        while(i < bytesOfFile.size && !hasEqualBytesAt(i, bytesOfFile, EndMarker)){
          pathBytes ::= bytesOfFile(i)
          i += 1
        }

        // add pathString to results:
        results ::= bytesToString(pathBytes.reverse.toArray)
      }

      i += 1
    }

    results.reverse
  }
}
