package csw.command.client.internal
import csw.params.commands.CommandResponse.{CommandNotAvailable, QueryResponse, SubmitResponse}
import csw.params.core.models.Id

/**
 * Manages state of a given command identified by a RunId
 *
 * @param cmdToCmdResponse a map of runId to CommandState
 */
private[command] case class CommandResponseManagerState(cmdToCmdResponse: Map[Id, SubmitResponse]) {

  /**
   * Add the command with some initial response
   *
   * @param runId command identifier
   * @param initialResponse an initial response
   * @return a new CommandResponseManagerState instance with updated cmdToCmdResponse
   */
  def add(runId: Id, initialResponse: SubmitResponse): CommandResponseManagerState =
    CommandResponseManagerState(cmdToCmdResponse.updated(runId, initialResponse))

  /**
   * Get the current command response for the command
   *
   * @param runId command identifier
   * @return current command response
   */
  def get(runId: Id): QueryResponse = cmdToCmdResponse.get(runId) match {
    case Some(cmdResponse) => cmdResponse
    case None              => CommandNotAvailable(runId)
  }

  /**
   * Update the current command response for the command
   *
   * @param commandResponse the command response to be updated for this command
   * @return a new CommandResponseManagerState instance with updated cmdToCmdStatus
   */
  def updateCommandStatus(commandResponse: SubmitResponse): CommandResponseManagerState =
    cmdToCmdResponse.get(commandResponse.runId) match {
      case Some(cmdState) ⇒ CommandResponseManagerState(cmdToCmdResponse.updated(commandResponse.runId, commandResponse))
      case None           ⇒ this
    }
}
