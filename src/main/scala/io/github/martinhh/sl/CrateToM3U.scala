package io.github.martinhh.sl

import java.io.UnsupportedEncodingException
import java.nio.file.{Path, Paths}

import cats.effect.{Effect, IO}
import fs2.{io => _, _}
import io.github.martinhh.sl.M3UBuilder.M3UConfig
import org.rogach.scallop.{ScallopConf, ScallopOption}

import scala.concurrent.ExecutionContext
import scala.language.higherKinds
import scala.util.{Failure, Success, Try}


object CrateToM3U {

  private val ApplicationName = io.github.martinhh.sl.BuildInfo.name.toLowerCase
  private val Version = io.github.martinhh.sl.BuildInfo.version

  private val DefaultCharSet = "UTF-16"

  /** Command line args parser config. */
  case class Conf(rawArgs: Array[String]) extends ScallopConf(rawArgs.toList) {

    version(s"$ApplicationName $Version")

    banner(
      s"""$ApplicationName is a tool to convert Serato .crate files to .m3u playlist files.
        |(Please note that "smart crates" are not supported.)
        |
        |Usage:
        |cratetom3u [options] inputpath outputpath
        |
        |Options:
        |""".stripMargin
    )

    private val irrelevantForFile = " - irrelevant in single file mode"

    val inputPath: ScallopOption[String] = trailArg[String](name = "inputPath",
        descr = "Path to input crates directory (or .crate file in single file mode)", required = true)
    val outputPath: ScallopOption[String] = trailArg[String](name = "outputPath",
        descr = "Path to output directory (or .m3u file in single file mode)", required = true)

    val remove: ScallopOption[String] = opt[String](name = "remove", short = 'r',
      descr = "Audio file path substring to remove (supports regex)", argName = "expression")
    val add: ScallopOption[String] = opt[String](name = "add", short = 'a',
      descr = "Audio file path prefix to prepend", argName = "prefix")
    val charSet: ScallopOption[String] = opt[String](name = "charset", short = 'c',
        descr = s"Charset for the output files (default is $DefaultCharSet)", argName = "charset")
    val matches: ScallopOption[String] = opt[String](name = "matches", short = 'm',
      descr = s"String that extracted .crate files must match (supports regex)$irrelevantForFile",
      argName = "expression")
    val backslash: ScallopOption[Boolean] = opt[Boolean](name = "backslash", short = 'b',
      descr = s"Replace all '/'s in audio file paths by '\\'s")
    private val _suffix: ScallopOption[String] = opt[String](name = "suffix", short = 's',
      descr = "The suffix for the output files in directory mode (including the leading '.' - default is \".m3u\")" +
        irrelevantForFile, argName = "expression")
    private val _fileMode: ScallopOption[Boolean] = opt[Boolean](name = "filemode", short = 'f',
      descr = "Enable single file mode")

    printedName = ApplicationName

    verify()

    val fileMode: Boolean = _fileMode.toOption.getOrElse(false)

    val suffix: String = _suffix.toOption.getOrElse(".m3u")

    val m3uConfig: M3UConfig =
      M3UConfig(remove.toOption, add.toOption, charSet.toOption, backslash.toOption.getOrElse(false))
  }

  case object EmptyAudioFileList extends Throwable

  // TODO (via-fs2): reactivate something like this
  private def resultString(result: Try[(Int, Boolean)], in: String, out: String,
                           charSet: Option[String]): String = result match {
    case Success((x, false)) =>
      s"Wrote $x tracks to $out"
    case Success((x, true)) =>
      s"Error: found $x tracks, but there was an error writing to $out"
    case Failure(EmptyAudioFileList) =>
      s"No tracks found in $in, so no output file was created"
    case Failure(_: UnsupportedEncodingException) if charSet.isDefined =>
      s"Error: unsupported charset: ${charSet.get}"
    case Failure(e) => s"Error: $e"
  }


  /** Combines crate-extraction and m3u-writing wrapped in [[Try]]s and prints a result line. */
  private def convertFile[F[_]](in: Path, out: Path, config: M3UConfig)(implicit F: Effect[F], ec: ExecutionContext): fs2.Stream[F, Unit] = {
    CrateExtractor.filePathsStream[F](in)
      .through(M3UBuilder.filePathsToM3U[F](config))
      .through(FileSink.async[F](out, config.charSetName.getOrElse(DefaultCharSet)))
  }

  def main(args: Array[String]): Unit = {
    import ExecutionContext.Implicits.global

    val conf = Conf(args)

    def failWithMessage(string: String): Stream[IO, Nothing] =
      Stream.eval_(IO {
        println(string)
      })

    val stream: fs2.Stream[IO, Unit] =
      if(conf.fileMode) {
        convertFile[IO](Paths.get(conf.inputPath()), Paths.get(conf.outputPath()), conf.m3uConfig)
    } else {
      Try(CrateExtractor.getCrateFiles(conf.inputPath(), conf.matches.toOption)) match {
        case Success(files) if files.isEmpty =>
          failWithMessage(s"[$ApplicationName]: no .crate files found in ${conf.inputPath()}")
        case Failure(e) =>
          failWithMessage(s"[$ApplicationName]: Error: $e")
        case Success(files) =>
          Stream(files:_*).flatMap { pathString =>
            convertFile[IO](Paths.get(conf.inputPath()), Paths.get(conf.outputPath()), conf.m3uConfig)
          }

      }
    }

    stream.compile.drain.unsafeRunSync()

  }
}
