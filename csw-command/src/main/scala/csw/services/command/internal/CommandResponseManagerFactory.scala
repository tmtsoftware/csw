package csw.services.command.internal

import akka.actor.ActorSystem
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import csw.messages.CommandResponseManagerMessage
import csw.services.command.CommandResponseManager
import csw.services.logging.scaladsl.LoggerFactory

/**
 * The factory for creating [[csw.services.command.internal.CommandResponseManagerBehavior]]
 */
private[csw] class CommandResponseManagerFactory {

  def make(
      commandResponseManagerActor: ActorRef[CommandResponseManagerMessage]
  )(implicit actorSystem: ActorSystem): CommandResponseManager =
    new CommandResponseManager(commandResponseManagerActor)

  def makeBehavior(loggerFactory: LoggerFactory): Behavior[CommandResponseManagerMessage] =
    Behaviors.setup[CommandResponseManagerMessage](ctx ⇒ new CommandResponseManagerBehavior(ctx, loggerFactory))

}
