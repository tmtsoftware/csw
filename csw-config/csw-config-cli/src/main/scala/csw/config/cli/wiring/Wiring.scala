package csw.config.cli.wiring

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import csw.aas.installed.InstalledAppAuthAdapterFactory
import csw.aas.installed.api.InstalledAppAuthAdapter
import csw.aas.installed.scaladsl.FileAuthStore
import csw.config.api.TokenFactory
import csw.config.api.scaladsl.ConfigService
import csw.config.cli.{CliApp, CliTokenFactory, CommandLineRunner}
import csw.config.client.internal.ActorRuntime
import csw.config.client.scaladsl.ConfigClientFactory
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory

/**
 * ClientCliWiring lazily joins the akka cluster and starts the app. After joining the cluster, it first resolves the location
 * of config server using `ConfigServiceResolver` from `csw-config-client` and then starts the app catering cli features
 * over admin api of config service.
 */
private[config] class Wiring {
  lazy val config: Config = ConfigFactory.load()
  lazy val settings       = new Settings(config)
  lazy val actorSystem    = ActorSystem("config-cli")
  lazy val actorRuntime   = new ActorRuntime(actorSystem)
  import actorRuntime._

  lazy val locationService: LocationService           = HttpLocationServiceFactory.makeLocalClient(actorSystem, actorRuntime.mat)
  lazy val authStore                                  = new FileAuthStore(settings.authStorePath)
  lazy val nativeAuthAdapter: InstalledAppAuthAdapter = InstalledAppAuthAdapterFactory.make(locationService, authStore)
  lazy val tokenFactory: TokenFactory                 = new CliTokenFactory(nativeAuthAdapter)
  lazy val configService: ConfigService               = ConfigClientFactory.adminApi(actorRuntime.actorSystem, locationService, tokenFactory)
  lazy val printLine: Any ⇒ Unit                      = println
  lazy val commandLineRunner                          = new CommandLineRunner(configService, actorRuntime, printLine, nativeAuthAdapter)
  lazy val cliApp                                     = new CliApp(commandLineRunner)
}

private[config] object Wiring {

  def make(locationHost: String): Wiring = new Wiring {
    override lazy val locationService: LocationService =
      HttpLocationServiceFactory.make(locationHost)(actorSystem, actorRuntime.mat)
  }

  def noPrinting(_config: Config): Wiring =
    new Wiring {
      override lazy val printLine: Any ⇒ Unit = _ ⇒ ()
      override lazy val config: Config        = _config.withFallback(ConfigFactory.load())
    }

  def noPrinting(_locationService: LocationService, _tokenFactory: TokenFactory): Wiring =
    new Wiring {
      override lazy val locationService: LocationService = _locationService
      override lazy val tokenFactory: TokenFactory       = _tokenFactory
      override lazy val printLine: Any ⇒ Unit            = _ ⇒ ()
    }

  def noPrinting(
      _locationService: LocationService,
      _tokenFactory: TokenFactory,
      _nativeAuthAdapter: InstalledAppAuthAdapter
  ): Wiring =
    new Wiring {
      override lazy val nativeAuthAdapter: InstalledAppAuthAdapter = _nativeAuthAdapter
      override lazy val locationService: LocationService           = _locationService
      override lazy val tokenFactory: TokenFactory                 = _tokenFactory
      override lazy val printLine: Any ⇒ Unit                      = _ ⇒ ()
    }
}
