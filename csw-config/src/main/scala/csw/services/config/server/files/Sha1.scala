package csw.services.config.server.files

import java.nio.file.Path
import java.security.MessageDigest

import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source}
import akka.util.ByteString
import csw.services.config.api.models.ConfigData

import scala.concurrent.Future

object Sha1 {

  private def fromSource(source: Source[ByteString, Any])(implicit mat: Materializer): Future[String] = {
    source.runWith(sink)
  }

  def fromConfigData(configData: ConfigData)(implicit mat: Materializer): Future[String] = {
    fromSource(configData.source)
  }

  def fromPath(path: Path)(implicit mat: Materializer): Future[String] = {
    fromSource(FileIO.fromPath(path))
  }

  //Keep this a def so that the digester is created anew each time.
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
