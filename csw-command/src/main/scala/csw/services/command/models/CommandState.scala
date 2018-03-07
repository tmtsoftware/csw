package csw.services.command.models

import akka.typed.ActorRef
import csw.messages.ccs.commands.CommandResponse
import csw.messages.params.models.Id

/**
 * Represents the state of a command execution
 * @param commandStatus the current command status
 * @param subscribers the subscriber list for the change in state
 */
case class CommandState private[command] (commandStatus: CommandStatus, subscribers: Set[ActorRef[CommandResponse]]) {

  /**
   * Add a new subscriber for change in state
   * @param subscriber the subscriber as an actor to which the updated state will be sent
   * @return
   */
  def addSubscriber(subscriber: ActorRef[CommandResponse]): CommandState = copy(subscribers = subscribers + subscriber)

  /**
   * Remove a subscriber for change in state
   * @param subscriber the subscriber as an actor to which the updated state will be sent
   * @return
   */
  def removeSubscriber(subscriber: ActorRef[CommandResponse]): CommandState =
    copy(subscribers = subscribers - subscriber)

  /**
   * Create a new state from `this` state with the provided command response
   * @param commandResponse the command Response
   * @return a new command state with the current state as provided command response
   */
  def withCommandStatus(commandResponse: CommandResponse): CommandState =
    copy(commandStatus = this.commandStatus.withCommandResponse(commandResponse))

}

private[command] object CommandState {

  /**
   * Inititialize the command state for a given command
   * @param runId command identifier as a RunId
   * @param initialState initial command response
   * @return a new command state
   */
  def init(runId: Id, initialState: CommandResponse): CommandState =
    CommandState(CommandStatus(runId, initialState), Set.empty)
}
