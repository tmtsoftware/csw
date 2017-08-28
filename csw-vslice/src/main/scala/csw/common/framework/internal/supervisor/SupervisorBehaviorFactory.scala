package csw.common.framework.internal.supervisor

import akka.typed.scaladsl.Actor
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.models.{ComponentInfo, ContainerIdleMessage, SupervisorExternalMessage, SupervisorMessage}
import csw.common.framework.scaladsl.ComponentBehaviorFactory
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}

object SupervisorBehaviorFactory {

  def make(
      containerRef: Option[ActorRef[ContainerIdleMessage]],
      componentInfo: ComponentInfo,
      locationService: LocationService,
      registrationFactory: RegistrationFactory
  ): Behavior[SupervisorExternalMessage] = {
    val componentWiringClass = Class.forName(componentInfo.className)
    val compWring            = componentWiringClass.newInstance().asInstanceOf[ComponentBehaviorFactory[_]]
    Actor
      .withTimers[SupervisorMessage](
        timerScheduler ⇒
          Actor.mutable[SupervisorMessage](
            ctx =>
              new SupervisorBehavior(
                ctx,
                containerRef,
                timerScheduler,
                componentInfo,
                compWring,
                registrationFactory,
                locationService
            )
        )
      )
      .narrow
  }
}
