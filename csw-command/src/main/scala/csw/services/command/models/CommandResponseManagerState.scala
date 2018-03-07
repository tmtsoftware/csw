package csw.services.command.models

import akka.typed.ActorRef
import csw.messages.commands.CommandResponse
import csw.messages.commands.CommandResponse.CommandNotAvailable
import csw.messages.params.models.Id

/**
 * Manages state of a given command identified by a RunId
 * @param cmdToCmdStatus a map of runId to CommandState
 */
case class CommandResponseManagerState private[command] (cmdToCmdStatus: Map[Id, CommandState]) {
  def add(runId: Id, initialState: CommandResponse): CommandResponseManagerState =
    CommandResponseManagerState(cmdToCmdStatus.updated(runId, CommandState.init(runId, initialState)))

  /**
   * Get the current command response for the command
   * @param runId command identifier
   * @return current command response
   */
  def get(runId: Id): CommandResponse = cmdToCmdStatus.get(runId) match {
    case Some(cmdState) => cmdState.commandStatus.currentCmdStatus
    case None           => CommandNotAvailable(runId)
  }

  /**
   * Update the current command response for the command
   * @param commandResponse the command response to be updated for this command
   * @return current command response
   */
  def updateCommandStatus(commandResponse: CommandResponse): CommandResponseManagerState =
    update(commandResponse.runId, _.withCommandStatus(commandResponse))

  /**
   * Subscribe to the change in state of a Command
   * @param runId command identifier
   * @param actorRef the subscriber as an actor to which the updated state will be sent
   * @return
   */
  def subscribe(runId: Id, actorRef: ActorRef[CommandResponse]): CommandResponseManagerState =
    update(runId, _.addSubscriber(actorRef))

  /**
   * UnSubscribe to the change in state of a Command
   * @param runId command identifier
   * @param actorRef the subscriber as an actor to which the updated state was being sent
   * @return
   */
  def unSubscribe(runId: Id, actorRef: ActorRef[CommandResponse]): CommandResponseManagerState =
    update(runId, _.removeSubscriber(actorRef))

  private def update(runId: Id, f: CommandState ⇒ CommandState): CommandResponseManagerState =
    cmdToCmdStatus.get(runId) match {
      case Some(cmdState) ⇒ CommandResponseManagerState(cmdToCmdStatus.updated(runId, f(cmdState)))
      case None           ⇒ this
    }
}
