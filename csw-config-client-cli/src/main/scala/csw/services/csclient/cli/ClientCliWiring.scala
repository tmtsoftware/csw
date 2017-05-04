package csw.services.csclient.cli

import akka.actor.CoordinatedShutdown
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.client.internal.ActorRuntime
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.location.commons.ClusterSettings
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}

class ClientCliWiring(clusterSettings: ClusterSettings) {
  lazy val actorRuntime                     = new ActorRuntime()
  lazy val locationService: LocationService = LocationServiceFactory.withSettings(clusterSettings)
  lazy val configService: ConfigService     = ConfigClientFactory.adminApi(actorRuntime.actorSystem, locationService)
  lazy val commandLineRunner                = new CommandLineRunner(configService, actorRuntime)

  actorRuntime.coordinatedShutdown.addTask(
    CoordinatedShutdown.PhaseBeforeServiceUnbind,
    "location-service-shutdown"
  )(() ⇒ locationService.shutdown())
}
