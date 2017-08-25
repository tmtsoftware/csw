package csw.common.framework.scaladsl

import akka.actor.ActorSystem
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import csw.common.framework.models.{ComponentInfo, SupervisorInfo}
import csw.services.location.scaladsl.LocationServiceFactory

class SupervisorFactory {
  def make(componentInfo: ComponentInfo): SupervisorInfo = {
    val system             = ActorSystem(s"${componentInfo.name}-system")
    val locationService    = LocationServiceFactory.withSystem(system)
    val supervisorBehavior = SupervisorBehaviorFactory.behavior(componentInfo, locationService)
    val supervisorRef      = system.spawn(supervisorBehavior, componentInfo.name)
    SupervisorInfo(system, supervisorRef, componentInfo)
  }
}
