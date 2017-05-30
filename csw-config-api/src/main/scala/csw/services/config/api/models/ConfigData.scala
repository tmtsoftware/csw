package csw.services.config.api.models

import java.io.InputStream
import java.nio.file.{Files, Path}
import java.util.concurrent.CompletableFuture

import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Keep, Source, StreamConverters}
import akka.util.ByteString
import com.typesafe.config.{Config, ConfigFactory}

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
   * Returns a future of Config object if the data is in valid parseable HOCON format. Else, throws ConfigException.
   */
  def toConfigObject(implicit mat: Materializer): Future[Config] = {
    import mat.executionContext
    toStringF.map { s ⇒
      ConfigFactory.parseString(s)
    }
  }

  /**
   * * Java API
   *
   * Returns a future string by reading the source.
   */
  private[config] def toJStringF(implicit mat: Materializer): CompletableFuture[String] =
    toStringF.toJava.toCompletableFuture

  /**
   * Returns a future of Config object if the data is in valid parseable HOCON format. Else, throws ConfigException.
   */
  private[config] def toJConfigObject(implicit mat: Materializer): CompletableFuture[Config] =
    toConfigObject.toJava.toCompletableFuture

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

  /**
   * Returns an inputStream which emits the bytes read from source
   */
  private[config] def toInputStream(implicit mat: Materializer): InputStream =
    source.runWith(StreamConverters.asInputStream())
}

/**
 * Provides various alternatives for constructing the data to be stored in the config service.
 */
object ConfigData {

  /**
   * Create from string
   */
  def fromString(str: String): ConfigData = ConfigData.fromBytes(str.getBytes())

  /**
   * Create from file path
   *
   */
  def fromPath(path: Path): Option[ConfigData] = {
    if (Files.exists(path)) Some(ConfigData.from(FileIO.fromPath(path), path.toFile.length()))
    else None
  }

  /**
   * Create from byte array
   *
   */
  def fromBytes(bytes: Array[Byte]): ConfigData = ConfigData.from(Source.single(ByteString(bytes)), bytes.length)

  private[config] def from(dataBytes: Source[ByteString, Any], length: Long): ConfigData =
    new ConfigData(dataBytes, length)
}
