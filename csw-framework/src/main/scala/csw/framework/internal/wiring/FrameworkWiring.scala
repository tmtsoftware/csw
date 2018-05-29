package csw.framework.internal.wiring

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import csw.framework.deploy.ConfigUtils
import csw.services.command.internal.CommandResponseManagerFactory
import csw.services.config.api.scaladsl.ConfigClientService
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.location.commons.ClusterSettings
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory, RegistrationFactory}
import csw.services.logging.commons.LogAdminActorFactory
import csw.services.logging.messages.LogControlMessages

/**
 * Represents a class that lazily initializes necessary instances to run a component(s)
 */
class FrameworkWiring {
  lazy val clusterSettings: ClusterSettings               = ClusterSettings()
  lazy val actorSystem: ActorSystem                       = clusterSettings.system
  lazy val locationService: LocationService               = LocationServiceFactory.withSystem(actorSystem)
  lazy val actorRuntime: ActorRuntime                     = new ActorRuntime(actorSystem)
  lazy val logAdminActorRef: ActorRef[LogControlMessages] = LogAdminActorFactory.make(actorSystem)
  lazy val registrationFactory                            = new RegistrationFactory(logAdminActorRef)
  lazy val commandResponseManagerFactory                  = new CommandResponseManagerFactory
  lazy val configClientService: ConfigClientService       = ConfigClientFactory.clientApi(actorSystem, locationService)
  lazy val configUtils: ConfigUtils                       = new ConfigUtils(configClientService, actorRuntime)
}

/**
 * Represents the factory to make framework wiring
 */
object FrameworkWiring {

  /**
   * Makes a FrameworkWiring with the given actor system
   *
   * @param _actorSystem used to initialize other necessary instances like locationService
   * @return a FrameworkWiring containing instances to run a component(s)
   */
  def make(_actorSystem: ActorSystem): FrameworkWiring = new FrameworkWiring {
    override lazy val actorSystem: ActorSystem = _actorSystem
  }

  /**
   * Makes a FrameworkWiring with the given actor system
   *
   * @param _actorSystem used to initialize other necessary instances like locationService
   * @param _locationService used to initialize other necessary instances like configuration service client
   * @return a FrameworkWiring containing instances to run a component(s)
   */
  def make(_actorSystem: ActorSystem, _locationService: LocationService): FrameworkWiring = new FrameworkWiring {
    override lazy val actorSystem: ActorSystem         = _actorSystem
    override lazy val locationService: LocationService = _locationService
  }
}
