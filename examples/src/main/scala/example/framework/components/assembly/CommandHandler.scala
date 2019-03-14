package example.framework.components.assembly

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import csw.command.api.scaladsl.CommandService
import csw.config.api.models.ConfigData

trait CommandHandlerMsgs

object CommandHandler {
  def behavior(configData: ConfigData, runningIn: Option[CommandService]): Behavior[CommandHandlerMsgs] =
    Behaviors.setup { ctx ⇒
      // setup required for actor

      Behaviors.receiveMessage {
        case _ ⇒ // Handle messages and return new behavior
          Behaviors.same
      }
    }
}
