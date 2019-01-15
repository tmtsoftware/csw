package csw.command.client.internal

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import csw.command.client.CommandResponseManager
import csw.command.client.messages.CommandResponseManagerMessage
import csw.logging.core.scaladsl.LoggerFactory

/**
 * The factory for creating [[csw.command.client.internal.CommandResponseManagerBehavior]]
 */
class CommandResponseManagerFactory {

  def make(
      commandResponseManagerActor: ActorRef[CommandResponseManagerMessage]
  )(implicit actorSystem: ActorSystem): CommandResponseManager =
    new CommandResponseManager(commandResponseManagerActor)

  def makeBehavior(loggerFactory: LoggerFactory): Behavior[CommandResponseManagerMessage] =
    Behaviors.setup[CommandResponseManagerMessage](ctx ⇒ new CommandResponseManagerBehavior(ctx, loggerFactory))

}
