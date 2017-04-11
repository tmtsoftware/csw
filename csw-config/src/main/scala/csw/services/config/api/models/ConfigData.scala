package csw.services.config.api.models

import java.io.InputStream
import java.util.concurrent.CompletableFuture

import akka.stream.scaladsl.{Source, StreamConverters}
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
    * Java API
    *
    * The data source can be any byte string
    */
  def fromJavaSource(source: javadsl.Source[ByteString, Any]): ConfigData = new ConfigData(source.asScala)
}
