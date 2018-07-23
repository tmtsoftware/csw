package csw.command.models

import csw.params.commands.CommandResponse
import csw.params.core.models.Id
import csw.messages.commands.Responses.SubmitResponse
import csw.messages.params.models.Id

/**
 * Represents current state of a command execution
 *
 * @param runId command identifier as a RunId
 * @param currentCmdStatus current command response
 */
private[csw] case class CommandStatus(runId: Id, currentCmdStatus: SubmitResponse) {

  /**
   * Create a new CommandStatus with provided command response
   *
   * @param commandResponse the response of command
   * @return a new CommandStatus instance with updated commandResponse
   */
  def withCommandResponse(commandResponse: SubmitResponse): CommandStatus = copy(currentCmdStatus = commandResponse)
}
