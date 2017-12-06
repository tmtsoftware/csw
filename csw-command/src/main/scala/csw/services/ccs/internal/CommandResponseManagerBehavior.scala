package csw.services.ccs.internal

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.messages.CommandResponseManagerMessage
import csw.messages.CommandResponseManagerMessage._
import csw.messages.ccs.commands.CommandResponse.CommandNotAvailable
import csw.messages.ccs.commands.CommandResultType.{Final, Intermediate}
import csw.messages.ccs.commands.{CommandResponse, CommandResultType}
import csw.messages.params.models.RunId
import csw.services.ccs.models.{CommandCorrelation, CommandResponseManagerState}
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

class CommandResponseManagerBehavior(
    ctx: ActorContext[CommandResponseManagerMessage],
    loggerFactory: LoggerFactory
) extends Actor.MutableBehavior[CommandResponseManagerMessage] {
  val log: Logger = loggerFactory.getLogger(ctx)

  var commandStatus: CommandResponseManagerState = CommandResponseManagerState(Map.empty)
  var commandCoRelation: CommandCorrelation      = CommandCorrelation(Map.empty, Map.empty)

  override def onMessage(msg: CommandResponseManagerMessage): Behavior[CommandResponseManagerMessage] = {
    msg match {
      case AddOrUpdateCommand(commandId, cmdStatus)  ⇒ addOrUpdateCommand(commandId, cmdStatus)
      case AddSubCommand(parentRunId, childRunId)    ⇒ commandCoRelation = commandCoRelation.add(parentRunId, childRunId)
      case UpdateSubCommand(subCommandId, cmdStatus) ⇒ updateSubCommand(subCommandId, cmdStatus)
      case Query(runId, replyTo)                     ⇒ replyTo ! commandStatus.get(runId)
      case Subscribe(runId, replyTo)                 ⇒ subscribe(runId, replyTo)
      case Unsubscribe(runId, subscriber)            ⇒ commandStatus = commandStatus.unSubscribe(runId, subscriber)
    }
    this
  }

  private def addOrUpdateCommand(commandId: RunId, commandResponse: CommandResponse): Unit =
    commandStatus.get(commandId) match {
      case _: CommandNotAvailable ⇒ commandStatus = commandStatus.add(commandId, commandResponse)
      case _                      ⇒ updateCommand(commandId, commandResponse)
    }

  private def updateCommand(commandId: RunId, commandResponse: CommandResponse): Unit =
    if (commandStatus.get(commandId) != commandResponse) {
      commandStatus = commandStatus.updateCommandStatus(commandResponse)
      publishToSubscribers(commandResponse, commandStatus.cmdToCmdStatus(commandResponse.runId).subscribers)
    }

  private def updateSubCommand(subCommandId: RunId, commandResponse: CommandResponse): Unit = {
    // If the sub command has a parent command, fetch the current status of parent command from command status service
    commandCoRelation
      .getParent(commandResponse.runId)
      .foreach(parentId ⇒ updateParent(parentId, commandResponse))
  }

  private def updateParent(parentRunId: RunId, childCommandResponse: CommandResponse): Unit =
    (commandStatus.get(parentRunId).resultType, childCommandResponse.resultType) match {
      // If the child command receives a negative result, the result of the parent command need not wait for the
      // result from other sub commands
      case (CommandResultType.Intermediate, CommandResultType.Negative) ⇒
        updateCommand(parentRunId, CommandResponse.withRunId(parentRunId, childCommandResponse))
      // If the child command receives a positive result, the result of the parent command needs to be evaluated based
      // on the result from other sub commands
      case (CommandResultType.Intermediate, CommandResultType.Positive) ⇒
        updateParentForChild(parentRunId, childCommandResponse)
      case _ ⇒ log.debug("Parent Command is already updated with a Final response. Ignoring this update.")
    }

  private def updateParentForChild(parentRunId: RunId, childCommandResponse: CommandResponse): Unit =
    childCommandResponse.resultType match {
      case _: Final ⇒
        commandCoRelation = commandCoRelation.remove(parentRunId, childCommandResponse.runId)
        if (!commandCoRelation.hasChildren(parentRunId))
          updateCommand(parentRunId, CommandResponse.withRunId(parentRunId, childCommandResponse))
      case _ ⇒ log.debug("Validation response will not affect status of Parent command.")
    }

  private def publishToSubscribers(commandResponse: CommandResponse, subscribers: Set[ActorRef[CommandResponse]]): Unit = {
    commandResponse.resultType match {
      case _: Final     ⇒ subscribers.foreach(_ ! commandResponse)
      case Intermediate ⇒
        // Do not send updates for validation response as it is send by the framework
        log.debug("Validation response will not affect status of Parent command.")
    }
  }

  private def subscribe(runId: RunId, replyTo: ActorRef[CommandResponse]): Unit = {
    ctx.watchWith(replyTo, Unsubscribe(runId, replyTo))
    commandStatus = commandStatus.subscribe(runId, replyTo)
    publishToSubscribers(commandStatus.get(runId), Set(replyTo))
  }
}
