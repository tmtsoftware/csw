package csw.framework.components.assembly

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, MutableBehavior}
import csw.command.scaladsl.CommandService
import csw.services.config.api.models.ConfigData

trait CommandHandlerMsgs

object CommandHandler {
  def make(configData: ConfigData, runningIn: Option[CommandService]): Behavior[CommandHandlerMsgs] =
    Behaviors.setup(ctx ⇒ new CommandHandler(ctx, configData, runningIn))
}

class CommandHandler(ctx: ActorContext[CommandHandlerMsgs], configData: ConfigData, runningIn: Option[CommandService])
    extends MutableBehavior[CommandHandlerMsgs] {
  override def onMessage(msg: CommandHandlerMsgs): Behavior[CommandHandlerMsgs] = ???
}
