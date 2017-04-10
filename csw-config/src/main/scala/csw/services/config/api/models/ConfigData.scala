package csw.services.config.api.models

import java.io.{ByteArrayOutputStream, File, FileOutputStream, OutputStream}
import java.nio.file.{Files, Paths, StandardCopyOption}
import java.util.concurrent.CompletionStage

import akka.NotUsed
import akka.actor.ActorRefFactory
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{FileIO, Sink, Source, StreamConverters}
import akka.util.ByteString

import scala.concurrent.Future
import scala.util.Try

import scala.compat.java8.FutureConverters._

/**
 * This class represents the contents of the files being managed.
 * It is wraps an Akka streams of ByteString
 */
class ConfigData(val source: Source[ByteString, Any]) {
  /**
   * Writes the contents of the source to the given output stream.
   */
  def writeToOutputStream(out: OutputStream)(implicit mat: Materializer): Future[Unit] = {
    import mat.executionContext
    source
      .runWith(StreamConverters.fromOutputStream(() ⇒ out))
      .map(_ ⇒ Try(out.close()))
  }

  /**
   * Writes the contents of the source to a temp file and returns it.
   */
  def toFileF(implicit mat: Materializer): Future[File] = {
    import mat.executionContext
    val path = Files.createTempFile("config-service-", null)
    source
      .runWith(FileIO.toPath(path))
      .map(_ ⇒ path.toFile)
  }

  /**
   * Returns a future string by reading the source.
   */
  def toStringF(implicit mat: Materializer): Future[String] = {
    source.runFold("")((str, bs) ⇒ str + bs.utf8String)
  }

  def jStringF(implicit mat: Materializer): CompletionStage[String] = {
    stringF.toJava
  }
}

/**
 * Provides various alternatives for constructing the data to be stored in the config service.
 */
object ConfigData {
  /**
   * The data is contained in the string
   */
  def apply(str: String): ConfigData = ConfigData(str.getBytes)

  /**
   * Takes the data from the byte array
   */
  def apply(bytes: Array[Byte]): ConfigData = ConfigData(Source.single(ByteString(bytes)))

  /**
   * Initialize with the contents of the given file.
   *
   * @param file      the data source
   * @param chunkSize the block or chunk size to use when streaming the data
   */
  def apply(file: File, chunkSize: Int = 4096): ConfigData = ConfigData(FileIO.fromPath(file.toPath, chunkSize))

  /**
   * The data source can be any byte string
   */
  def apply(source: Source[ByteString, Any]): ConfigData = new ConfigData(source)
}
