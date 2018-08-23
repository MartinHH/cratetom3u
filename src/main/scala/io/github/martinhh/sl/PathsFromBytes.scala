package io.github.martinhh.sl

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import fs2.{Chunk, Pipe, Pull, Stream}

import scala.language.higherKinds

/**
 * Converts the [[Byte]]s from the raw (.crate-)input-files to (track-)file-path-[[String]]s.
 */
object PathsFromBytes {


  def pipe[F[_]]: Pipe[F, Byte, String] = in =>
    in.scan(ScanState.Initial)(_.apply(_)).collect { case ScanState.Success(string) => string }


  /**
   * States to model the conversion of a file to audio-file-paths
   * as a state machine.
   */
  private sealed trait ScanState {
    def apply(byte: Byte): ScanState
  }

  private object ScanState {
    val Initial: ScanState = CheckingStartMarker.P

    object Success {
      def unapply(scanState: ScanState): Option[String] = scanState match {
        case Done(string) => Some(string)
        case _ => None
      }
    }

    private sealed trait CheckingStartMarker extends ScanState

    /** Start of a playlist-entry is indicated by the four bytes "ptrk". */
    private object CheckingStartMarker {

      final case object P extends CheckingStartMarker {
        override def apply(byte: Byte): ScanState = if (byte == 'p'.toByte) T else this
      }

      final case object T extends CheckingStartMarker {
        override def apply(byte: Byte): ScanState = if (byte == 't'.toByte) R else P
      }

      final case object R extends CheckingStartMarker {
        override def apply(byte: Byte): ScanState = if (byte == 'r'.toByte) K else P
      }

      final case object K extends CheckingStartMarker {
        // after the four start-marker-bytes, we expect four bytes indicating the size of the path-string:
        override def apply(byte: Byte): ScanState = if (byte == 'k'.toByte) CountingBytes.PathSize else P
      }

    }

    private final case class CountingBytes(bytesSoFar: List[Byte] = List.empty, leftToScan: Int,
                                           factory: CountingBytes.NextStateFactory) extends ScanState {
      override def apply(byte: Byte): ScanState = {
        val newBytes = byte :: bytesSoFar
        if (leftToScan > 1)
          CountingBytes(newBytes, leftToScan - 1, factory)
        else
          factory(newBytes)
      }

    }

    private object CountingBytes {
      type NextStateFactory = List[Byte] => ScanState

      /** Reads four bytes as size-parameters for the following path-string. */
      val PathSize = CountingBytes(leftToScan = 4, factory = CountingBytes.NextStateFactory.PathSize)

      object NextStateFactory {
        /** once the path-size is read, this will lead to reading of the file-path-string. */
        case object PathSize extends NextStateFactory {
          override def apply(completeBytes: List[Byte]): ScanState = {
            val pathSize = ByteBuffer.wrap(completeBytes.reverse.toArray).getInt
            CountingBytes(leftToScan = pathSize, factory = FilePath)
          }
        }

        /** once the file-path is read, this will lead to the final [[Done]] (which will behave like [[Initial]]). */
        case object FilePath extends NextStateFactory {
          override def apply(completeBytes: List[Byte]): ScanState = {
            val string = new java.lang.String(completeBytes.reverse.toArray, StandardCharsets.UTF_16)
            Done(string)
          }
        }
      }
    }


    private final case class Done(value: String) extends ScanState {
      override def apply(byte: Byte) = Initial.apply(byte)
    }
  }

  // The start of an audio file path is marked by these 4 bytes + 4 subsequent bytes
  // that contain the length of the audio file path.
  private val StartMarker: Vector[Byte] = "ptrk".map(_.toByte).toVector
  private val PathLengthOffset = 4
  private val StartMarkerFullLength = StartMarker.length + PathLengthOffset

  def pipe2[F[_]]: Pipe[F, Byte, String] = {

    /** Check that bytes contains subSet at idx. */
    def hasEqualBytesAt(idx: Int, bytes: Vector[Byte], subSet: Vector[Byte]): Boolean = {
      idx < bytes.length - subSet.length && subSet.indices.forall(i => bytes(idx + i) == subSet(i))
    }

    def audioFilePathsFromCrateFile(bytes: Vector[Byte]): (Vector[String], Vector[Byte]) = {
      val bytesLength = bytes.length
      var i = 0
      var indexOfRemainder = 0
      var results = Vector.empty[String]
      var done = false

      /** Create String from bytesOfFile starting at i. */
      def bytesToString(size: Int): String = {
        new java.lang.String(bytes.toArray, i, size, StandardCharsets.UTF_16)
      }

      while (!done && i < bytesLength - StartMarkerFullLength) {

        // search for a startMarker:
        if (hasEqualBytesAt(i, bytes, StartMarker)) {
          // skip marker
          i += StartMarker.length

          // the next 4 bytes indicate the length of the audio file path
          val pathSize = ByteBuffer.wrap(bytes.toArray, i, 4).getInt

          i += PathLengthOffset

          if (i + pathSize <= bytesLength) {
            // add audio file path to results:
            results :+= bytesToString(pathSize)

            i += pathSize
            indexOfRemainder = i
          } else {
            done = true
          }


        }

        i += 1
      }

      results -> bytes.drop(i)
    }

    def go(buffer: Vector[Byte],
           stream: Stream[F, Byte]): Pull[F, String, Option[Unit]] =
      stream.pull.unconsChunk.flatMap[F, String, Option[Unit]] {
        case Some((chunk, remainingStream)) =>
          val (toOutput, remainder) = audioFilePathsFromCrateFile(buffer ++ chunk.toVector)
          Pull.outputChunk(Chunk.vector(toOutput)) >> go(remainder, remainingStream)
        case None =>
          Pull.pure(None)
      }

    in => go(Vector.empty, in).stream
  }

}
