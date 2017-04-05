package csw.services.cs.models

import java.io.{ByteArrayOutputStream, File, FileOutputStream, OutputStream}
import java.nio.file.{Files, Paths, StandardCopyOption}

import akka.NotUsed
import akka.actor.ActorRefFactory
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString

import scala.concurrent.Future
import scala.util.Try

/**
 * This trait represents the contents of the files being managed.
 * It is based on Akka streams.
 */
trait ConfigData {
  /**
   * Returns a stream which can be used to read the data
   */
  def source: Source[ByteString, Any]

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
  def toFutureString(implicit context: ActorRefFactory): Future[String] = {
    implicit val materializer = ActorMaterializer()
    import context.dispatcher
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
  def apply(str: String): ConfigData = ConfigString(str)

  /**
   * Takes the data from the byte array
   */
  def apply(bytes: Array[Byte]): ConfigData = ConfigBytes(bytes)

  /**
   * Initialize with the contents of the given file.
   *
   * @param file      the data source
   * @param chunkSize the block or chunk size to use when streaming the data
   */
  def apply(file: File, chunkSize: Int = 4096): ConfigData = ConfigFile(file, chunkSize)

  /**
   * The data source can be any byte string
   */
  def apply(source: Source[ByteString, Any]): ConfigData = ConfigSource(source)
}

case class ConfigString(str: String) extends ConfigData {
  override def source: Source[ByteString, NotUsed] = Source(List(ByteString(str.getBytes)))

  override def toString: String = str
}

case class ConfigBytes(bytes: Array[Byte]) extends ConfigData {
  override def source: Source[ByteString, NotUsed] = Source(List(ByteString(bytes)))

  override def toString: String = new String(bytes)
}

case class ConfigFile(file: File, chunkSize: Int = 4096) extends ConfigData {

  // XXX Seems that Source is not serializable...
  //  override def source: Source[ByteString, NotUsed] = {
  //    val mappedByteBuffer = FileUtils.mmap(file.toPath)
  //    val iterator = new FileUtils.ByteBufferIterator(mappedByteBuffer, chunkSize)
  //    Source.fromIterator(() => iterator)
  //  }

  private val bytes = Files.readAllBytes(Paths.get(file.getPath))

  override def source: Source[ByteString, NotUsed] = Source(List(ByteString(bytes)))

  override def toString: String = new String(bytes)
}

case class ConfigSource(override val source: Source[ByteString, Any]) extends ConfigData