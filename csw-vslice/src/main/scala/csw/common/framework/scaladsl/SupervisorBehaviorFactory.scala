package csw.common.framework.scaladsl

import akka.typed.Behavior
import akka.typed.scaladsl.Actor
import csw.common.framework.internal.Supervisor
import csw.common.framework.models.{ComponentInfo, SupervisorExternalMessage, SupervisorMsg}
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}

object SupervisorBehaviorFactory {

  def behavior(componentInfo: ComponentInfo,
               locationService: LocationService,
               registrationFactory: RegistrationFactory): Behavior[SupervisorExternalMessage] = {
    val componentWiringClass = Class.forName(componentInfo.className)
    val compWring            = componentWiringClass.newInstance().asInstanceOf[ComponentWiring[_]]
    Actor
      .withTimers[SupervisorMsg](
        timerScheduler ⇒
          Actor.mutable[SupervisorMsg](
            ctx => new Supervisor(ctx, timerScheduler, componentInfo, compWring, registrationFactory, locationService)
        )
      )
      .narrow
  }
}
