package csw.ccs.models

import akka.typed.ActorRef
import csw.messages.ccs.commands.CommandExecutionResponse.CommandNotAvailable
import csw.messages.ccs.commands.CommandResponse
import csw.messages.params.models.RunId

case class CommandStatus(cmdToCmdStatus: Map[RunId, CommandState]) {
  def add(runId: RunId, replyTo: ActorRef[CommandResponse]): CommandStatus =
    CommandStatus(cmdToCmdStatus.updated(runId, CommandState.init(runId, replyTo)))

  def get(runId: RunId): CommandResponse = cmdToCmdStatus.get(runId) match {
    case Some(cmdState) => cmdState.currentCmdStatus
    case None           => CommandNotAvailable(runId)
  }

  def updateCommandStatus(runId: RunId, commandResponse: CommandResponse): CommandStatus =
    update(runId, _.withCmdStatus(commandResponse))

  def subscribe(runId: RunId, actorRef: ActorRef[CommandResponse]): CommandStatus =
    update(runId, _.addSubscriber(actorRef))

  def unSubscribe(runId: RunId, actorRef: ActorRef[CommandResponse]): CommandStatus =
    update(runId, _.removeSubscriber(actorRef))

  private def update(runId: RunId, f: CommandState ⇒ CommandState): CommandStatus = cmdToCmdStatus.get(runId) match {
    case Some(cmdState) ⇒ CommandStatus(cmdToCmdStatus.updated(runId, f(cmdState)))
    case None           ⇒ this
  }
}
