package csw.services.config.api.models

import java.io.InputStream
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Keep, Source, StreamConverters}
import akka.util.ByteString

import scala.compat.java8.FutureConverters._
import scala.concurrent.Future

/**
 * This class represents the contents of the files being managed.
 * It is wraps an Akka streams of ByteString
 */
class ConfigData private (val source: Source[ByteString, Any], val length: Long) {

  /**
   * Returns a future string by reading the source.
   */
  def toStringF(implicit mat: Materializer): Future[String] =
    source.runFold("")((str, bs) ⇒ str + bs.utf8String)

  /**
   * * Java API
   *
   * Returns a future string by reading the source.
   */
  def toJStringF(implicit mat: Materializer): CompletableFuture[String] =
    toStringF.toJava.toCompletableFuture

  /**
   * Returns an inputStream which emits the bytes read from source
   */
  def toInputStream(implicit mat: Materializer): InputStream =
    source.runWith(StreamConverters.asInputStream())

  /**
   * Writes config data to a provided file path and returns future file.
   */
  def toPath(path: Path)(implicit mat: Materializer): Future[Path] = {
    import mat.executionContext
    source
      .toMat(FileIO.toPath(path))(Keep.right)
      .mapMaterializedValue { resultF =>
        resultF.map { ioResult ⇒
          ioResult.status.get
          path
        }
      }
      .run()
  }
}

/**
 * Provides various alternatives for constructing the data to be stored in the config service.
 */
object ConfigData {

  /**
   * The data is contained in the string
   */
  def fromString(str: String): ConfigData = {
    val byteString = ByteString(str.getBytes())
    ConfigData.from(Source.single(byteString), byteString.length)
  }

  /**
   * Initialize with the contents of the file from given path.
   *
   * @param path      the data source
   */
  def fromPath(path: Path): ConfigData = ConfigData.from(FileIO.fromPath(path), path.toFile.length())

  def from(dataBytes: Source[ByteString, Any], length: Long): ConfigData = new ConfigData(dataBytes, length)
}
