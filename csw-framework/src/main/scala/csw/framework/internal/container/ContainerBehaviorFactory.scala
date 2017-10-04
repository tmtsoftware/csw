package csw.framework.internal.container

import akka.typed.scaladsl.Actor
import akka.typed.{ActorRef, Behavior}
import csw.framework.internal.supervisor.SupervisorInfoFactory
import csw.framework.models.ContainerInfo
import csw.messages.ContainerMessage
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}
import csw.services.logging.internal.LogControlMessages

object ContainerBehaviorFactory {
  def behavior(
      containerInfo: ContainerInfo,
      locationService: LocationService,
      adminActorRef: ActorRef[LogControlMessages]
  ): Behavior[ContainerMessage] = {
    val supervisorFactory   = new SupervisorInfoFactory(containerInfo.name)
    val registrationFactory = new RegistrationFactory
    Actor.mutable(
      ctx ⇒
        new ContainerBehavior(
          ctx,
          containerInfo,
          supervisorFactory,
          registrationFactory,
          locationService,
          adminActorRef
      )
    )
  }
}
