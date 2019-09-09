package csw.location.server.dns

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.github.mkroli.dns4s.akka.Dns
import csw.location.api.scaladsl.LocationService

import scala.concurrent.Future

object DNSService {
  def start(port: Int, locationService: LocationService)(
      implicit
      actorSystem: ActorSystem,
      timeout: Timeout
  ): Future[Any] = {
    val dnsActorRef = LocationDnsActor.start(port, locationService)
    IO(Dns) ? Dns.Bind(dnsActorRef, port)
  }
}
