package csw.services.ccs.internal

import akka.typed.ActorRef
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.messages.{CommandResponseManagerMessage, SupervisorMessage}
import csw.services.logging.scaladsl.LoggerFactory

object CommandResponseManagerFactory {
  def make(
      ctx: ActorContext[SupervisorMessage],
      actorName: String,
      loggerFactory: LoggerFactory
  ): ActorRef[CommandResponseManagerMessage] = {
    ctx.spawn(
      Actor.mutable[CommandResponseManagerMessage](ctx ⇒ new CommandResponseManagerBehavior(ctx, loggerFactory)),
      actorName
    )
  }
}
