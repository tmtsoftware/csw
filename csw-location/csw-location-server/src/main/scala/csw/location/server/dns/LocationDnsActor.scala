package csw.location.server.dns

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.github.mkroli.dns4s.Message
import com.github.mkroli.dns4s.dsl.{Response, _}
import csw.location.api.scaladsl.LocationService
import csw.location.models.Connection.HttpConnection
import csw.location.models.{ComponentId, ComponentType}

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

class LocationDnsActor(locationService: LocationService) extends Actor {

  import context.dispatcher

  override def receive: Receive = {

    case Query(q) ~ Questions(QName(host) ~ TypeA() :: Nil) =>
      val senderCached = sender()
      resolve(host).onComplete {
        case Success(ip) =>
          senderCached ! Response(q) ~ Answers(RRName(host) ~ ARecord(ip)) ~ AuthoritativeAnswer
        case Failure(_) =>
          senderCached ! Response(q) ~ Refused
      }

    case message: Message =>
      sender ! message ~ Refused
  }

  private def resolve(host: String)(implicit executionContext: ExecutionContext): Future[String] = async {
    //todo: check component-id
    await(locationService.find(HttpConnection(ComponentId(s"$host-http", ComponentType.Service))))
      .map(_.uri.getHost)
      .getOrElse(throw new RuntimeException(s"could not resolve $host"))
  }
}

object LocationDnsActor {
  def start(port: Int, locationService: LocationService)(
      implicit actorSystem: ActorSystem
  ): ActorRef = {
    actorSystem.actorOf(Props(new LocationDnsActor(locationService)))
  }
}
