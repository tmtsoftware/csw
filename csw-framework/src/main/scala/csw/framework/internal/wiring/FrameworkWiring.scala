package csw.framework.internal.wiring

import akka.actor.ActorSystem
import akka.typed.ActorRef
import csw.apps.deployment.commons.ConfigUtils
import csw.framework.internal.pubsub.PubSubBehaviorFactory
import csw.services.config.api.scaladsl.ConfigClientService
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.location.commons.ClusterSettings
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory, RegistrationFactory}
import csw.services.logging.internal.LogControlMessages
import csw.services.logging.scaladsl.LogAdminActorFactory

class FrameworkWiring {
  lazy val clusterSettings: ClusterSettings               = ClusterSettings()
  lazy val actorSystem: ActorSystem                       = clusterSettings.system
  lazy val locationService: LocationService               = LocationServiceFactory.withSystem(actorSystem)
  lazy val actorRuntime: ActorRuntime                     = new ActorRuntime(actorSystem)
  lazy val logAdminActorRef: ActorRef[LogControlMessages] = LogAdminActorFactory.make(actorSystem)
  lazy val registrationFactory                            = new RegistrationFactory(logAdminActorRef)
  lazy val pubSubBehaviorFactory                          = new PubSubBehaviorFactory
  lazy val configClientService: ConfigClientService       = ConfigClientFactory.clientApi(actorSystem, locationService)
  lazy val configUtils: ConfigUtils                       = new ConfigUtils(configClientService, actorRuntime)
}

object FrameworkWiring {

  def make(_actorSystem: ActorSystem): FrameworkWiring = new FrameworkWiring {
    override lazy val actorSystem: ActorSystem = _actorSystem
  }

  def make(_actorSystem: ActorSystem, _locationService: LocationService): FrameworkWiring = new FrameworkWiring {
    override lazy val actorSystem: ActorSystem         = _actorSystem
    override lazy val locationService: LocationService = _locationService
  }
}
