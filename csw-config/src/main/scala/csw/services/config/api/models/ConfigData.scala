package csw.services.config.api.models

import java.io.{ByteArrayOutputStream, File, FileOutputStream, OutputStream}
import java.nio.file.{Files, Paths, StandardCopyOption}

import akka.NotUsed
import akka.actor.ActorRefFactory
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.util.ByteString

import scala.concurrent.Future
import scala.util.Try

/**
 * This class represents the contents of the files being managed.
 * It is wraps an Akka streams of ByteString
 */
class ConfigData(val source: Source[ByteString, Any]) {
  /**
   * Writes the contents of the source to the given output stream.
   */
  def writeToOutputStream(out: OutputStream)(implicit context: ActorRefFactory): Future[Unit] = {
    import context.dispatcher
    implicit val materializer = ActorMaterializer()
    val sink = Sink.foreach[ByteString] { bytes =>
      out.write(bytes.toArray)
    }
    val materialized = source.runWith(sink)
    // ensure the output file is closed when done
    for {
      _ <- materialized
    } yield {
      Try(out.close())
    }
  }

  /**
   * Writes the contents of the source to the given file.
   */
  def writeToFile(file: File)(implicit context: ActorRefFactory): Future[Unit] = {
    import context.dispatcher
    val path = file.toPath
    val dir = Option(path.getParent).getOrElse(new File(".").toPath)
    if (!Files.isDirectory(dir))
      Files.createDirectories(dir)

    // Write to a tmp file and then rename
    val tmpFile = File.createTempFile(file.getName, null, dir.toFile)
    val out = new FileOutputStream(tmpFile)
    for {
      _ <- writeToOutputStream(out)
    } yield {
      Files.move(tmpFile.toPath, path, StandardCopyOption.ATOMIC_MOVE)
    }
  }

  /**
   * Returns a future string by reading the source.
   */
  def toFutureString(implicit mat: Materializer): Future[String] = {
    import mat.executionContext
    val out = new ByteArrayOutputStream
    val sink = Sink.foreach[ByteString] { bytes =>
      out.write(bytes.toArray)
    }
    val materialized = source.runWith(sink)
    for (_ <- materialized) yield out.toString
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
