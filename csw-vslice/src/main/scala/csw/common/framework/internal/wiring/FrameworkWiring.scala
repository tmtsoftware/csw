package csw.common.framework.internal.wiring

import akka.actor.ActorSystem
import csw.common.framework.internal.pubsub.PubSubBehaviorFactory
import csw.services.location.commons.ClusterSettings
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory, RegistrationFactory}

class FrameworkWiring {
  lazy val clusterSettings: ClusterSettings = ClusterSettings()
  lazy val actorSystem: ActorSystem         = clusterSettings.system
  lazy val locationService: LocationService = LocationServiceFactory.withSystem(actorSystem)
  lazy val actorRuntime: ActorRuntime       = new ActorRuntime(actorSystem)
  lazy val registrationFactory              = new RegistrationFactory
  lazy val pubSubBehaviorFactory            = new PubSubBehaviorFactory
}

object FrameworkWiring {

  def make(_actorSystem: ActorSystem): FrameworkWiring = new FrameworkWiring {
    override lazy val actorSystem: ActorSystem = _actorSystem
  }

  def make(_actorSystem: ActorSystem, _locationService: LocationService) = new FrameworkWiring {
    override lazy val actorSystem: ActorSystem         = _actorSystem
    override lazy val locationService: LocationService = _locationService
  }
}
