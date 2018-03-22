package csw.messages.commands

import akka.actor.typed.ActorRef
import csw.messages.commands.CommandResponse.CommandNotAvailable
import csw.messages.params.models.Id

/**
 * Manages state of a given command identified by a RunId
 *
 * @param cmdToCmdStatus a map of runId to CommandState
 */
private[csw] case class CommandResponseManagerState(cmdToCmdStatus: Map[Id, CommandState]) {

  /**
   * Add the command with some initial state
   *
   * @param runId command identifier
   * @param initialState an initial state
   * @return a new CommandResponseManagerState instance with updated cmdToCmdStatus
   */
  def add(runId: Id, initialState: CommandResponse): CommandResponseManagerState =
    CommandResponseManagerState(cmdToCmdStatus.updated(runId, CommandState.init(runId, initialState)))

  /**
   * Get the current command response for the command
   *
   * @param runId command identifier
   * @return current command response
   */
  def get(runId: Id): CommandResponse = cmdToCmdStatus.get(runId) match {
    case Some(cmdState) => cmdState.commandStatus.currentCmdStatus
    case None           => CommandNotAvailable(runId)
  }

  /**
   * Update the current command response for the command
   *
   * @param commandResponse the command response to be updated for this command
   * @return a new CommandResponseManagerState instance with updated cmdToCmdStatus
   */
  def updateCommandStatus(commandResponse: CommandResponse): CommandResponseManagerState =
    update(commandResponse.runId, _.withCommandStatus(commandResponse))

  /**
   * Subscribe to the change in state of a Command
   * @param runId command identifier
   * @param actorRef the subscriber as an actor to which the updated state will be sent
   * @return a new CommandResponseManagerState instance with updated cmdToCmdStatus
   */
  def subscribe(runId: Id, actorRef: ActorRef[CommandResponse]): CommandResponseManagerState =
    update(runId, _.addSubscriber(actorRef))

  /**
   * UnSubscribe to the change in state of a Command
   *
   * @param runId command identifier
   * @param actorRef the subscriber as an actor to which the updated state was being sent
   * @return a new CommandResponseManagerState instance with updated cmdToCmdStatus
   */
  def unSubscribe(runId: Id, actorRef: ActorRef[CommandResponse]): CommandResponseManagerState =
    update(runId, _.removeSubscriber(actorRef))

  def removeSubscriber(actorRef: ActorRef[CommandResponse]): CommandResponseManagerState = {
    def remove(ids: List[Id], commandResponseManagerState: CommandResponseManagerState): CommandResponseManagerState = {
      ids match {
        case Nil                ⇒ commandResponseManagerState
        case id :: remainingIds ⇒ remove(remainingIds, unSubscribe(id, actorRef))
      }
    }
    remove(cmdToCmdStatus.keys.toList, this)
  }

  private def update(runId: Id, f: CommandState ⇒ CommandState): CommandResponseManagerState =
    cmdToCmdStatus.get(runId) match {
      case Some(cmdState) ⇒ CommandResponseManagerState(cmdToCmdStatus.updated(runId, f(cmdState)))
      case None           ⇒ this
    }
}
