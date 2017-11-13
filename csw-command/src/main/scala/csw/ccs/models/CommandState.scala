package csw.ccs.models

import akka.typed.ActorRef
import csw.messages.ccs.commands.CommandResponse
import csw.messages.params.models.RunId

case class CommandState(commandStatus: CommandStatus, subscribers: Set[ActorRef[CommandResponse]]) {
  def addSubscriber(subscriber: ActorRef[CommandResponse]): CommandState = copy(subscribers = subscribers + subscriber)
  def removeSubscriber(subscriber: ActorRef[CommandResponse]): CommandState =
    copy(subscribers = subscribers - subscriber)
  def withCommandStatus(commandResponse: CommandResponse): CommandState =
    copy(commandStatus = this.commandStatus.withCommandResponse(commandResponse))
}

object CommandState {
  def init(runId: RunId, initialState: CommandResponse): CommandState =
    CommandState(CommandStatus(runId, initialState), Set.empty)
}
