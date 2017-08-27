package csw.common.framework.internal.supervisor

import akka.typed.Behavior
import akka.typed.scaladsl.Actor
import csw.common.framework.models.{ComponentInfo, SupervisorExternalMessage, SupervisorMessage}
import csw.common.framework.scaladsl.ComponentBehaviorFactory
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}

object SupervisorBehaviorFactory {

  def behavior(componentInfo: ComponentInfo,
               locationService: LocationService,
               registrationFactory: RegistrationFactory): Behavior[SupervisorExternalMessage] = {
    val componentWiringClass = Class.forName(componentInfo.className)
    val compWring            = componentWiringClass.newInstance().asInstanceOf[ComponentBehaviorFactory[_]]
    Actor
      .withTimers[SupervisorMessage](
        timerScheduler ⇒
          Actor.mutable[SupervisorMessage](
            ctx =>
              new SupervisorBehavior(
                ctx,
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
