package csw.location.server.dns

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorSystem, Props}
import akka.io.IO
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.github.mkroli.dns4s.Message
import com.github.mkroli.dns4s.akka._
import com.github.mkroli.dns4s.dsl._
import csw.location.api.scaladsl.LocationService
import csw.location.models.{AkkaLocation, HttpLocation, TcpLocation}

import scala.async.Async.{async, await}
import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

class LocationDnsActor(locationService: LocationService) extends Actor {
  import context._

  def resolve(host: String): Future[String] = async {

    val locations = await(locationService.list(host))

    val location = locations.find {
      //this order matters because we it gives priority to connections in the same order
      case _: HttpLocation => true
      case _: TcpLocation  => true
      case _: AkkaLocation => true
    }

    location.map(_.uri.getHost).getOrElse(throw new RuntimeException(s"could not resolve $host"))
  }

  val names: Map[String, String] = Map(
    "tmt.com" -> "127.0.0.1"
  )

  val destinationDns = new InetSocketAddress("8.8.8.8", 53)

  def forwardMessage(message: Message): Future[Message] = {
    implicit val timeout: Timeout = Timeout(2 seconds)
    (IO(Dns) ? Dns.DnsPacket(message, destinationDns)).mapTo[Message]
  }

  override def receive: PartialFunction[Any, Unit] = {
    case Query(q) ~ Questions(QName(host) ~ TypeA() :: Nil) if names.contains(host) =>
      sender ! Response(q) ~ Answers(RRName(host) ~ ARecord(names(host)))
    case message: Message =>
      forwardMessage(message).pipeTo(sender)
  }
}

object LocationDnsActor {
  def start(locationService: LocationService)(implicit ec: ExecutionContext): Future[Any] = {
    implicit val system: ActorSystem = ActorSystem("DnsServer")
    implicit val timeout: Timeout    = Timeout(2 seconds)
    val f                            = IO(Dns) ? Dns.Bind(system.actorOf(Props(new LocationDnsActor(locationService))), 7878)
    f.onComplete {
      case Failure(exception) =>
        println("DNS service failed to bind")
        println(exception.getClass.getSimpleName)
        println(exception.getMessage)
        exception.printStackTrace()
      case Success(value) =>
        println(s"DNS service: $value")
    }
    f
  }
}
