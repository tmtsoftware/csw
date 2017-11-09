package csw.ccs.internal

import akka.typed.Behavior
import akka.typed.scaladsl.ActorContext
import csw.ccs.models.CommandStatus
import csw.messages.CommandStatusMessages._
import csw.messages.ccs.commands.{CommandResponse, CommandResultType}
import csw.messages.params.models.RunId
import csw.messages.{Add, CommandStatusMessages, Update}
import csw.services.logging.scaladsl.ComponentLogger

class CommandStatusService(
    ctx: ActorContext[CommandStatusMessages],
    componentName: String
) extends ComponentLogger.MutableActor[CommandStatusMessages](ctx, componentName) {

  var commandStatus: CommandStatus = CommandStatus(Map.empty)

  override def onMessage(msg: CommandStatusMessages): Behavior[CommandStatusMessages] = {
    msg match {
      case Query(runId, replyTo)          ⇒ replyTo ! commandStatus.get(runId)
      case Add(runId, replyTo)            ⇒ commandStatus.add(runId, replyTo)
      case Update(runId, commandResponse) ⇒ commandStatus.updateCommandStatus(runId, commandResponse)
      case Subscribe(runId, replyTo)      ⇒ commandStatus.subscribe(runId, replyTo)
      case UnSubscribe(runId, replyTo)    ⇒ commandStatus.unSubscribe(runId, replyTo)
    }
    this
  }

  def updateCommandStatus(runId: RunId, commandResponse: CommandResponse): Unit = {
    updateCommandStatus(runId, commandResponse)
    publishFinalStateToSender(runId, commandResponse)
    publishToSubscribers(runId)
  }

  private def publishToSubscribers(runId: RunId): Unit = {
    val commandState = commandStatus.cmdToCmdStatus(runId)
    commandState.subscribers.foreach(_ ! commandState.currentCmdStatus)
  }

  private def publishFinalStateToSender(runId: RunId, commandResponse: CommandResponse): Unit = {
    if (commandResponse.resultType.isInstanceOf[CommandResultType.Final]) {
      val commandState = commandStatus.cmdToCmdStatus(runId)
      commandState.replyTo ! commandState.currentCmdStatus
    }
  }
}
