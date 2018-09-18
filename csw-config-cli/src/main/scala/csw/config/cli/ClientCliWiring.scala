package csw.config.cli

import akka.actor.ActorSystem
import csw.location.api.scaladsl.LocationService
import csw.config.api.scaladsl.ConfigService
import csw.config.client.internal.ActorRuntime
import csw.config.client.scaladsl.ConfigClientFactory
import csw.location.api.commons.ClusterSettings
import csw.location.commons.CswCluster
import csw.location.scaladsl.LocationServiceFactory

/**
 * ClientCliWiring lazily joins the akka cluster and starts the app. After joining the cluster, it first resolves the location
 * of config server using `ConfigServiceResolver` from `csw-config-client` and then starts the app catering cli features
 * over admin api of config service.
 *
 * @param actorSystem the ActorSystem used to join akka cluster
 */
private[config] class ClientCliWiring(actorSystem: ActorSystem) {
  lazy val actorRuntime                     = new ActorRuntime(actorSystem)
  lazy val locationService: LocationService = LocationServiceFactory.withCluster(CswCluster.withSystem(actorSystem))
  lazy val configService: ConfigService     = ConfigClientFactory.adminApi(actorRuntime.actorSystem, locationService)
  lazy val printLine: Any ⇒ Unit            = println
  lazy val commandLineRunner                = new CommandLineRunner(configService, actorRuntime, printLine)
  lazy val cliApp                           = new CliApp(commandLineRunner)
}

private[config] object ClientCliWiring {
  def noPrinting(_clusterSettings: ClusterSettings): ClientCliWiring = new ClientCliWiring(_clusterSettings.system) {
    override lazy val printLine: Any ⇒ Unit = _ ⇒ ()
  }
}
