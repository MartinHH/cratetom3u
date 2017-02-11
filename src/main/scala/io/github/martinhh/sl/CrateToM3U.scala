package io.github.martinhh.sl

import java.io.UnsupportedEncodingException

import org.rogach.scallop.{ScallopConf, ScallopOption}

import scala.util.{Failure, Success, Try}


object CrateToM3U {

  val ApplicationName = "CrateToM3U"
  val Version = "0.1.0"

  private val DefaultCharset = "UTF-16"

  /** Command line args parser config. */
  case class Conf(rawArgs: Array[String]) extends ScallopConf(rawArgs.toList) {

    version(s"$ApplicationName $Version")

    banner(
      s"""$ApplicationName is a tool to convert Serato .crate files to .m3u playlist files.
        |(Please note that "smart crates" are not supported.)
        |
        |Options:
        |""".stripMargin
    )

    mainOptions = Seq(inputPath, outputPath)

    private def descr(descr: String, isMandatory: Boolean = false): String =
      s"$descr (${if (isMandatory) "required" else "optional"})"

    val inputPath: ScallopOption[String] =
      opt[String](name = "input", short = 'i', descr = descr("path to input .crate file", isMandatory = true),
        required = true)
    val outputPath: ScallopOption[String] =
      opt[String](name = "output", short = 'o', descr = descr("path to output .m3u file", isMandatory = true),
        required = true)

    val remove: ScallopOption[String] =
      opt[String](name = "remove", short = 'r', descr = descr("audio file paths substring to remove (supports regex)"))
    val add: ScallopOption[String] =
      opt[String](name = "add", short = 'a', descr = descr("audio file paths substring to prepend"))
    private val _charSet: ScallopOption[String] =
      opt[String](name = "charset", short = 'c', descr = descr(s"charset for the output file (default is " +
        s"$DefaultCharset)"))

    def charset: String = _charSet.toOption.getOrElse(DefaultCharset)

    printedName = ApplicationName

    verify()
  }

  case object EmptyAudioFileList extends Throwable

  /** If no audio tracks were found in input, something is probably wrong. */
  private def requireNonEmptyFileSize(audioFilePaths: Traversable[String]): Try[Int] = {
    val size = audioFilePaths.size
    if (size <= 0) Failure(EmptyAudioFileList) else Success(size)
  }

  private def resultString(result: Try[(Int, Boolean)], conf: Conf): String = result match {
    case Success((x, false)) =>
      s"Wrote $x-track m3u file to ${conf.outputPath()}"
    case Success((x, true)) =>
      s"Error: found $x tracks, but there was an error writing to ${conf.outputPath()}"
    case Failure(EmptyAudioFileList) =>
      s"Error: no tracks found. Are you sure ${conf.inputPath()} is a valid .crate file?"
    case Failure(e: UnsupportedEncodingException) =>
      s"Error: unsupported charset: ${conf.charset}"
    case Failure(e) => s"Error: $e"
  }

  def main(args: Array[String]): Unit = {
    import CrateExtractor.audioFilePathsFromCrateFile
    import M3UBuilder.writeToFile

    val conf = Conf(args)

    val result = for {
      audioPaths <- Try(audioFilePathsFromCrateFile(conf.inputPath()))
      nFiles <- requireNonEmptyFileSize(audioPaths)
      hasError <- Try(writeToFile(conf.outputPath(), audioPaths, conf.remove.toOption, conf.add.toOption, conf.charset))
    } yield (nFiles, hasError)

    println(s"[$ApplicationName]: ${resultString(result, conf)}")
  }
}
