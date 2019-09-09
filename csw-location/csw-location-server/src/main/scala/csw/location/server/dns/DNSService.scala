package csw.location.server.dns

import akka.actor
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.github.mkroli.dns4s.akka.Dns
import csw.location.api.scaladsl.LocationService

import scala.concurrent.Future

object DNSService {
  def start(port: Int, locationService: LocationService)(
      implicit
      actorSystem: ActorSystem[SpawnProtocol],
      timeout: Timeout
  ): Future[Any] = {
    implicit val untypedSys: actor.ActorSystem = actorSystem.toUntyped
    import actorSystem.executionContext

    for {
      dnsActorRef   <- LocationDnsActor.start(port, locationService)
      proxyActorRef = ProxyActor.start(dnsActorRef)
      bindResult    <- IO(Dns) ? Dns.Bind(proxyActorRef, port)
    } yield bindResult
  }
}
