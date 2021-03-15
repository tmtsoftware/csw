package csw.framework.internal.supervisor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import csw.command.client.messages.{ComponentMessage, ContainerIdleMessage, SupervisorMessage}
import csw.framework.models.CswContext
import csw.framework.scaladsl.{ComponentBehaviorFactory, RegistrationFactory}
import csw.prefix.models.Prefix

/**
 * The factory for creating [[akka.actor.typed.scaladsl.AbstractBehavior]] of the supervisor of a component
 */
private[csw] object SupervisorBehaviorFactory {

  def make(
      containerRef: Option[ActorRef[ContainerIdleMessage]],
      registrationFactory: RegistrationFactory,
      cswCtx: CswContext,
      agentPrefix: Option[Prefix]
  ): Behavior[ComponentMessage] = {
    val componentWiringClass = Class.forName(cswCtx.componentInfo.behaviorFactoryClassName)
    val componentBehaviorFactory =
      componentWiringClass.getDeclaredConstructor().newInstance().asInstanceOf[ComponentBehaviorFactory]

    make(
      containerRef,
      registrationFactory,
      componentBehaviorFactory,
      cswCtx,
      agentPrefix
    )
  }

  // This method is used by test
  def make(
      containerRef: Option[ActorRef[ContainerIdleMessage]],
      registrationFactory: RegistrationFactory,
      componentBehaviorFactory: ComponentBehaviorFactory,
      cswCtx: CswContext,
      agentPrefix: Option[Prefix]
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
              cswCtx,
              agentPrefix
            )
          )
      )
      .narrow
  }
}
