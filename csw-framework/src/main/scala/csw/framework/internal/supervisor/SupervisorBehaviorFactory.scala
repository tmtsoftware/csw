package csw.framework.internal.supervisor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import csw.command.client.messages.{ComponentMessage, ContainerIdleMessage, SupervisorMessage}
import csw.framework.models.CswContext
import csw.framework.scaladsl.{ComponentBehaviorFactory, RegistrationFactory}

/**
 * The factory for creating [[akka.actor.typed.scaladsl.AbstractBehavior]] of the supervisor of a component
 */
private[csw] object SupervisorBehaviorFactory {

  def make(
      containerRef: Option[ActorRef[ContainerIdleMessage]],
      registrationFactory: RegistrationFactory,
      cswCtx: CswContext
  ): Behavior[ComponentMessage] = {
    val componentHandlerClass    = Class.forName(cswCtx.componentInfo.componentHandlerClassName)
    val componentBehaviorFactory = ComponentBehaviorFactory.make(componentHandlerClass)
    make(
      containerRef,
      registrationFactory,
      componentBehaviorFactory,
      cswCtx
    )
  }

  // This method is used by test
  def make(
      containerRef: Option[ActorRef[ContainerIdleMessage]],
      registrationFactory: RegistrationFactory,
      componentBehaviorFactory: ComponentBehaviorFactory,
      cswCtx: CswContext
  ): Behavior[ComponentMessage] = {
    Behaviors
      .withTimers[SupervisorMessage](timerScheduler =>
        Behaviors
          .setup[SupervisorMessage](ctx =>
            new SupervisorBehavior(
              ctx,
              timerScheduler,
              containerRef,
              componentBehaviorFactory,
              registrationFactory,
              cswCtx
            )
          )
      )
      .narrow
  }
}
