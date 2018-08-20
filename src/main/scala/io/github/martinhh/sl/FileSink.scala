package io.github.martinhh.sl

import java.nio.charset.Charset
import java.nio.file.Path

import cats.effect.Effect
import fs2.{Chunk, Pipe, Sink, Stream, io}

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

object FileSink {

  private def charSetEncode[F[_]](charsetName: String): Pipe[F, String, Byte] = {
    val charset = Charset.forName(charsetName)
    _.flatMap(s => Stream.chunk(Chunk.bytes(s.getBytes(charset))))
  }

  def async[F[_]](path: Path, charsetName: String)(implicit F: Effect[F], ec: ExecutionContext): Sink[F, String] = {
    _.intersperse("\n")
      .through(charSetEncode[F](charsetName))
      .through(io.file.writeAllAsync[F](path))
  }
}
