package csw.services.config.api.models

import java.io.{File, InputStream}
import java.nio.file.Files
import java.util.concurrent.CompletableFuture

import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Source, StreamConverters}
import akka.util.ByteString

import scala.compat.java8.FutureConverters._
import scala.concurrent.Future

/**
 * This class represents the contents of the files being managed.
 * It is wraps an Akka streams of ByteString
 */
class ConfigData(val source: Source[ByteString, Any]) {
  /**
   * Returns a future string by reading the source.
   */
  def toStringF(implicit mat: Materializer): Future[String] = {
    source.runFold("")((str, bs) ⇒ str + bs.utf8String)
  }

  def toJStringF(implicit mat: Materializer): CompletableFuture[String] = {
    toStringF.toJava.toCompletableFuture
  }
  /**
    * Returns an inputStream which emits the bytes read from source
    */
  def toInputStream(implicit mat: Materializer): InputStream = {
    source.runWith(StreamConverters.asInputStream())
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
