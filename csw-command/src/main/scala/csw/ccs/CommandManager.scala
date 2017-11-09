package csw.ccs

import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior}
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

  val parentToChildren: Map[RunId, Set[RunId]] = Map.empty
  val childToParent: Map[RunId, RunId]         = Map.empty

  override def onMessage(msg: CommandManagerMessages): Behavior[CommandManagerMessages] = {
    msg match {
      case Add(runId, replyTo)            ⇒ add(runId, replyTo)
      case AddTo(runIdParent, runIdChild) ⇒ addTo(runIdParent, runIdChild)
      case UpdateCommand(cmdStatus)       ⇒ updateCommand(cmdStatus)
      case UpdateSubCommand(cmdStatus)    ⇒ updateSubCommand(cmdStatus)
      case CommandResponseE(commandResponseParent, commandResponseChild) ⇒
        updateParent(commandResponseParent, commandResponseChild)
    }
    this
  }

  private def add(runId: RunId, replyTo: ActorRef[CommandResponse]): Unit =
    commandStatusService ! Add(runId, replyTo)

  private def addTo(runIdParent: RunId, runIdChild: RunId): Unit = {
    parentToChildren + (runIdParent → (parentToChildren(runIdParent) + runIdChild))
    childToParent + (runIdChild     → runIdParent)
  }

  def updateCommand(commandResponse: CommandResponse): Unit = {
    parentToChildren.get(commandResponse.runId) match {
      case Some(_) ⇒ commandStatusService ! UpdateCommand(commandResponse)
      case None    ⇒ //TODO: Implement this
    }
  }

  private def updateSubCommand(commandResponse: CommandResponse): Unit = {
    childToParent
      .get(commandResponse.runId)
      .foreach(commandStatusService ! Query(_, ctx.spawnAdapter(CommandResponseE(_, commandResponse))))
  }

  private def updateParent(commandResponseParent: CommandResponse, commandResponseChild: CommandResponse): Unit =
    (commandResponseParent.resultType, commandResponseChild.resultType) match {
      case (CommandResultType.Intermediate, CommandResultType.Negative) ⇒
        updateCommand(commandResponseChild)
      case (CommandResultType.Intermediate, CommandResultType.Positive) ⇒
        updateParentForChild(commandResponseParent.runId, commandResponseChild)
      case _ ⇒
    }

  private def updateParentForChild(parentId: RunId, commandResponseChild: CommandResponse): Unit =
    commandResponseChild match {
      case _: CommandExecutionResponse ⇒
        parentToChildren + (parentId → (parentToChildren(parentId) - commandResponseChild.runId))
        if (parentToChildren(parentId).isEmpty)
          updateCommand(commandResponseChild)
      case _ ⇒
    }
}
