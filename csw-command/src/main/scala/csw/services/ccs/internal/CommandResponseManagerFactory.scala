package csw.services.ccs.internal

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import csw.messages.{CommandResponseManagerMessage, SupervisorMessage}
import csw.services.ccs.scaladsl.CommandResponseManager
import csw.services.logging.scaladsl.LoggerFactory

/**
 * The factory for creating [[csw.services.ccs.internal.CommandResponseManagerBehavior]]
 */
private[csw] class CommandResponseManagerFactory {

  def make(
      ctx: ActorContext[SupervisorMessage],
      commandResponseManagerActor: ActorRef[CommandResponseManagerMessage]
  ): CommandResponseManager = new CommandResponseManager(commandResponseManagerActor)(ctx.system)

  def makeBehavior(loggerFactory: LoggerFactory): Behavior[CommandResponseManagerMessage] =
    Behaviors.mutable[CommandResponseManagerMessage](ctx ⇒ new CommandResponseManagerBehavior(ctx, loggerFactory))

}
