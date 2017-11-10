package csw.ccs.models

import akka.typed.ActorRef
import csw.messages.ccs.commands.CommandExecutionResponse.CommandNotAvailable
import csw.messages.ccs.commands.CommandResponse
import csw.messages.params.models.RunId

case class CommandStatusServiceState(cmdToCmdStatus: Map[RunId, CommandState]) {
  def add(runId: RunId, replyTo: ActorRef[CommandResponse]): CommandStatusServiceState =
    CommandStatusServiceState(cmdToCmdStatus.updated(runId, CommandState.init(runId, replyTo)))

  def get(runId: RunId): CommandResponse = cmdToCmdStatus.get(runId) match {
    case Some(cmdState) => cmdState.commandStatus.currentCmdStatus
    case None           => CommandNotAvailable(runId)
  }

  def updateCommandStatus(commandResponse: CommandResponse): CommandStatusServiceState =
    update(commandResponse.runId, _.withCommandStatus(commandResponse))

  def subscribe(runId: RunId, actorRef: ActorRef[CommandResponse]): CommandStatusServiceState =
    update(runId, _.addSubscriber(actorRef))

  def unSubscribe(runId: RunId, actorRef: ActorRef[CommandResponse]): CommandStatusServiceState =
    update(runId, _.removeSubscriber(actorRef))

  private def update(runId: RunId, f: CommandState ⇒ CommandState): CommandStatusServiceState =
    cmdToCmdStatus.get(runId) match {
      case Some(cmdState) ⇒ CommandStatusServiceState(cmdToCmdStatus.updated(runId, f(cmdState)))
      case None           ⇒ this
    }
}
