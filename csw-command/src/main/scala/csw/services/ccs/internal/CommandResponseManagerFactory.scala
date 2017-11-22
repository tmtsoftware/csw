package csw.services.ccs.internal

import akka.typed.ActorRef
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.messages.{CommandResponseManagerMessage, SupervisorMessage}

object CommandResponseManagerFactory {
  def make(
      ctx: ActorContext[SupervisorMessage],
      actorName: String,
      componentName: String
  ): ActorRef[CommandResponseManagerMessage] = {
    ctx
      .spawn(
        Actor.mutable[CommandResponseManagerMessage](ctx ⇒ new CommandResponseManagerBehavior(ctx, componentName)),
        actorName
      )
  }
}
