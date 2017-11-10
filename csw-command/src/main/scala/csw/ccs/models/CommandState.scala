package csw.ccs.models

import akka.typed.ActorRef
import csw.messages.ccs.commands.CommandExecutionResponse.Initialized
import csw.messages.ccs.commands.CommandResponse
import csw.messages.params.models.RunId

case class CommandState(
    commandStatus: CommandStatus,
    replyTo: ActorRef[CommandResponse],
    subscribers: Set[ActorRef[CommandResponse]]
) {
  def addSubscriber(subscriber: ActorRef[CommandResponse]): CommandState = copy(subscribers = subscribers + subscriber)
  def removeSubscriber(subscriber: ActorRef[CommandResponse]): CommandState =
    copy(subscribers = subscribers - subscriber)
  def withCommandStatus(commandResponse: CommandResponse): CommandState =
    copy(commandStatus = this.commandStatus.withCommandResponse(commandResponse))
}

object CommandState {
  def init(runId: RunId, replyTo: ActorRef[CommandResponse]): CommandState =
    CommandState(CommandStatus(runId, Initialized(runId)), replyTo, Set.empty)
}
