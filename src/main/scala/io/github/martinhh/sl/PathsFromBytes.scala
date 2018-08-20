package io.github.martinhh.sl

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import fs2.Pipe

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


}
