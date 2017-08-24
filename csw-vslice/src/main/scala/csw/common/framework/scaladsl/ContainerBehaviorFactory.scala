package csw.common.framework.scaladsl

import akka.typed.Behavior
import akka.typed.scaladsl.Actor
import csw.common.framework.internal.Container
import csw.common.framework.models.{ContainerInfo, ContainerMsg}
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}

object ContainerBehaviorFactory {
  def behavior(containerInfo: ContainerInfo, locationService: LocationService): Behavior[ContainerMsg] = {
    val supervisorFactory   = new SupervisorFactory()
    val registrationFactory = new RegistrationFactory
    Actor.mutable(ctx ⇒ new Container(ctx, containerInfo, locationService, supervisorFactory, registrationFactory))
  }
}
