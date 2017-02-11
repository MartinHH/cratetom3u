package io.github.martinhh.sl

import java.io.UnsupportedEncodingException

import org.rogach.scallop.{ScallopConf, ScallopOption, ValueConverter}

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
    private val _charSet: ScallopOption[String] =
      createOpt[String](name = "charset", short = 'c', descr = s"charset for the output file (default is " +
        s"$DefaultCharset)")

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
      s"Wrote $x tracks to ${conf.outputPath()}"
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
