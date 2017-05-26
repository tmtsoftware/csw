package csw.services.csclient.cli

import akka.actor.{ActorSystem, CoordinatedShutdown}
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.client.internal.ActorRuntime
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.location.commons.{ClusterSettings, CswCluster}
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}

class ClientCliWiring(actorSystem: ActorSystem) {
  lazy val actorRuntime                     = new ActorRuntime(actorSystem)
  lazy val locationService: LocationService = LocationServiceFactory.withCluster(CswCluster.withSystem(actorSystem))
  lazy val configService: ConfigService     = ConfigClientFactory.adminApi(actorRuntime.actorSystem, locationService)
  lazy val commandLineRunner                = new CommandLineRunner(configService, actorRuntime)

  actorRuntime.coordinatedShutdown.addTask(
    CoordinatedShutdown.PhaseBeforeClusterShutdown,
    "location-service-shutdown"
  )(() ⇒ locationService.shutdown())
}
