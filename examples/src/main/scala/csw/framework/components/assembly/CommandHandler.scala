package csw.framework.components.assembly

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors.MutableBehavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import csw.services.command.scaladsl.CommandService
import csw.services.config.api.models.ConfigData

trait CommandHandlerMsgs

object CommandHandler {
  def make(configData: ConfigData, runningIn: Option[CommandService]): Behavior[CommandHandlerMsgs] =
    Behaviors.mutable(ctx ⇒ new CommandHandler(ctx, configData, runningIn))
}

class CommandHandler(ctx: ActorContext[CommandHandlerMsgs], configData: ConfigData, runningIn: Option[CommandService])
    extends MutableBehavior[CommandHandlerMsgs] {
  override def onMessage(msg: CommandHandlerMsgs): Behavior[CommandHandlerMsgs] = ???
}
