package csw.framework.components.assembly

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.messages.scaladsl.ComponentMessage
import csw.services.config.api.models.ConfigData

trait CommandHandlerMsgs

object CommandHandler {
  def make(configData: ConfigData, runningIn: Option[ActorRef[ComponentMessage]]): Behavior[CommandHandlerMsgs] =
    Actor.mutable(ctx ⇒ new CommandHandler(ctx, configData, runningIn))
}

class CommandHandler(ctx: ActorContext[CommandHandlerMsgs], configData: ConfigData, runningIn: Option[ActorRef[ComponentMessage]])
    extends MutableBehavior[CommandHandlerMsgs] {
  override def onMessage(msg: CommandHandlerMsgs): Behavior[CommandHandlerMsgs] = ???
}
