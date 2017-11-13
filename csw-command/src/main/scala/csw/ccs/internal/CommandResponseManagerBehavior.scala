package csw.ccs.internal

import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior}
import csw.ccs.models.{CommandCoRelation, CommandResponseManagerState}
import csw.messages.CommandResponseManagerMessage
import csw.messages.CommandResponseManagerMessage._
import csw.messages.ccs.commands.{
  CommandExecutionResponse,
  CommandResponse,
  CommandResultType,
  CommandValidationResponse
}
import csw.messages.params.models.RunId
import csw.services.logging.scaladsl.ComponentLogger

class CommandResponseManagerBehavior(
    ctx: ActorContext[CommandResponseManagerMessage],
    componentName: String
) extends ComponentLogger.MutableActor[CommandResponseManagerMessage](ctx, componentName) {

  var commandStatus: CommandResponseManagerState = CommandResponseManagerState(Map.empty)
  var commandCoRelation: CommandCoRelation       = CommandCoRelation(Map.empty, Map.empty)

  override def onMessage(msg: CommandResponseManagerMessage): Behavior[CommandResponseManagerMessage] = {
    msg match {
      case AddCommand(runId, initialState)           ⇒ commandStatus = commandStatus.add(runId, initialState)
      case AddSubCommand(parentRunId, childRunId)    ⇒ addTo(parentRunId, childRunId)
      case UpdateCommand(commandId, cmdStatus)       ⇒ updateCommand(commandId, cmdStatus)
      case UpdateSubCommand(subCommandId, cmdStatus) ⇒ updateSubCommand(subCommandId, cmdStatus)
      case Query(runId, replyTo)                     ⇒ replyTo ! commandStatus.get(runId)
      case Subscribe(runId, replyTo)                 ⇒ subscribe(runId, replyTo)
      case UnSubscribe(runId, replyTo)               ⇒ commandStatus = commandStatus.unSubscribe(runId, replyTo)
    }
    this
  }

  private def addTo(parentRunId: RunId, childRunId: RunId): Unit =
    commandCoRelation = commandCoRelation.add(parentRunId, childRunId)

  private def updateCommand(commandId: RunId, commandResponse: CommandResponse): Unit = {
    commandStatus = commandStatus.updateCommandStatus(commandResponse)
    publishToSubscribers(commandResponse, commandStatus.cmdToCmdStatus(commandResponse.runId).subscribers)
  }

  private def updateSubCommand(subCommandId: RunId, commandResponse: CommandResponse): Unit = {
    // If the sub command has a parent command, fetch the current status of parent command from command status service
    commandCoRelation.childToParent
      .get(commandResponse.runId)
      .foreach(parentId ⇒ updateParent(parentId, commandResponse))
  }

  private def updateParent(parentRunId: RunId, childCommandResponse: CommandResponse): Unit =
    (commandStatus.get(parentRunId).resultType, childCommandResponse.resultType) match {
      // If the child command receives a negative result, the result of the parent command need not wait for the
      // result from other sub commands
      case (CommandResultType.Intermediate, CommandResultType.Negative) ⇒
        updateCommand(parentRunId, childCommandResponse.withRunId(parentRunId))
      // If the child command receives a positive result, the result of the parent command needs to be evaluated based
      // on the result from other sub commands
      case (CommandResultType.Intermediate, CommandResultType.Positive) ⇒
        updateParentForChild(parentRunId, childCommandResponse)
      case _ ⇒ // TODO: Implement this
    }

  private def updateParentForChild(parentRunId: RunId, childCommandResponse: CommandResponse): Unit =
    childCommandResponse match {
      case _: CommandExecutionResponse ⇒
        commandCoRelation = commandCoRelation.remove(parentRunId, childCommandResponse.runId)
        if (commandCoRelation.parentToChildren(parentRunId).isEmpty)
          updateCommand(parentRunId, childCommandResponse.withRunId(parentRunId))
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
