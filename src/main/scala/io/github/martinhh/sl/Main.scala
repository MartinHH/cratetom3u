package io.github.martinhh.sl

import org.rogach.scallop.{ScallopConf, ScallopOption}

import scala.util.{Failure, Success, Try}


object Main {

  val ApplicationName = "CrateToM3U"

  /** Command line args parser config. */
  case class Conf(rawArgs: Array[String]) extends ScallopConf(rawArgs.toList) {

    mainOptions = Seq(inputPath, outputPath)

    val inputPath: ScallopOption[String] =
      opt[String](name = "input", short = 'i', descr = "path to input .crate file", required = true)
    val outputPath: ScallopOption[String] =
      opt[String](name = "output", short = 'o', descr = "path to output .m3u file", required = true)

    val remove: ScallopOption[String] =
      opt[String](name = "remove", short = 'r', descr = "audio files path substring to remove (supports regex)")
    val add: ScallopOption[String] =
      opt[String](name = "add", short = 'a', descr = "audio file paths substring to prepend")

    printedName = ApplicationName

    verify()
  }

  case object EmptyAudioFileList extends Throwable

  /** If no audio tracks were found in input, something is probably wrong. */
  private def requireNonEmptyFileSize(audioFilePaths: Traversable[String]): Try[Int] = {
    val size = audioFilePaths.size
    if(size <= 0) Failure(EmptyAudioFileList) else Success(size)
  }

  private def resultString(result: Try[(Int, Boolean)], inPath: String, outPath: String): String = result match {
    case Success((x, false)) => s"Wrote $x-track m3u file to $outPath"
    case Success((x, true)) => s"Error: found $x tracks, but there was an error writing to $outPath"
    case Failure(EmptyAudioFileList) => s"Error: no tracks found. Are you sure $inPath is a valid .crate file?"
    case Failure(e) => s"Error: $e"
  }

  def main(args: Array[String]): Unit = {
    val conf = Conf(args)

    val in = conf.inputPath()
    val out = conf.outputPath()

    val result = for {
      audioFilePaths <- Try(CrateExtractor.audioFilePathsFromCrateFile(in))
      nFiles <- requireNonEmptyFileSize(audioFilePaths)
      hasError <- Try(M3UBuilder.writeToFile(out, audioFilePaths, conf.remove.toOption, conf.add.toOption))
    } yield (nFiles, hasError)

    println(s"[$ApplicationName]: ${resultString(result, in, out)}")

  }
}
