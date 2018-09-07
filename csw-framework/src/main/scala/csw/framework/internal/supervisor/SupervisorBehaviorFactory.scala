package csw.framework.internal.supervisor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import csw.framework.models.CswServices
import csw.framework.scaladsl.ComponentBehaviorFactory
import csw.messages.{ComponentMessage, ContainerIdleMessage, SupervisorMessage}
import csw.services.location.scaladsl.RegistrationFactory

/**
 * The factory for creating [[akka.actor.typed.scaladsl.MutableBehavior]] of the supervisor of a component
 */
private[framework] object SupervisorBehaviorFactory {

  def make(
      containerRef: Option[ActorRef[ContainerIdleMessage]],
      registrationFactory: RegistrationFactory,
      cswServices: CswServices
  ): Behavior[ComponentMessage] = {
    val componentWiringClass = Class.forName(cswServices.componentInfo.behaviorFactoryClassName)
    val componentBehaviorFactory =
      componentWiringClass.getDeclaredConstructor().newInstance().asInstanceOf[ComponentBehaviorFactory]

    make(
      containerRef,
      registrationFactory,
      componentBehaviorFactory,
      cswServices
    )
  }

  // This method is used by test
  def make(
      containerRef: Option[ActorRef[ContainerIdleMessage]],
      registrationFactory: RegistrationFactory,
      componentBehaviorFactory: ComponentBehaviorFactory,
      cswServices: CswServices
  ): Behavior[ComponentMessage] = {
    Behaviors
      .withTimers[SupervisorMessage](
        timerScheduler ⇒
          Behaviors
            .setup[SupervisorMessage](
              ctx =>
                new SupervisorBehavior(
                  ctx,
                  timerScheduler,
                  containerRef,
                  componentBehaviorFactory,
                  registrationFactory,
                  cswServices
              )
          )
      )
      .narrow
  }
}
