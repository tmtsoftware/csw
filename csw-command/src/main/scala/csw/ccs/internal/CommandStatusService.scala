package csw.ccs.internal

import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior}
import csw.ccs.models.{CommandManagerState, CommandStatusServiceState}
import csw.messages.CommandStatusMessages
import csw.messages.CommandStatusMessages._
import csw.messages.ccs.commands.{
  CommandExecutionResponse,
  CommandResponse,
  CommandResultType,
  CommandValidationResponse
}
import csw.messages.params.models.RunId
import csw.services.logging.scaladsl.ComponentLogger

class CommandStatusService(
    ctx: ActorContext[CommandStatusMessages],
    componentName: String
) extends ComponentLogger.MutableActor[CommandStatusMessages](ctx, componentName) {

  var commandStatus: CommandStatusServiceState = CommandStatusServiceState(Map.empty)
  var commandManagerState: CommandManagerState = CommandManagerState(Map.empty, Map.empty)

  override def onMessage(msg: CommandStatusMessages): Behavior[CommandStatusMessages] = {
    msg match {
      case AddCommand(runId, initialState)        ⇒ commandStatus = commandStatus.add(runId, initialState)
      case AddSubCommand(runIdParent, runIdChild) ⇒ addTo(runIdParent, runIdChild)
      case UpdateCommand(cmdStatus)               ⇒ updateCommand(cmdStatus)
      case UpdateSubCommand(cmdStatus)            ⇒ updateSubCommand(cmdStatus)
      case Query(runId, replyTo)                  ⇒ replyTo ! commandStatus.get(runId)
      case Subscribe(runId, replyTo)              ⇒ subscribe(runId, replyTo)
      case UnSubscribe(runId, replyTo)            ⇒ commandStatus = commandStatus.unSubscribe(runId, replyTo)
    }
    this
  }

  private def addTo(runIdParent: RunId, runIdChild: RunId): Unit =
    commandManagerState = commandManagerState.add(runIdParent, runIdChild)

  private def updateCommand(commandResponse: CommandResponse): Unit = {
    commandStatus = commandStatus.updateCommandStatus(commandResponse)
    publishToSubscribers(commandResponse, commandStatus.cmdToCmdStatus(commandResponse.runId).subscribers)
  }

  private def updateSubCommand(commandResponse: CommandResponse): Unit = {
    // If the sub command has a parent command, fetch the current status of parent command from command status service
    commandManagerState.childToParent
      .get(commandResponse.runId)
      .foreach(parentId ⇒ updateParent(parentId, commandResponse))
  }

  private def updateParent(parentId: RunId, childCommandResponse: CommandResponse): Unit =
    (commandStatus.get(parentId).resultType, childCommandResponse.resultType) match {
      // If the child command receives a negative result, the result of the parent command need not wait for the
      // result from other sub commands
      case (CommandResultType.Intermediate, CommandResultType.Negative) ⇒
        updateCommand(childCommandResponse)
      // If the child command receives a positive result, the result of the parent command needs to be evaluated based
      // on the result from other sub commands
      case (CommandResultType.Intermediate, CommandResultType.Positive) ⇒
        updateParentForChild(parentId, childCommandResponse)
      case _ ⇒ // TODO: Implement this
    }

  private def updateParentForChild(parentId: RunId, childCommandResponse: CommandResponse): Unit =
    childCommandResponse match {
      case _: CommandExecutionResponse ⇒
        commandManagerState = commandManagerState.remove(parentId, childCommandResponse.runId)
        if (commandManagerState.parentToChildren(parentId).isEmpty)
          updateCommand(childCommandResponse)
      case _ ⇒
    }

  private def publishToSubscribers(
      commandResponse: CommandResponse,
      subscribers: Set[ActorRef[CommandResponse]]
  ): Unit = {
    commandResponse match {
      case _: CommandExecutionResponse ⇒
        subscribers.foreach(_ ! commandResponse)
      case _: CommandValidationResponse ⇒ // Do not send updates for validation response as it is send by the framework
    }
  }

  private def subscribe(runId: RunId, replyTo: ActorRef[CommandResponse]): Unit = {
    commandStatus = commandStatus.subscribe(runId, replyTo)
    publishToSubscribers(commandStatus.get(runId), Set(replyTo))
  }
}
