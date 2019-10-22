package csw.config.server

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.{Config, ConfigFactory}
import csw.aas.http.SecurityDirectives
import csw.config.server.files._
import csw.config.server.http.{ConfigHandlers, ConfigServiceRoute, HttpService}
import csw.config.server.svn.{SvnConfigServiceFactory, SvnRepo}
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory

/**
 * Server configuration
 */
private[csw] class ServerWiring {
  lazy val config: Config = ConfigFactory.load()
  lazy val settings       = new Settings(config)

  lazy val actorSystem  = ActorSystem(SpawnProtocol(), "config-server")
  lazy val actorRuntime = new ActorRuntime(actorSystem, settings)
  import actorRuntime._

  lazy val annexFileRepo    = new AnnexFileRepo(actorRuntime.blockingIoDispatcher)
  lazy val annexFileService = new AnnexFileService(settings, annexFileRepo, actorRuntime)

  lazy val svnRepo              = new SvnRepo(settings.`svn-user-name`, settings, actorRuntime.blockingIoDispatcher)
  lazy val configServiceFactory = new SvnConfigServiceFactory(actorRuntime, annexFileService)

  lazy val locationService: LocationService =
    HttpLocationServiceFactory.makeLocalClient(actorSystem, actorRuntime.mat)

  lazy val configHandlers     = new ConfigHandlers
  lazy val securityDirectives = SecurityDirectives(config, locationService)
  lazy val configServiceRoute = new ConfigServiceRoute(configServiceFactory, actorRuntime, configHandlers, securityDirectives)

  lazy val httpService: HttpService = new HttpService(locationService, configServiceRoute, settings, actorRuntime)
}

private[csw] object ServerWiring {

  def make(maybePort: Option[Int]): ServerWiring = new ServerWiring {
    override lazy val settings: Settings = new Settings(config) {
      override val `service-port`: Int = maybePort.getOrElse(super.`service-port`)
    }
  }

  def make(_config: Config): ServerWiring = new ServerWiring {
    override lazy val config: Config = _config.withFallback(ConfigFactory.load())
  }

  def make(_locationService: LocationService, _securityDirectives: SecurityDirectives): ServerWiring = new ServerWiring {
    override lazy val locationService: LocationService       = _locationService
    override lazy val securityDirectives: SecurityDirectives = _securityDirectives
  }

  def make(_securityDirectives: SecurityDirectives): ServerWiring = new ServerWiring {
    override lazy val securityDirectives: SecurityDirectives = _securityDirectives
  }
}
