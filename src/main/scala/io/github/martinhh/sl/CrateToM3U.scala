package io.github.martinhh.sl

import java.io.UnsupportedEncodingException

import io.github.martinhh.sl.CrateExtractor.CrateExtractionError
import org.rogach.scallop.{ScallopConf, ScallopOption, ValueConverter}

import scala.util.{Failure, Success, Try}


object CrateToM3U {

  private val ApplicationName = io.github.martinhh.sl.BuildInfo.name.toLowerCase
  private val Version = io.github.martinhh.sl.BuildInfo.version

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

    /** Create [[ScallopOption]] with standard description. */
    private def createOpt[T: ValueConverter](name: String, short: Char, descr: String,
                                             required: Boolean = false): ScallopOption[T] = {
      val fullDescr = if(required) s"$descr (required)" else descr
      opt[T](name = name, short = short, descr = fullDescr, required = required)
    }

    val inputPath: ScallopOption[String] =
      createOpt[String](name = "input", short = 'i', descr = "path to input .crate file", required = true)
    val outputPath: ScallopOption[String] =
      createOpt[String](name = "output", short = 'o', descr = "path to output .m3u file", required = true)

    val remove: ScallopOption[String] =
      createOpt[String](name = "remove", short = 'r', descr = "audio file paths substring to remove (supports regex)")
    val add: ScallopOption[String] =
      createOpt[String](name = "add", short = 'a', descr = "audio file paths substring to prepend")
    val charSet: ScallopOption[String] =
      createOpt[String](name = "charset", short = 'c', descr = s"charset for the output file (default is your system's" +
        s" default)")

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
      s"Wrote $x tracks to ${conf.outputPath()}"
    case Success((x, true)) =>
      s"Error: found $x tracks, but there was an error writing to ${conf.outputPath()}"
    case Failure(EmptyAudioFileList) =>
      s"Error: no tracks found. Are you sure ${conf.inputPath()} is a valid .crate file?"
    case Failure(e: UnsupportedEncodingException) if conf.charSet.isDefined =>
      s"Error: unsupported charset: ${conf.charSet()}"
    case Failure(CrateExtractionError(msg)) =>
      s"Error: unexpected crate file format - $msg"
    case Failure(e) => s"Error: $e"
  }

  def main(args: Array[String]): Unit = {

    val conf = Conf(args)

    def writeM3U(audioPaths: Traversable[String]): Boolean =
      M3UBuilder.writeToFile(conf.outputPath(), audioPaths, conf.remove.toOption, conf.add.toOption,
        conf.charSet.toOption)

    val result = for {
      audioPaths <- Try(CrateExtractor.audioFilePathsFromCrateFile(conf.inputPath()))
      nFiles <- requireNonEmptyFileSize(audioPaths)
      hasError <- Try(writeM3U(audioPaths))
    } yield (nFiles, hasError)

    println(s"[$ApplicationName]: ${resultString(result, conf)}")
  }
}
