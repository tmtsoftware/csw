package csw.common.framework.internal.wiring

import akka.actor.ActorSystem
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings, CswCluster}
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory, RegistrationFactory}

class FrameworkWiring {
  lazy val clusterSettings: ClusterSettings = ClusterSettings()
  lazy val actorSystem: ActorSystem         = clusterSettings.system
  lazy val locationService: LocationService =
    LocationServiceFactory.withSettings(clusterSettings)
  lazy val actorRuntime: ActorRuntime = new ActorRuntime(actorSystem)
  lazy val registrationFactory        = new RegistrationFactory
}

object FrameworkWiring {

  def make(_clusterSettings: ClusterSettings): FrameworkWiring = new FrameworkWiring {
    override lazy val clusterSettings = _clusterSettings
  }

  def make(_actorSystem: ActorSystem): FrameworkWiring = new FrameworkWiring {
    override lazy val actorSystem = _actorSystem
  }

  def make(): FrameworkWiring = new FrameworkWiring()
}
