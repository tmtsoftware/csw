package csw.location.agent.wiring
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory

private[agent] class Wiring {
  lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "location-agent")
  lazy val actorRuntime                                    = new ActorRuntime(actorSystem)

  import actorRuntime._
  lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient
}
