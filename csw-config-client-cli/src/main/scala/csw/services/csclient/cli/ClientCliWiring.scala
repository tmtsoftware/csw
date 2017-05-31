package csw.services.csclient.cli

import akka.actor.ActorSystem
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.client.internal.ActorRuntime
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.location.commons.{ClusterSettings, CswCluster}
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}

class ClientCliWiring(actorSystem: ActorSystem) {
  lazy val actorRuntime                     = new ActorRuntime(actorSystem)
  lazy val locationService: LocationService = LocationServiceFactory.withCluster(CswCluster.withSystem(actorSystem))
  lazy val configService: ConfigService     = ConfigClientFactory.adminApi(actorRuntime.actorSystem, locationService)
  lazy val printLine: Any ⇒ Unit            = println
  lazy val commandLineRunner                = new CommandLineRunner(configService, actorRuntime, printLine)
  lazy val cliApp                           = new CliApp(commandLineRunner)
}

object ClientCliWiring {
  def noPrinting(_clusterSettings: ClusterSettings) = new ClientCliWiring(_clusterSettings.system) {
    override lazy val printLine: Any ⇒ Unit = _ ⇒ ()
  }
}
