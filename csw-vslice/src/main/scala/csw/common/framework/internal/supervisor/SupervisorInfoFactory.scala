package csw.common.framework.internal.supervisor

import akka.typed.ActorRef
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import csw.common.framework.models.{Component, ComponentInfo, ContainerIdleMessage, SupervisorInfo}
import csw.services.location.scaladsl.{ActorSystemFactory, LocationService, RegistrationFactory}

class SupervisorInfoFactory {

  def make(
      containerRef: ActorRef[ContainerIdleMessage],
      componentInfo: ComponentInfo,
      locationService: LocationService
  ): SupervisorInfo = {
    val system              = ActorSystemFactory.remote(s"${componentInfo.name}-system")
    val registrationFactory = new RegistrationFactory
    val supervisorBehavior =
      SupervisorBehaviorFactory.make(Some(containerRef), componentInfo, locationService, registrationFactory)
    val supervisorRef = system.spawn(supervisorBehavior, componentInfo.name)
    SupervisorInfo(system, Component(supervisorRef, componentInfo))
  }

}
