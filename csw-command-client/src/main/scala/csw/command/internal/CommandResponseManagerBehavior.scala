package csw.command.internal

import akka.actor.typed.scaladsl.{ActorContext, MutableBehavior}
import akka.actor.typed.{ActorRef, Behavior}
import csw.command.messages.CommandResponseManagerMessage
import csw.command.messages.CommandResponseManagerMessage._
import csw.command.models.{CommandCorrelation, CommandResponseManagerState}
import csw.params.commands.CommandResponse
import csw.params.core.models.Id
import csw.logging.scaladsl.{Logger, LoggerFactory}

/**
 * The Behavior of a Command Response Manager is represented as a mutable behavior. This behavior will be created as an actor.
 * There will be one CommandResponseManager for a given component which will provide an interface to interact with the status
 * and result of a submitted commands.
 *
 * This class defines the behavior of CommandResponseManagerActor and is responsible for adding/updating/querying command result.
 * When component receives command of type Submit, then framework (ComponentBehavior - TLA) will add a entry of this command
 * with the [[csw.params.commands.CommandResponse.SubmitResponse]] of [[csw.params.commands.CommandResponse.Started]]
 * stored into the CommandResponseManager.
 *
 * In case of short running or immediate command, submit response will be of type final result which can either positive or negative
 * In case of long running command, a positive validation will result of type [[csw.params.commands.CommandResponse.Started]]
 * then it is the responsibility of component writer to update the runId of the command with its final command status later on
 * with a [[csw.params.commands.CommandResponse.SubmitResponse]] which will either be a positive or negative response.
 *
 * CommandResponseManager also provides subscribe API.
 * One of the use cases for this is when Assembly splits a top level command into two sub commands and forwards them to two different HCD's.
 * In this case, Assembly can register its interest in the final [[csw.params.commands.CommandResponse.SubmitResponse]]
 * from two HCD's when these sub commands completes, using subscribe API. And once Assembly receives final submit response
 * from both the HCD's then it can update Top level command with final [[csw.params.commands.CommandResponse.SubmitResponse]]
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

  import CommandResponse._

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

  // This is where the command is initially added. Note that every Submit is added as "Started"/Intermediate
  private def addOrUpdateCommand(runId: Id, commandResponse: SubmitResponse): Unit =
    commandResponseManagerState.get(runId) match {
      case _: CommandNotAvailable ⇒
        commandResponseManagerState = commandResponseManagerState.add(runId, commandResponse)
      case _ ⇒ updateCommand(runId, commandResponse)
    }

  private def updateCommand(runId: Id, updateResponse: SubmitResponse): Unit = {
    val currentResponse = commandResponseManagerState.get(runId)
    // Note that commands are added with state Started by ComponentBehavior
    // Also makes sure that once it is final, it is final and can't be set back to Started
    // Also fixes a potential race condition where someone sets to final status before return from onSubmit returning Started
    if (isIntermediate(currentResponse)) {
      if (isFinal(updateResponse)) {
        commandResponseManagerState = commandResponseManagerState.updateCommandStatus(updateResponse)
      }
      // This means that even if not final (i.e. Intermediate/Started) it will publish a Started
      doPublish(updateResponse, commandResponseManagerState.cmdToCmdStatus(updateResponse.runId).subscribers)
    }
  }

  private def updateSubCommand(subCommandRunId: Id, commandResponse: SubmitResponse): Unit = {
    // If the sub command has a parent command, fetch the current status of parent command from command status service
    commandCoRelation
      .getParent(commandResponse.runId)
      .foreach(parentId ⇒ updateParent(parentId, commandResponse))
  }

  private def updateParent(parentRunId: Id, childCommandResponse: SubmitResponse): Unit =
    // Is the parent in the Started/Intermediate
    if (isIntermediate(commandResponseManagerState.get(parentRunId))) {
      // If the child is positive, update parent
      if (isPositive(childCommandResponse)) {
        updateParentForChild(parentRunId, childCommandResponse)
      } else if (isNegative(childCommandResponse)) {
        // isNegative - update the parent and quit early
        updateCommand(parentRunId, CommandResponse.withRunId(parentRunId, childCommandResponse))
      }
    } else {
      log.debug("Parent Command is already updated with a Final response. Ignoring this update.")
    }

  private def updateParentForChild(parentRunId: Id, childCommandResponse: SubmitResponse): Unit =
    if (isFinal(childCommandResponse)) {
      commandCoRelation = commandCoRelation.remove(parentRunId, childCommandResponse.runId)
      if (!commandCoRelation.hasChildren(parentRunId))
        updateCommand(parentRunId, CommandResponse.withRunId(parentRunId, childCommandResponse))
    } else {
      log.debug("Validation response will not affect status of Parent command.")
    }

  // This publish only publishes if the value is a final response
  private def publishToSubscribers(commandResponse: SubmitResponse, subscribers: Set[ActorRef[SubmitResponse]]): Unit =
    if (isFinal(commandResponse)) doPublish(commandResponse, subscribers)

  // This low level will publish no matter what
  private def doPublish(commandResponse: SubmitResponse, subscribers: Set[ActorRef[SubmitResponse]]): Unit =
    subscribers.foreach(_ ! commandResponse)

  private def subscribe(runId: Id, replyTo: ActorRef[SubmitResponse]): Unit = {
    ctx.watchWith(replyTo, SubscriberTerminated(replyTo))
    commandResponseManagerState = commandResponseManagerState.subscribe(runId, replyTo)
    commandResponseManagerState.get(runId) match {
      case sr: SubmitResponse => publishToSubscribers(sr, Set(replyTo))
      case _                  => log.debug("Failed to find runId for subscribe.")
    }
  }
}
