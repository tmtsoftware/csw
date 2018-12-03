package csw.config.server.files

import java.nio.file.Path
import java.security.MessageDigest

import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source}
import akka.util.ByteString
import csw.config.api.models.ConfigData

import scala.concurrent.Future

/**
 * Generates SHA1 for file represented as stream
 */
object Sha1 {

  private def fromSource(source: Source[ByteString, Any])(implicit mat: Materializer): Future[String] =
    source.runWith(sink)

  /**
   * Create the sha digest of the data from the given configData model.
   *
   * @param configData the configData model to get the data from
   * @param mat an akka materializer required to start the stream of data that will incrementally calculate the sha digest
   *            of it
   * @return a future that completes with calculated sha value of the data
   */
  def fromConfigData(configData: ConfigData)(implicit mat: Materializer): Future[String] =
    fromSource(configData.source)

  /**
   * Create the sha digest of the data from the given path. It is used while creating a file in annex repository, as the
   * data is first stored at temporary location and then at final location. This method helps to validate the sha of data
   * stored both places and find out discrepancies if any.
   *
   * @param path the path to get the data from
   * @param mat an akka materializer required to start the stream of data that will incrementally calculate the sha digest
   *            of it
   * @return a future that completes with calculated sha value of the data
   */
  def fromPath(path: Path)(implicit mat: Materializer): Future[String] =
    fromSource(FileIO.fromPath(path))

  /**
   * Create a sink that incrementally calculates the sha digest for the data
   *
   * @note keep this a def so that the digester is created anew each time
   * @return a Sink which accepts byte data and gives a future of calculated sha
   */
  def sink: Sink[ByteString, Future[String]] = {
    val sha1Digester = MessageDigest.getInstance("SHA-1")
    Flow[ByteString]
      .fold(sha1Digester) { (digester, bs) =>
        digester.update(bs.toArray)
        digester
      }
      .mapConcat(_.digest().toList)
      .map(_ & 0xFF)
      .map("%02x" format _)
      .toMat(Sink.fold("")(_ + _))(Keep.right)
  }

}
