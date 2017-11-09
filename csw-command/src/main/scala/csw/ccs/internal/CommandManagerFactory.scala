package csw.ccs.internal

import akka.typed.ActorRef
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.ccs.CommandManager
import csw.messages.{CommandManagerMessages, SupervisorMessage}

object CommandManagerFactory {
  def make(
      ctx: ActorContext[SupervisorMessage],
      actorName: String,
      componentName: String
  ): ActorRef[CommandManagerMessages] = {
    ctx
      .spawn(
        Actor.mutable[CommandManagerMessages](ctx ⇒ new CommandManager(ctx, componentName)),
        actorName
      )
  }

}
