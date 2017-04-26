package csw.services.csclient.cli

import akka.Done
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.client.internal.ActorRuntime
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.location.commons.ClusterSettings
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}

class Wiring(clusterSettings: ClusterSettings) {
  lazy val actorRuntime                     = new ActorRuntime()
  lazy val locationService: LocationService = LocationServiceFactory.withSettings(clusterSettings)
  lazy val configService: ConfigService     = ConfigClientFactory.make(actorRuntime.actorSystem, locationService)
  lazy val commandLineRunner                = new CommandLineRunner(configService, actorRuntime)

  def shutdown(): Done = {
    Block.await(actorRuntime.actorSystem.terminate())
    Block.await(locationService.shutdown())
  }
}
