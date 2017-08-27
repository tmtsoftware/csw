package csw.common.framework.internal.supervisor

import akka.actor.ActorSystem
import akka.typed.ActorRef
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import csw.common.framework.models.{ComponentInfo, SupervisorExternalMessage}
import csw.services.location.scaladsl.{ActorSystemFactory, LocationServiceFactory, RegistrationFactory}

case class SupervisorInfo(
    system: ActorSystem,
    supervisor: ActorRef[SupervisorExternalMessage],
    componentInfo: ComponentInfo
)

class SupervisorInfoFactory {
  def make(componentInfo: ComponentInfo): SupervisorInfo = {
    val system              = ActorSystemFactory.remote(s"${componentInfo.name}-system")
    val locationService     = LocationServiceFactory.make()
    val registrationFactory = new RegistrationFactory
    val supervisorBehavior  = SupervisorBehaviorFactory.behavior(componentInfo, locationService, registrationFactory)
    val supervisorRef       = system.spawn(supervisorBehavior, componentInfo.name)
    SupervisorInfo(system, supervisorRef, componentInfo)
  }
}
