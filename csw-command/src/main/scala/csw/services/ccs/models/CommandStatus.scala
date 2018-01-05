package csw.services.ccs.models

import csw.messages.ccs.commands.CommandResponse
import csw.messages.params.models.RunId

/**
 * Represents current state of a command execution
 * @param runId command identifier as a RunId
 * @param currentCmdStatus current command response
 */
case class CommandStatus(runId: RunId, currentCmdStatus: CommandResponse) {
  def withCommandResponse(commandResponse: CommandResponse): CommandStatus = copy(currentCmdStatus = commandResponse)
}
