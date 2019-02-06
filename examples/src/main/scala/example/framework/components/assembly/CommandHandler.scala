package example.framework.components.assembly

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import csw.command.api.scaladsl.CommandService
import csw.config.api.models.ConfigData

trait CommandHandlerMsgs

object CommandHandler {
  def make(configData: ConfigData, runningIn: Option[CommandService]): Behavior[CommandHandlerMsgs] =
    Behaviors.setup(ctx ⇒ new CommandHandler(ctx, configData, runningIn))
}

class CommandHandler(ctx: ActorContext[CommandHandlerMsgs], configData: ConfigData, runningIn: Option[CommandService])
    extends AbstractBehavior[CommandHandlerMsgs] {
  override def onMessage(msg: CommandHandlerMsgs): Behavior[CommandHandlerMsgs] = ???
}
