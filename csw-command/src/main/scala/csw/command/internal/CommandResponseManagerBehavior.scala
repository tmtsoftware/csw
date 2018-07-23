package csw.command.internal

import akka.actor.typed.scaladsl.{ActorContext, MutableBehavior}
import akka.actor.typed.{ActorRef, Behavior}
import csw.command.messages.CommandResponseManagerMessage
import csw.command.messages.CommandResponseManagerMessage._
import csw.command.models.{CommandCorrelation, CommandResponseManagerState}
import csw.params.commands.CommandResponse.CommandNotAvailable
import csw.params.commands.CommandResultType.{Final, Intermediate}
import csw.params.commands.{CommandResponse, CommandResultType}
import csw.params.core.models.Id
import csw.logging.scaladsl.{Logger, LoggerFactory}
import csw.messages.CommandResponseManagerMessage
import csw.messages.commands._
import csw.messages.params.models.Id
import csw.messages.CommandResponseManagerMessage._
import csw.messages.commands.Responses._
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

/**
 * The Behavior of a Command Response Manager, represented as a mutable behavior. This behavior will be created as an actor.
 * There will be one CommandResponseManger for a given component which will provide an interface to interact with the status
 * and result of a submitted command.
 *
 * This class defines the behavior of CommandResponseManagerActor and is responsible for adding/updating/querying command result.
 * When component receives command of type Submit, then framework (ComponentBehavior - TLA) will add a entry of this command
 * with its validation status into CommandResponseManager.
 *
 * In case of short running or immediate command,
 * validation response will be of type final result which can either be of type
 * [[csw.params.commands.CommandResultType.Positive]] or [[csw.params.commands.CommandResultType.Negative]]
 *
 * In case of long running command, validation response will be of type [[csw.params.commands.CommandResultType.Intermediate]]
 * then it is the responsibility of component writer to update its final command status later on
 * with [[csw.params.commands.CommandResponse]] which should be of type
 * [[csw.params.commands.CommandResultType.Positive]] or [[csw.params.commands.CommandResultType.Negative]]
 *
 * CommandResponseManager also provides subscribe API.
 * One of the use case for this is when Assembly splits top level command into two sub commands and forwards them to two different HCD's.
 * In this case, Assembly can register its interest in the final [[csw.params.commands.CommandResponse]]
 * from two HCD's when these sub commands completes, using subscribe API. And once Assembly receives final command response
 * from both the HCD's then it can update Top level command with final [[csw.params.commands.CommandResponse]]
 *
 * @param ctx             The Actor Context under which the actor instance of this behavior is created
 * @param loggerFactory   The factory for creating [[csw.logging.scaladsl.Logger]] instance
 */
