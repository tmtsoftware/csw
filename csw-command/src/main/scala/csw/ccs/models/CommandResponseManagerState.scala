package csw.ccs.models

import akka.typed.ActorRef
import csw.messages.ccs.commands.CommandExecutionResponse.CommandNotAvailable
import csw.messages.ccs.commands.CommandResponse
import csw.messages.params.models.RunId

case class CommandResponseManagerState(cmdToCmdStatus: Map[RunId, CommandState]) {
  def add(runId: RunId, initialState: CommandResponse) =
    CommandResponseManagerState(cmdToCmdStatus.updated(runId, CommandState.init(runId, initialState)))

  def get(runId: RunId): CommandResponse = cmdToCmdStatus.get(runId) match {
    case Some(cmdState) => cmdState.commandStatus.currentCmdStatus
    case None           => CommandNotAvailable(runId)
  }

  def updateCommandStatus(commandResponse: CommandResponse): CommandResponseManagerState =
    update(commandResponse.runId, _.withCommandStatus(commandResponse))

  def subscribe(runId: RunId, actorRef: ActorRef[CommandResponse]): CommandResponseManagerState =
    update(runId, _.addSubscriber(actorRef))

  def unSubscribe(runId: RunId, actorRef: ActorRef[CommandResponse]): CommandResponseManagerState =
    update(runId, _.removeSubscriber(actorRef))

  private def update(runId: RunId, f: CommandState ⇒ CommandState): CommandResponseManagerState =
    cmdToCmdStatus.get(runId) match {
      case Some(cmdState) ⇒ CommandResponseManagerState(cmdToCmdStatus.updated(runId, f(cmdState)))
      case None           ⇒ this
    }
}
