package csw.ccs.internal

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.ActorContext
import csw.ccs.models.CommandStatusServiceState
import csw.messages.CommandStatusMessages._
import csw.messages.ccs.commands.CommandResponse
import csw.messages.params.models.RunId
import csw.messages.{Add, CommandStatusMessages, UpdateCommand}
import csw.services.logging.scaladsl.ComponentLogger

class CommandStatusService(
    ctx: ActorContext[CommandStatusMessages],
    componentName: String
) extends ComponentLogger.MutableActor[CommandStatusMessages](ctx, componentName) {

  var commandStatus: CommandStatusServiceState = CommandStatusServiceState(Map.empty)

  override def onMessage(msg: CommandStatusMessages): Behavior[CommandStatusMessages] = {
    msg match {
      case Add(runId, initialState)       ⇒ commandStatus.add(runId, initialState)
      case UpdateCommand(commandResponse) ⇒ updateCommandStatus(commandResponse)
      case Query(runId, replyTo)          ⇒ replyTo ! commandStatus.get(runId)
      case Subscribe(runId, replyTo)      ⇒ subscribe(runId, replyTo)
      case UnSubscribe(runId, replyTo)    ⇒ commandStatus.unSubscribe(runId, replyTo)
    }
    this
  }

  def updateCommandStatus(commandResponse: CommandResponse): Unit = {
    commandStatus.updateCommandStatus(commandResponse)
    publishToSubscribers(commandResponse.runId)
  }

  private def publishToSubscribers(runId: RunId): Unit = {
    val commandState = commandStatus.cmdToCmdStatus(runId)
    commandState.subscribers.foreach(_ ! commandState.commandStatus.currentCmdStatus)
  }

  private def subscribe(runId: RunId, replyTo: ActorRef[CommandResponse]): Unit = {
    commandStatus.subscribe(runId, replyTo)
    replyTo ! commandStatus.get(runId)
  }
}
