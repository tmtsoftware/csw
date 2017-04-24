package csw.services.config.server

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.server.files._
import csw.services.config.server.http.{ConfigExceptionHandler, ConfigServiceRoute, HttpService}
import csw.services.config.server.svn.{SvnConfigService, SvnRepo}
import csw.services.location.commons.{ClusterSettings, CswCluster}
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}

class ServerWiring {
  lazy val config: Config = ConfigFactory.load()
  lazy val settings       = new Settings(config)

  lazy val actorSystem      = ActorSystem("config-service", config)
  lazy val actorRuntime     = new ActorRuntime(actorSystem, settings)
  lazy val oversizeFileRepo = new OversizeFileRepo(actorRuntime.blockingIoDispatcher)
  lazy val svnRepo          = new SvnRepo(settings, actorRuntime.blockingIoDispatcher)

  lazy val oversizeFileService          = new OversizeFileService(settings, oversizeFileRepo)
  lazy val configService: ConfigService = new SvnConfigService(settings, oversizeFileService, actorRuntime, svnRepo)

  lazy val clusterSettings                  = ClusterSettings()
  lazy val cswCluster: CswCluster           = CswCluster.withSettings(clusterSettings)
  lazy val locationService: LocationService = LocationServiceFactory.withCluster(cswCluster)

  lazy val configExceptionHandler = new ConfigExceptionHandler
  lazy val configServiceRoute     = new ConfigServiceRoute(configService, actorRuntime, configExceptionHandler)

  def makeHttpService(port: Int): HttpService =
    new HttpService(locationService, configServiceRoute, port, actorRuntime)
  lazy val httpService: HttpService = makeHttpService(settings.`service-port`)
}

object ServerWiring {
  def make(_locationService: LocationService): ServerWiring = new ServerWiring {
    override lazy val locationService: LocationService = _locationService
  }
  def make(_clusterSettings: ClusterSettings, maybePort: Option[Int]): ServerWiring = new ServerWiring {
    override lazy val clusterSettings: ClusterSettings = _clusterSettings
    override lazy val httpService: HttpService         = makeHttpService(maybePort.getOrElse(settings.`service-port`))
  }
}
