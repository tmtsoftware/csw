package csw.ccs.internal

import akka.typed.ActorRef
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.messages.{CommandStatusMessages, SupervisorMessage}

object CommandStatusFactory {
  def make(
      ctx: ActorContext[SupervisorMessage],
      actorName: String,
      componentName: String
  ): ActorRef[CommandStatusMessages] = {
    ctx
      .spawn(
        Actor.mutable[CommandStatusMessages](ctx ⇒ new CommandStatusService(ctx, componentName)),
        actorName
      )
  }
}
