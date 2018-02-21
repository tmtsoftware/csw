package csw.services.ccs.internal

import akka.typed.scaladsl.{Actor, ActorContext}
import csw.messages.{CommandResponseManagerMessage, SupervisorMessage}
import csw.services.ccs.scaladsl.CommandResponseManager
import csw.services.logging.scaladsl.LoggerFactory

/**
 * The factory for creating [[csw.services.ccs.internal.CommandResponseManagerBehavior]]
 */
object CommandResponseManagerFactory {

  def make(
      ctx: ActorContext[SupervisorMessage],
      actorName: String,
      loggerFactory: LoggerFactory
  ): CommandResponseManager = {

    new CommandResponseManager(
      ctx.spawn(
        Actor.mutable[CommandResponseManagerMessage](ctx ⇒ new CommandResponseManagerBehavior(ctx, loggerFactory)),
        actorName
      )
    )(ctx.system)
  }

}