private[command] class CommandResponseManagerBehavior(
    ctx: ActorContext[CommandResponseManagerMessage],
    loggerFactory: LoggerFactory
) extends MutableBehavior[CommandResponseManagerMessage] {
  private val log: Logger = loggerFactory.getLogger(ctx)

  private[command] var commandResponseManagerState: CommandResponseManagerState = CommandResponseManagerState(Map.empty)
  private[command] var commandCoRelation: CommandCorrelation                    = CommandCorrelation(Map.empty, Map.empty)

  override def onMessage(msg: CommandResponseManagerMessage): Behavior[CommandResponseManagerMessage] = {
    msg match {
      case AddOrUpdateCommand(runId, cmdStatus)         ⇒ addOrUpdateCommand(runId, cmdStatus)
      case AddSubCommand(parentRunId, childRunId)       ⇒ commandCoRelation = commandCoRelation.add(parentRunId, childRunId)
      case UpdateSubCommand(subCommandRunId, cmdStatus) ⇒ updateSubCommand(subCommandRunId, cmdStatus)
      case Query(runId, replyTo)                        ⇒ replyTo ! commandResponseManagerState.get(runId)
      case Subscribe(runId, replyTo)                    ⇒ subscribe(runId, replyTo)
      case Unsubscribe(runId, subscriber) ⇒
        commandResponseManagerState = commandResponseManagerState.unSubscribe(runId, subscriber)
      case SubscriberTerminated(subscriber) ⇒
        commandResponseManagerState = commandResponseManagerState.removeSubscriber(subscriber)
      case GetCommandCorrelation(replyTo)          ⇒ replyTo ! commandCoRelation
      case GetCommandResponseManagerState(replyTo) ⇒ replyTo ! commandResponseManagerState
    }
    this
  }

  private def addOrUpdateCommand(runId: Id, commandResponse: SubmitResponse): Unit =
    commandResponseManagerState.get(runId) match {
      case _: CommandNotAvailable ⇒ commandResponseManagerState = commandResponseManagerState.add(runId, commandResponse)
      case _                      ⇒ updateCommand(runId, commandResponse)
    }

  private def updateCommand(runId: Id, commandResponse: SubmitResponse): Unit = {
    val currentResponse = commandResponseManagerState.get(runId)
    if (currentResponse.resultType == CommandResultType.Intermediate && currentResponse != commandResponse) {
      commandResponseManagerState = commandResponseManagerState.updateCommandStatus(commandResponse)
      publishToSubscribers(commandResponse, commandResponseManagerState.cmdToCmdStatus(commandResponse.runId).subscribers)
    }
  }

  private def updateSubCommand(subCommandRunId: Id, commandResponse: SubmitResponse): Unit = {
    // If the sub command has a parent command, fetch the current status of parent command from command status service
    commandCoRelation
      .getParent(commandResponse.runId)
      .foreach(parentId ⇒ updateParent(parentId, commandResponse))
  }

  private def updateParent(parentRunId: Id, childCommandResponse: SubmitResponse): Unit =
    (commandResponseManagerState.get(parentRunId).resultType, childCommandResponse.resultType) match {
      // If the child command receives a negative result, the result of the parent command need not wait for the
      // result from other sub commands
      case (CommandResultType.Intermediate, CommandResultType.Negative) ⇒
        updateCommand(parentRunId, Responses.withRunId(parentRunId, childCommandResponse))
      // If the child command receives a positive result, the result of the parent command needs to be evaluated based
      // on the result from other sub commands
      case (CommandResultType.Intermediate, CommandResultType.Positive) ⇒
        updateParentForChild(parentRunId, childCommandResponse)
      case _ ⇒ log.debug("Parent Command is already updated with a Final response. Ignoring this update.")
    }

  private def updateParentForChild(parentRunId: Id, childCommandResponse: SubmitResponse): Unit =
    childCommandResponse.resultType match {
      case _: CommandResultType.Final ⇒
        commandCoRelation = commandCoRelation.remove(parentRunId, childCommandResponse.runId)
        if (!commandCoRelation.hasChildren(parentRunId))
          updateCommand(parentRunId, Responses.withRunId(parentRunId, childCommandResponse))
      case _ ⇒ log.debug("Validation response will not affect status of Parent command.")
    }

  private def publishToSubscribers(commandResponse: SubmitResponse, subscribers: Set[ActorRef[SubmitResponse]]): Unit = {
    commandResponse.resultType match {
      case _: CommandResultType.Final ⇒
        subscribers.foreach(_ ! commandResponse)
      case CommandResultType.Intermediate ⇒
        // Do not send updates for validation response as it is send by the framework
        log.debug("Validation response will not affect status of Parent command.")
    }
  }

  private def subscribe(runId: Id, replyTo: ActorRef[SubmitResponse]): Unit = {
    ctx.watchWith(replyTo, SubscriberTerminated(replyTo))
    commandResponseManagerState = commandResponseManagerState.subscribe(runId, replyTo)
    publishToSubscribers(commandResponseManagerState.get(runId), Set(replyTo))
  }
}
