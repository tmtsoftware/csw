package csw.location.server.dns

import java.net.InetSocketAddress

import akka.actor.Actor
import akka.io.IO
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.github.mkroli.dns4s.Message
import com.github.mkroli.dns4s.akka._
import com.github.mkroli.dns4s.dsl._
import csw.location.api.models.{AkkaLocation, HttpLocation, TcpLocation}
import csw.location.server.internal.LocationServiceImpl

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.language.postfixOps

class DnsActor(locationServiceImpl: LocationServiceImpl) extends Actor {
  import context._

  def resolve(host: String): Future[String] = async {

    val locations = await(locationServiceImpl.list(host))

    val location = locations.find {
      //this order matters because we it gives priority to connections in the same order
      case _: HttpLocation => true
      case _: TcpLocation  => true
      case _: AkkaLocation => true
    }

    location.map(_.uri.getHost).getOrElse(throw new RuntimeException(s"could not resolve $host"))
  }

  implicit val timeout: Timeout = Timeout(5 seconds)

  val names: Map[String, String] = Map(
    "tmt.com" -> "127.0.0.1"
  )

  val destinationDns = new InetSocketAddress("8.8.8.8", 53)

  def forwardMessage(message: Message): Future[Message] = {
    (IO(Dns) ? Dns.DnsPacket(message, destinationDns)).mapTo[Message]
  }

  override def receive: PartialFunction[Any, Unit] = {
    case Query(q) ~ Questions(QName(host) ~ TypeA() :: Nil) if names.contains(host) =>
      sender ! Response(q) ~ Answers(RRName(host) ~ ARecord(names(host)))
    case message: Message =>
      forwardMessage(message).pipeTo(sender)
  }
}
