package csw.framework.internal.supervisor

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import csw.command.client.messages.{ComponentMessage, ContainerIdleMessage, SupervisorMessage, TopLevelActorMessage}
import csw.framework.models.CswContext
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers, RegistrationFactory}

/**
 * The factory for creating [[akka.actor.typed.scaladsl.AbstractBehavior]] of the supervisor of a component
 */
private[csw] object SupervisorBehaviorFactory {

  def make(
      containerRef: Option[ActorRef[ContainerIdleMessage]],
      registrationFactory: RegistrationFactory,
      cswCtx: CswContext
  ): Behavior[ComponentMessage] = {
    val componentBehaviorFactory = ComponentBehaviorFactory.make(cswCtx.componentInfo.componentHandlerClassName)
    make(
      containerRef,
      registrationFactory,
      componentBehaviorFactory,
      cswCtx
    )
  }

  def make(
      containerRef: Option[ActorRef[ContainerIdleMessage]],
      registrationFactory: RegistrationFactory,
      behaviorFactory: (ActorContext[TopLevelActorMessage], CswContext) => ComponentHandlers,
      cswCtx: CswContext
  ): Behavior[ComponentMessage] = {
    val bf: ComponentBehaviorFactory = (ctx, cswCtx) => behaviorFactory(ctx, cswCtx)
    make(containerRef, registrationFactory, bf, cswCtx)
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
