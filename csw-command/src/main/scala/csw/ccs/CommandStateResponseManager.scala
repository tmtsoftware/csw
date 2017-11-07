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

class CommandStateResponseManager(
    ctx: ActorContext[CommandStatePubSub],
    timerScheduler: TimerScheduler[CommandStatePubSub],
    componentName: String
) extends ComponentLogger.MutableActor[CommandStatePubSub](ctx, componentName) {

  val cmdToSubscribers: mutable.Map[RunId, mutable.Set[ActorRef[CommandResponse]]] = mutable.Map.empty
  var cmdToSender: mutable.Map[RunId, Option[ActorRef[CommandResponse]]]           = mutable.Map.empty
  val cmdToCurrentState: mutable.Map[RunId, CommandResponse]                       = mutable.Map.empty
  val parentToChildren: mutable.Map[RunId, Set[RunId]]                             = mutable.Map.empty
  val childToParent: mutable.Map[RunId, RunId]                                     = mutable.Map.empty

  override def onMessage(msg: CommandStatePubSub): Behavior[CommandStatePubSub] = {
    msg match {
      case Add(runId, replyTo)            ⇒ add(runId, Some(replyTo))
      case AddTo(runIdParent, runIdChild) ⇒ addTo(runIdParent, runIdChild)
      case Update(runId, cmdStatus)       ⇒ update(runId, cmdStatus)
      case Subscribe(runId, replyTo)      ⇒ subscribe(runId, replyTo)
      case UnSubscribe(runId, replyTo)    ⇒ unSubscribe(runId, replyTo)
      case Query(runId, replyTo)          ⇒ replyTo ! cmdToCurrentState.getOrElse(runId, CommandNotAvailable(runId))
      case ClearCommandState(runId)       ⇒ clearCmdState(runId)
    }
    this
  }

  private def add(runId: RunId, replyTo: Option[ActorRef[CommandResponse]]) = {
    cmdToSender + (runId       → replyTo)
    cmdToCurrentState + (runId → Initialized)
  }

  private def addTo(
      runIdParent: RunId,
      runIdChild: RunId
  ): mutable.Map[RunId, Object] = {
    add(runIdChild, None)
    parentToChildren + (runIdParent → (parentToChildren(runIdParent) + runIdChild))
    childToParent + (runIdChild     → runIdParent)
  }

  private def subscribe(runId: RunId, actorRef: ActorRef[CommandResponse]): Unit = {
    cmdToSubscribers
      .get(runId)
      .foreach(subscribers ⇒ cmdToSubscribers + (runId → (subscribers + actorRef)))
  }

  private def unSubscribe(runId: RunId, actorRef: ActorRef[CommandResponse]): Unit = {
    cmdToSubscribers
      .get(runId)
      .foreach(subscribers ⇒ cmdToSubscribers + (runId → (subscribers - actorRef)))
  }

  private def update(runId: RunId, commandResponse: CommandResponse): Unit = {
    cmdToCurrentState + (runId → commandResponse)
    childToParent.get(runId) match {
      case Some(parentId) => updateParent(parentId, runId, commandResponse)
      case None =>
        if (commandResponse.isInstanceOf[CommandFinalResponse]) {
          timerScheduler.startSingleTimer("ClearState", ClearCommandState(runId), 10.seconds)
        }
    }

    log.debug(s"Notifying subscribers :[${cmdToSubscribers(runId).mkString(",")}] with data :[$commandResponse]")
    cmdToSubscribers(runId).foreach(_ ! commandResponse)
  }

  private def updateParent(parentId: RunId, childId: RunId, response: CommandResponse): Unit = {
    cmdToCurrentState(parentId).asInstanceOf[CommandStateType] match {
      case _: CommandFinalResponse ⇒ // do nothing
      case _: CommandIntermediateResponse ⇒
        val resultType: CommandResultType = response.asInstanceOf[CommandResultType]
        resultType match {
          case _: CommandNegativeResponse ⇒ update(parentId, response)
          case _: CommandPositiveResponse ⇒
            parentToChildren + (parentId → (parentToChildren(parentId) - parentId))
            if (parentToChildren(parentId).isEmpty)
              update(parentId, response)
        }
    }
  }

  def clearCmdState(runId: RunId) = ???
}
