package io.github.martinhh.sl

import java.nio.file.Paths

import cats.effect.IO
import org.scalatest.FunSuite

object CrateExtractorSuite {
  val NumberOfCrateFilesInTestCratesDir = 1
}

class CrateExtractorSuite extends FunSuite {

  import CrateExtractorSuite._

  test("extracting 2SongTestCrate.crate via stream should return the two contained paths") {
    import scala.concurrent.ExecutionContext.Implicits.global
    val filePath = getClass.getResource("/testcrates/2SongTestCrate.crate").toURI
    val path = Paths.get(filePath)
    val extractedAudioPaths = CrateExtractor.filePathsStream[IO](path).compile.toList.unsafeRunSync()
    assertResult(2)(extractedAudioPaths.size)
    assertResult("my music/genre a/Artist X/03 - Some Song.mp3")(extractedAudioPaths.head)
    assertResult("my music/genre b/artist y/Thät Söng With Thöse Germän Letters.wav")(extractedAudioPaths(1))
  }

  test("getCrateFiles should not return non-\".crate\"-files") {
    val dirPath = getClass.getResource("/testcrates").getFile
    val extractedCratePaths = CrateExtractor.getCrateFiles(dirPath, matchRegex = None)
    assertResult(NumberOfCrateFilesInTestCratesDir)(extractedCratePaths.length)
  }

  test("getCrateFiles with regex should not return non-matching \".crate\"-files") {
    val dirPath = getClass.getResource("/testcrates").getFile
    val extractedCratePaths = CrateExtractor.getCrateFiles(dirPath, matchRegex = Some(""".*foo.*"""))
    assertResult(0)(extractedCratePaths.length)
  }

  test("getCrateFiles with regex should return matching \".crate\"-files") {
    val dirPath = getClass.getResource("/testcrates").getFile
    val extractedCratePaths = CrateExtractor.getCrateFiles(dirPath, matchRegex = Some(""".*2So.*Test.*"""))
    assertResult(1)(extractedCratePaths.length)
  }

  test("CrateSuffixRegex should not replace \".crate\" in the middle of the path") {
    val input = "/someDir/.crateDir/MyCrate.crate"
    val replaced = input.replaceAll(CrateExtractor.CrateSuffixRegex, "")
    assertResult("/someDir/.crateDir/MyCrate")(replaced)
  }

  test("CrateFileRegex should be matched by \".crate\" at the end of the path") {
    val input = "/someDir/subDir/MyCrate.crate"
    assert(input.matches(CrateExtractor.CrateFileRegex))
  }

  test("CrateFileRegex should not be matched by \".crate\" in the middle of the path") {
    val input = "/someDir/.crateDir/MyCrate.foo"
    assert(!input.matches(CrateExtractor.CrateFileRegex))
  }
}
