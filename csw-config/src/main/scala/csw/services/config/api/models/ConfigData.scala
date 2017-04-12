package csw.services.config.api.models

import java.io.{File, InputStream}
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

import akka.stream.scaladsl.{FileIO, Source, StreamConverters}
import akka.stream.{Materializer, javadsl}
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

  /**
    * * Java API
    *
    * Returns a future string by reading the source.
    */
  def toJStringF(implicit mat: Materializer): CompletableFuture[String] = {
    toStringF.toJava.toCompletableFuture
  }
  /**
    * Returns an inputStream which emits the bytes read from source
    */
  def toInputStream(implicit mat: Materializer): InputStream = {
    source.runWith(StreamConverters.asInputStream())
  }

  /**
    * Writes the contents of the source to a provided file and returns it.
    */
  def toFileF(path: Path)(implicit mat: Materializer): Future[File] = {
    import mat.executionContext
    val file = new File(path.toString)
    source
      .runWith(FileIO.toPath(file.toPath))
      .map(_ ⇒ file)
  }

}

/**
 * Provides various alternatives for constructing the data to be stored in the config service.
 */
object ConfigData {
  /**
   * The data is contained in the string
   */
  def fromString(str: String): ConfigData = new ConfigData(Source.single(ByteString(str.getBytes())))

  /**
   * The data source can be any byte string
   */
  def fromSource(source: Source[ByteString, Any]): ConfigData = new ConfigData(source)

  /**
    * Initialize with the contents of the file from given path.
    *
    * @param path      the data source
    * @param chunkSize the block or chunk size to use when streaming the data
    */
  def fromPath(path: Path, chunkSize: Int = 4096): ConfigData = ConfigData.fromSource(FileIO.fromPath(path, chunkSize))

  /**
    * Java API
    *
    * The data source can be any byte string
    */
  def fromJavaSource(source: javadsl.Source[ByteString, Any]): ConfigData = new ConfigData(source.asScala)
}
