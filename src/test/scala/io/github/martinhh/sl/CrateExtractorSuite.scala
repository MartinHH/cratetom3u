package io.github.martinhh.sl

import org.scalatest.FunSuite


class CrateExtractorSuite extends FunSuite {

  test("extracting 2SongTestCrate.crate should return the two contained paths") {
    val filePath = getClass.getResource("/testcrates/2SongTestCrate.crate").getFile
    val extractedAudioPaths = CrateExtractor.audioFilePathsFromCrateFile(filePath)
    assertResult(2)(extractedAudioPaths.size)
    assertResult("my music/genre a/Artist X/03 - Some Song.mp3")(extractedAudioPaths.head)
    assertResult("my music/genre b/artist y/Thät Söng With Thöse Germän Letters.wav")(extractedAudioPaths(1))
  }
}
