package csw.ccs.internal

import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior}
import csw.ccs.CommandState
import csw.messages.CommandStatusMessages
import csw.messages.CommandStatusMessages._
import csw.messages.ccs.commands.CommandExecutionResponse.CommandNotAvailable
import csw.messages.ccs.commands._
import csw.messages.params.models.RunId
import csw.services.logging.scaladsl.ComponentLogger

class CommandStatusService(
    ctx: ActorContext[CommandStatusMessages],
    componentName: String
) extends ComponentLogger.MutableActor[CommandStatusMessages](ctx, componentName) {

  var cmdToCmdStatus: Map[RunId, CommandState] = Map.empty

  override def onMessage(msg: CommandStatusMessages): Behavior[CommandStatusMessages] = {
    msg match {
      case Subscribe(runId, replyTo)      ⇒ subscribe(runId, replyTo)
      case UnSubscribe(runId, replyTo)    ⇒ unSubscribe(runId, replyTo)
      case Query(runId, replyTo)          ⇒ sendCurrentState(runId, replyTo)
      case Update(runId, commandResponse) ⇒ updateCommandStatus(runId, commandResponse)
    }
    this
  }

  private def subscribe(
      runId: RunId,
      actorRef: ActorRef[CommandResponse]
  ): Unit =
    cmdToCmdStatus
      .get(runId)
      .foreach(
        cmdState ⇒
          cmdToCmdStatus = cmdToCmdStatus + (runId → cmdState.copy(subscribers = cmdState.subscribers + actorRef))
      )

  private def unSubscribe(
      runId: RunId,
      actorRef: ActorRef[CommandResponse]
  ): Unit =
    cmdToCmdStatus
      .get(runId)
      .foreach(
        cmdState ⇒
          cmdToCmdStatus = cmdToCmdStatus + (runId → cmdState.copy(subscribers = cmdState.subscribers - actorRef))
      )

  private def sendCurrentState(runId: RunId, replyTo: ActorRef[CommandResponse]): Unit = {
    val currentCmdState = cmdToCmdStatus.get(runId) match {
      case Some(cmdState) => cmdState.currentCmdStatus
      case None           => CommandNotAvailable(runId)
    }
    replyTo ! currentCmdState
  }

  def updateCommandStatus(runId: RunId, commandResponse: CommandResponse): Unit = {
    cmdToCmdStatus
      .get(runId)
      .foreach(cmdState ⇒ cmdToCmdStatus = cmdToCmdStatus + (runId → cmdState.copy(currentCmdStatus = commandResponse)))

    if (commandResponse.resultType.isInstanceOf[CommandResultType.Final]) cmdToCmdStatus(runId).sendStatus()

    cmdToCmdStatus(runId).publishStatus()
  }
}
