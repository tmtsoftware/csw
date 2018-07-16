package csw.command.models

import csw.params.commands.CommandResponse
import csw.params.core.models.Id

/**
 * Represents current state of a command execution
 *
 * @param runId command identifier as a RunId
 * @param currentCmdStatus current command response
 */
private[csw] case class CommandStatus(runId: Id, currentCmdStatus: CommandResponseBase) {

  /**
   * Create a new CommandStatus with provided command response
   *
   * @param commandResponse the response of command
   * @return a new CommandStatus instance with updated commandResponse
   */
  def withCommandResponse(commandResponse: CommandResponseBase): CommandStatus = copy(currentCmdStatus = commandResponse)
}
