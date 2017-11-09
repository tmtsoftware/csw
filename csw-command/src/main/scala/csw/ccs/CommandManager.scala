package csw.ccs

import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior}
import csw.messages.CommandManagerMessages
import csw.messages.CommandManagerMessages._
import csw.messages.ccs.commands.CommandExecutionResponse.{CommandNotAvailable, Initialized}
import csw.messages.ccs.commands._
import csw.messages.params.models.RunId
import csw.services.logging.scaladsl.ComponentLogger

class CommandManager(
    ctx: ActorContext[CommandManagerMessages],
    componentName: String
) extends ComponentLogger.MutableActor[CommandManagerMessages](ctx, componentName) {

  var cmdToCmdStatus: Map[RunId, CommandState] = Map.empty

  val parentToChildren: Map[RunId, Set[RunId]] = Map.empty
  val childToParent: Map[RunId, RunId]         = Map.empty

  override def onMessage(msg: CommandManagerMessages): Behavior[CommandManagerMessages] = {
    msg match {
      case Add(runId, replyTo)            ⇒ add(runId, Some(replyTo))
      case AddTo(runIdParent, runIdChild) ⇒ addTo(runIdParent, runIdChild)
      case Update(runId, cmdStatus)       ⇒ update(runId, cmdStatus)
    }
    this
  }

  private def add(runId: RunId, replyTo: Option[ActorRef[CommandResponse]]): Unit =
    cmdToCmdStatus = cmdToCmdStatus + (runId → CommandState(runId, replyTo, Set.empty, Initialized(runId)))

  private def addTo(runIdParent: RunId, runIdChild: RunId): Unit = {
    add(runIdChild, None)
    parentToChildren + (runIdParent → (parentToChildren(runIdParent) + runIdChild))
    childToParent + (runIdChild     → runIdParent)
  }

  private def update(
      runId: RunId,
      commandResponse: CommandResponse
  ): Unit = {
    cmdToCmdStatus
      .get(runId)
      .foreach(cmdState ⇒ cmdToCmdStatus = cmdToCmdStatus + (runId → cmdState.copy(currentCmdStatus = commandResponse)))

    childToParent.get(runId) match {
      case Some(parentId) => updateParent(parentId, runId, commandResponse)
      case None =>
        if (commandResponse.isInstanceOf[CommandExecutionResponse] && commandResponse.resultType
              .isInstanceOf[CommandResultType.Final]) {
          cmdToCmdStatus(runId).sendStatus()
        }

    }

    log.debug(
      s"Notifying subscribers :[${cmdToCmdStatus(runId).subscribers.mkString(",")}] with data :[$commandResponse]"
    )
    cmdToCmdStatus(runId).publishStatus()
  }

  private def updateParent(
      parentId: RunId,
      childId: RunId,
      responseFromChildCmd: CommandResponse
  ): Unit =
    (cmdToCmdStatus(parentId).currentCmdStatus.resultType, responseFromChildCmd.resultType) match {
      case (CommandResultType.Intermediate, CommandResultType.Negative) ⇒ update(parentId, responseFromChildCmd)
      case (CommandResultType.Intermediate, CommandResultType.Positive) ⇒
        updateParentForChild(parentId, childId, responseFromChildCmd)
      case _ ⇒
    }

  private def updateParentForChild(
      parentId: RunId,
      childId: RunId,
      responseFromChildCmd: CommandResponse
  ): Unit =
    responseFromChildCmd match {
      case _: CommandExecutionResponse ⇒
        parentToChildren + (parentId → (parentToChildren(parentId) - childId))
        if (parentToChildren(parentId).isEmpty)
          update(parentId, responseFromChildCmd)
      case _ ⇒
    }

  private def sendCurrentState(runId: RunId, replyTo: ActorRef[CommandResponse]): Unit = {
    val currentCmdState = cmdToCmdStatus.get(runId) match {
      case Some(cmdState) => cmdState.currentCmdStatus
      case None           => CommandNotAvailable(runId)
    }
    replyTo ! currentCmdState
  }

}
