package csw.ccs

import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior}
import csw.ccs.models.CommandManagerState
import csw.messages.CommandManagerExternalMessages.{AddTo, UpdateSubCommand}
import csw.messages.CommandManagerMessages.CommandResponseE
import csw.messages.CommandStatusMessages.Query
import csw.messages._
import csw.messages.ccs.commands._
import csw.messages.params.models.RunId
import csw.services.logging.scaladsl.ComponentLogger

class CommandManager(
    ctx: ActorContext[CommandManagerMessages],
    commandStatusService: ActorRef[CommandStatusMessages],
    componentName: String
) extends ComponentLogger.MutableActor[CommandManagerMessages](ctx, componentName) {

  var commandManagerState = CommandManagerState(Map.empty, Map.empty)

  override def onMessage(msg: CommandManagerMessages): Behavior[CommandManagerMessages] = {
    msg match {
      case Add(runId, initialState)       ⇒ add(runId, initialState)
      case AddTo(runIdParent, runIdChild) ⇒ addTo(runIdParent, runIdChild)
      case UpdateCommand(cmdStatus)       ⇒ updateCommand(cmdStatus)
      case UpdateSubCommand(cmdStatus)    ⇒ updateSubCommand(cmdStatus)
      case CommandResponseE(commandResponseParent, commandResponseChild) ⇒
        updateParent(commandResponseParent, commandResponseChild)
    }
    this
  }

  private def add(runId: RunId, initialState: CommandResponse): Unit =
    commandStatusService ! Add(runId, initialState)

  private def addTo(runIdParent: RunId, runIdChild: RunId): Unit =
    commandManagerState = commandManagerState.add(runIdParent, runIdChild)

  def updateCommand(commandResponse: CommandResponse): Unit = {
    // Update the state of parent command, if it exists, directly in the command status service
    commandManagerState.parentToChildren.get(commandResponse.runId) match {
      case Some(_) ⇒ commandStatusService ! UpdateCommand(commandResponse)
      case None    ⇒ //TODO: Implement this
    }
  }

  private def updateSubCommand(commandResponse: CommandResponse): Unit = {
    // If the sub command has a parent command, fetch the current status of parent command from command status service
    commandManagerState.childToParent
      .get(commandResponse.runId)
      .foreach(commandStatusService ! Query(_, ctx.spawnAdapter(CommandResponseE(_, commandResponse))))
  }

  private def updateParent(parentCommandResponse: CommandResponse, childCommandResponse: CommandResponse): Unit =
    (parentCommandResponse.resultType, childCommandResponse.resultType) match {
      // If the child command receives a negative result, the result of the parent command need not wait for the
      // result from other sub commands
      case (CommandResultType.Intermediate, CommandResultType.Negative) ⇒
        updateCommand(childCommandResponse)
      // If the child command receives a positive result, the result of the parent command needs to be evaluated based
      // on the result from other sub commands
      case (CommandResultType.Intermediate, CommandResultType.Positive) ⇒
        updateParentForChild(parentCommandResponse.runId, childCommandResponse)
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
}
