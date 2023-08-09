/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.internal.supervisor

import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import csw.command.client.messages.{ComponentMessage, ContainerIdleMessage, SupervisorMessage, TopLevelActorMessage}
import csw.framework.models.CswContext
import csw.framework.scaladsl.{ComponentHandlersFactory, ComponentHandlers, RegistrationFactory}

/**
 * The factory for creating [[pekko.actor.typed.scaladsl.AbstractBehavior]] of the supervisor of a component
 */
private[csw] object SupervisorBehaviorFactory {

  def make(
      containerRef: Option[ActorRef[ContainerIdleMessage]],
      registrationFactory: RegistrationFactory,
      cswCtx: CswContext
  ): Behavior[ComponentMessage] = {
    make(
      containerRef,
      registrationFactory,
      ComponentHandlersFactory.make(cswCtx.componentInfo.componentHandlerClassName).handlers,
      cswCtx
    )
  }

  def make(
      containerRef: Option[ActorRef[ContainerIdleMessage]],
      registrationFactory: RegistrationFactory,
      handlersFactory: (ActorContext[TopLevelActorMessage], CswContext) => ComponentHandlers,
      cswCtx: CswContext
  ): Behavior[ComponentMessage] = {
    val componentHandlersFactory: ComponentHandlersFactory = (ctx, cswCtx) => handlersFactory(ctx, cswCtx)
    Behaviors
      .withTimers[SupervisorMessage](timerScheduler =>
        Behaviors
          .setup[SupervisorMessage](ctx =>
            new SupervisorBehavior(
              ctx,
              timerScheduler,
              containerRef,
              componentHandlersFactory,
              registrationFactory,
              cswCtx
            )
          )
      )
      .narrow
  }
}
