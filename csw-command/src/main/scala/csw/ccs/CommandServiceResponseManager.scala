package csw.ccs

import akka.typed.scaladsl.{ActorContext, TimerScheduler}
import akka.typed.{ActorRef, Behavior}
import csw.messages.CommandStatePubSub
import csw.messages.CommandStatePubSub._
import csw.messages.ccs.commands.CommandFinalExecutionResponse.CommandNotAvailable
import csw.messages.ccs.commands.CommandIntermediateExecutionResponse.Initialized
import csw.messages.ccs.commands._
import csw.messages.params.models.RunId
import csw.services.logging.scaladsl.ComponentLogger

import scala.collection.mutable
import scala.concurrent.duration.DurationDouble

class CommandServiceResponseManager(
    ctx: ActorContext[CommandStatePubSub],
    timerScheduler: TimerScheduler[CommandStatePubSub],
    componentName: String
) extends ComponentLogger.MutableActor[CommandStatePubSub](ctx, componentName) {

  val cmdToCmdStatus: mutable.Map[RunId, CommandState] = mutable.Map.empty

  val parentToChildren: mutable.Map[RunId, Set[RunId]] = mutable.Map.empty
  val childToParent: mutable.Map[RunId, RunId]         = mutable.Map.empty

  override def onMessage(msg: CommandStatePubSub): Behavior[CommandStatePubSub] = {
    msg match {
      case Add(runId, replyTo)            ⇒ add(runId, Some(replyTo))
      case AddTo(runIdParent, runIdChild) ⇒ addTo(runIdParent, runIdChild)
      case Update(runId, cmdStatus)       ⇒ update(runId, cmdStatus)
      case Subscribe(runId, replyTo)      ⇒ subscribe(runId, replyTo)
      case UnSubscribe(runId, replyTo)    ⇒ unSubscribe(runId, replyTo)
      case Query(runId, replyTo)          ⇒ sendCurrentState(runId, replyTo)
      case ClearCommandState(runId)       ⇒ clearCmdState(runId)
    }
    this
  }

  private def add(runId: RunId, replyTo: Option[ActorRef[CommandResponse]]) =
    cmdToCmdStatus + (runId → CommandState(runId, replyTo, Set.empty, Initialized(runId)))

  private def addTo(runIdParent: RunId, runIdChild: RunId): mutable.Map[RunId, Object] = {
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
      .foreach(cmdState ⇒ cmdToCmdStatus + (runId → cmdState.copy(currentCmdStatus = commandResponse)))

    childToParent.get(runId) match {
      case Some(parentId) => updateParent(parentId, runId, commandResponse)
      case None =>
        if (commandResponse.isInstanceOf[CommandFinalExecutionResponse]) {
          cmdToCmdStatus(runId).sendStatus()
        }
        if (commandResponse.isInstanceOf[CommandFinalResponse]) {
          timerScheduler.startSingleTimer("ClearState", ClearCommandState(runId), 10.seconds)
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
    if (cmdToCmdStatus(parentId).currentCmdStatus.isInstanceOf[CommandIntermediateResponse]) {
      responseFromChildCmd.asInstanceOf[CommandResultType] match {
        case _: CommandNegativeResponse ⇒ update(parentId, responseFromChildCmd)
        case _: CommandPositiveResponse ⇒ updateParentForChild(parentId, childId, responseFromChildCmd)
      }
    }

  private def updateParentForChild(
      parentId: RunId,
      childId: RunId,
      responseFromChildCmd: CommandResponse
  ): Unit =
    if (responseFromChildCmd.isInstanceOf[CommandExecutionResponse]) {
      parentToChildren + (parentId → (parentToChildren(parentId) - childId))
      if (parentToChildren(parentId).isEmpty)
        update(parentId, responseFromChildCmd)
    }

  private def subscribe(
      runId: RunId,
      actorRef: ActorRef[CommandResponse]
  ): Unit =
    cmdToCmdStatus
      .get(runId)
      .foreach(cmdState ⇒ cmdToCmdStatus + (runId → cmdState.copy(subscribers = cmdState.subscribers + actorRef)))

  private def unSubscribe(
      runId: RunId,
      actorRef: ActorRef[CommandResponse]
  ): Unit =
    cmdToCmdStatus
      .get(runId)
      .foreach(cmdState ⇒ cmdToCmdStatus + (runId → cmdState.copy(subscribers = cmdState.subscribers - actorRef)))

  private def sendCurrentState(runId: RunId, replyTo: ActorRef[CommandResponse]): Unit = {
    val currentCmdState = cmdToCmdStatus.get(runId) match {
      case Some(cmdState) => cmdState.currentCmdStatus
      case None           => CommandNotAvailable(runId)
    }
    replyTo ! currentCmdState
  }

  def clearCmdState(runId: RunId) = ???
}
