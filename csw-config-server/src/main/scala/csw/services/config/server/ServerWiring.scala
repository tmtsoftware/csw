package csw.services.config.server

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.server.files._
import csw.services.config.server.http.{ConfigHandlers, ConfigServiceRoute, HttpService}
import csw.services.config.server.svn.{SvnConfigService, SvnRepo}
import csw.services.location.commons.ClusterSettings
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}

/**
 * Server configuration
 */
class ServerWiring {
  lazy val config: Config = ConfigFactory.load()
  lazy val settings       = new Settings(config)

  lazy val clusterSettings          = ClusterSettings()
  lazy val actorSystem: ActorSystem = clusterSettings.system
  lazy val actorRuntime             = new ActorRuntime(actorSystem, settings)
  lazy val annexFileRepo            = new AnnexFileRepo(actorRuntime.blockingIoDispatcher)
  lazy val svnRepo                  = new SvnRepo(settings, actorRuntime.blockingIoDispatcher)

  lazy val annexFileService             = new AnnexFileService(settings, annexFileRepo, actorRuntime)
  lazy val configService: ConfigService = new SvnConfigService(settings, annexFileService, actorRuntime, svnRepo)

  lazy val locationService: LocationService = LocationServiceFactory.withSystem(actorSystem)

  lazy val configHandlers     = new ConfigHandlers
  lazy val configServiceRoute = new ConfigServiceRoute(configService, actorRuntime, configHandlers)

  lazy val httpService: HttpService = new HttpService(locationService, configServiceRoute, settings, actorRuntime)
}

object ServerWiring {

  def make(_locationService: LocationService): ServerWiring = new ServerWiring {
    override lazy val locationService: LocationService = _locationService
  }

  def make(_clusterSettings: ClusterSettings): ServerWiring = new ServerWiring {
    override lazy val clusterSettings: ClusterSettings = _clusterSettings
  }

  def make(_clusterSettings: ClusterSettings, maybePort: Option[Int]): ServerWiring = new ServerWiring {
    override lazy val clusterSettings: ClusterSettings = _clusterSettings

    override lazy val settings: Settings = new Settings(config) {
      override val `service-port`: Int = maybePort.getOrElse(super.`service-port`)
    }
  }

  def make(_config: Config): ServerWiring = new ServerWiring {
    override lazy val config: Config = _config.withFallback(ConfigFactory.load())
  }
}
