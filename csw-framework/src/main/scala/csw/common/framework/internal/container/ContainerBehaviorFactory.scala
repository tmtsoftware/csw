package csw.common.framework.internal.container

import akka.typed.Behavior
import akka.typed.scaladsl.Actor
import csw.common.framework.internal.supervisor.SupervisorInfoFactory
import csw.common.framework.models.{ContainerInfo, ContainerMessage}
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}

object ContainerBehaviorFactory {
  def behavior(containerInfo: ContainerInfo, locationService: LocationService): Behavior[ContainerMessage] = {
    val supervisorFactory   = new SupervisorInfoFactory(containerInfo.name)
    val registrationFactory = new RegistrationFactory
    Actor.mutable(
      ctx ⇒ new ContainerBehavior(ctx, containerInfo, supervisorFactory, registrationFactory, locationService)
    )
  }
}
