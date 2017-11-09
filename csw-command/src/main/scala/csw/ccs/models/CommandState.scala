package csw.ccs.models

import akka.typed.ActorRef
import csw.messages.ccs.commands.CommandExecutionResponse.Initialized
import csw.messages.ccs.commands.CommandResponse
import csw.messages.params.models.RunId

case class CommandState(
    runId: RunId,
    replyTo: ActorRef[CommandResponse],
    subscribers: Set[ActorRef[CommandResponse]],
    currentCmdStatus: CommandResponse
) {
  def addSubscriber(subscriber: ActorRef[CommandResponse]): CommandState = copy(subscribers = subscribers + subscriber)
  def removeSubscriber(subscriber: ActorRef[CommandResponse]): CommandState =
    copy(subscribers = subscribers - subscriber)
  def withCmdStatus(commandResponse: CommandResponse): CommandState = copy(currentCmdStatus = commandResponse)
}

object CommandState {
  def init(runId: RunId, replyTo: ActorRef[CommandResponse]): CommandState =
    CommandState(runId, replyTo, Set.empty, Initialized(runId))
}
